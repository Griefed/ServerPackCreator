# A Windows Forgejo runner on a Linux x86_64 host

One Windows guest on libvirt/KVM, because `jpackage` cannot cross-compile ServerPackCreator's Windows
app-image. Why the guest exists and what the job does:
[`../../claude-docs/WINDOWS-RUNNER.md`](../../claude-docs/WINDOWS-RUNNER.md).

Blocks are marked **host**, **workstation** or **guest**. Everything but the Windows installation
itself can be driven from your workstation.

**Follow the steps in order and the guest comes up tuned.** Every performance decision is made at the
point where it is cheap — the disk format when the image is created, the memory backing before the
domain is defined, the guest settings before the runner is registered. Nothing here needs a second
pass. [Appendix A](#appendix-a-retrofitting-a-guest-that-already-exists) exists only for a guest built
before this file said so.

## 0. Conventions — **host** and **workstation**

`<user>@<host>` is a login on the Linux KVM host — the account step 1 adds to the `libvirt` group.
Substitute it wherever it appears.

Export `VM` in every shell you use, on both machines; the blocks below interpolate it.

```sh
export VM=windows-11-x86_64-forgejo-runner
```

On the **workstation**, point libvirt at the host as well — `virsh` and `virt-viewer` then tunnel
over that SSH connection by themselves, with no VNC port published on the host:

```sh
export LIBVIRT_DEFAULT_URI="qemu+ssh://<user>@<host>/system"
```

On the host itself, leave it unset or use `qemu:///system`. Not `qemu:///session`: that is a
per-user libvirt instance and cannot see a domain defined system-wide.

## 1. The host — **host**

```sh
sudo apt install -y qemu-system-x86 libvirt-daemon-system swtpm swtpm-tools ovmf
# Fedora/RHEL: qemu-kvm libvirt swtpm swtpm-tools edk2-ovmf

sudo usermod -aG libvirt "$USER" && newgrp libvirt
systemctl is-active libvirtd && test -w /dev/kvm && echo ready
```

`swtpm` is required: Windows 11 will not install without TPM 2.0. `virt-viewer` is not — it belongs on
your workstation.

### Tune the host before it has a guest to tune for

```sh
sudo apt install -y tuned
tuned-adm list | grep virtual-host     # confirm the profile exists before selecting it
sudo tuned-adm profile virtual-host
```

`virtual-host` is the profile Red Hat names for a KVM host: it lowers swappiness and writes dirty
pages back more aggressively, which is what keeps a guest's I/O burst from becoming the host's.

```sh
cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
```

The governor is the one knob `tuned` does not settle for you. `performance` gives the best guest
throughput; `schedutil` gives a 24/7 host a far better idle bill for a runner that is busy around a
release and asleep the rest of the month. Pick against the duty cycle, not the benchmark.

KSM needs no host change: `<memoryBacking><nosharepages/>` in `domain.xml` keeps the scanner off this
domain, and leaves it available to the others.

## 2. Media — **host**

```sh
sudo curl -L -o /var/lib/libvirt/images/virtio-win.iso \
  https://fedorapeople.org/groups/virt/virtio-win/direct-downloads/stable-virtio/virtio-win.iso
```

**The Windows ISO link has to be minted in a browser** — Microsoft rejects scripted clients. On your
workstation open
[microsoft.com/en-us/software-download/windows11](https://www.microsoft.com/en-us/software-download/windows11),
pick *Windows 11 (multi-edition ISO for x64 devices)* → **Confirm**, then *English (United States)* →
**Confirm**. Copy the **64-bit Download** link and the SHA-256 under *Verify your download*. The link
is valid 24 hours and needs no cookies, so the host fetches the 8.4 GiB itself:

```sh
sudo curl -L -C - -o /var/lib/libvirt/images/win11.iso '<link>'    # quote it: it contains &
sha256sum /var/lib/libvirt/images/win11.iso
```

Keep the ISO. The link expires, the file does not.

## 3. The disk image — **host**

Decide the format now. Converting later is an offline copy of the whole image
([Appendix A](#appendix-a-retrofitting-a-guest-that-already-exists)); choosing now is one word.

**`domain.xml` ships qcow2, and this is the command that matches it:**

```sh
sudo qemu-img create -f qcow2 -o extended_l2=on,cluster_size=128k \
  /var/lib/libvirt/images/$VM.qcow2 128G
sudo mkdir -p /var/lib/libvirt/qemu/nvram
```

Both `-o` options are load-bearing, and they only work as a pair. 128 KiB clusters cut the L2 metadata
32x against the 64 KiB default, which is what keeps the mapping tables resident instead of thrashing.
On its own that would raise the allocation unit to 128 KiB, which against a 4 KiB-cluster NTFS doing
scattered Gradle writes is write amplification. `extended_l2=on` splits every cluster into exactly 32
subclusters — fixed, not tunable — so 128 KiB / 32 = **4 KiB**, and allocation and zero/discard
marking happen per subcluster. Big-cluster metadata, small-cluster write granularity.

That fixed 32 is also the constraint: extended L2 needs `cluster_bits >= 14`, a **16 KiB minimum
cluster size**, because below that a subcluster would be under 512 bytes. It needs QEMU 5.2+, and the
image is unreadable by anything older — which matters only if the image will ever move to an older
host.

**`raw` is still the faster choice, and costs nothing you are using** — `domain.xml` ships qcow2
because it is what every other command in this file names, not because it won a measurement. The only
thing qcow2 buys over a sparse raw file is internal snapshots, and §8's rollback does not use them
— internal snapshots of a UEFI domain need the NVRAM in qcow2 format too, so the rollback is a file
copy either way. A raw file is sparse on ext4, XFS and btrfs, and `discard='unmap'` punches holes in
it, so it still occupies only what the guest actually uses. If you take it, create the image and edit the two matching lines in
`domain.xml` in the same sitting:

```sh
sudo qemu-img create -f raw /var/lib/libvirt/images/$VM.img 128G
# then in domain.xml: <driver name='qemu' type='raw' .../> and a matching <source file=.../>
```

## 4. Define the domain — **host**

[`domain.xml`](domain.xml) is the whole definition, commented inline.

### First, check your libvirt accepts it

```sh
virsh version        # the "Using library:" line is the one that decides this
```

| Element | Needs | In `domain.xml`? |
|---|---|---|
| `<disk><driver iothread='1'/>` | libvirt 1.2.8, QEMU 2.1 | yes |
| `<disk><driver queues='4'/>` | libvirt 3.9.0 | yes |
| `<memballoon freePageReporting='on'/>` | libvirt 6.9.0 | yes |
| `<qemu:commandline>` → `-set …discard_granularity` | libvirt 0.8.3 | yes |
| `<hyperv><xmm_input/>` | libvirt 10.7.0, QEMU 7.1 | **no — opt in** |
| `<hyperv><tlbflush><extended/>` | libvirt 11.0.0, QEMU 7.1 | **no — opt in** |

The last two are commented out rather than shipped, because Ubuntu 24.04 is on libvirt 10.0.0 and
Debian 12 on 9.0.0 — neither accepts either one, and a domain that will not define is worse than one
that is marginally slower. The comment beside them in `domain.xml` says how to turn them on.

**The discard granularity is set through a raw `-set` argument on purpose.** libvirt has two tidier
spellings for it and both are too new for Ubuntu 22.04, which ships libvirt 8.0.0:

| Spelling | Needs |
|---|---|
| `<blockio discard_granularity='33554432'/>` on the disk — native, taints nothing | libvirt 10.0.0 |
| `<qemu:override><qemu:frontend><qemu:property …>` | libvirt 8.2.0 |
| `<qemu:commandline>` with `-set` — what `domain.xml` ships | libvirt 0.8.3 |

`-set` is not deprecated (checked against QEMU master), and `discard_granularity` is a property of
every block device from well before QEMU 6.2. **On a host at libvirt 10.0.0 or newer, make the swap
now rather than living with it:** move the value to `<blockio>` on the disk and delete the
`<qemu:commandline>` block. That spelling needs no `xmlns:qemu` on the root element and does not mark
the domain tainted.

### Then pick the memory backing — this is the one real either/or

Backing the guest's 16 GiB with 2 MiB hugepages removes a level of page-table walk from every guest
memory access. The price is that hugepage memory is **locked**: it cannot be swapped, and it cannot be
given back, which means `freePageReporting` on the balloon stops meaning anything. So:

- **Shared host, several domains** — keep what `domain.xml` ships, and skip to the next block. Free
  page reporting hands pages back as the guest frees them, so the host's resident set follows real
  use rather than the high-water mark.
- **Host that mostly runs this guest** — reserve the pages, add `<hugepages/>` to `<memoryBacking>`,
  and set `<memballoon model='none'/>`, before you define.

```sh
echo 'vm.nr_hugepages=8300' | sudo tee /etc/sysctl.d/10-kvm-hugepages.conf   # 16 GiB / 2 MiB + headroom
sudo sysctl --system
grep Huge /proc/meminfo
```

Reserve them through `/etc/sysctl.d` rather than a bare `sysctl`, for two reasons: the reservation
survives a host reboot, and it is re-applied at each one, when memory is least fragmented. Read
`HugePages_Total` straight back — the kernel satisfies the request as far as it can and reports
success either way, so a host that has been up for weeks can hand you fewer pages than you asked for.
If it will not give them up, `hugepages=8300` on the kernel command line allocates them before
anything can fragment the memory at all.

§7 has the second check, the one that says whether the *domain* took them. Do not skip it: a domain
configured for hugepages whose pages are unavailable starts anyway, out of the ordinary pool, and
performs exactly as it did before.

### Then the site-specific values, and define

The only values you should need to change: `<uuid>`, `<mac address>` and `<source network>`. The guest
has **one** NIC, on a NAT network: it reaches the internet, and nothing outside the host can open a
connection to it. Give it no bridged second NIC — that would put an unauthenticated runner on the LAN
for no gain.

Generate a fresh MAC and check it against the network's own definition, not just against the
other domains: `virsh net-dumpxml vmnet1` lists `<host mac=... ip=...>` reservations, and a MAC
already in one belongs to another machine.

```sh
printf '52:54:00:%02x:%02x:%02x\n' $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256))
```

Adding a reservation for it is worth it — the address stops moving, and step 5's tunnel can
name it.

```sh
curl -LO https://git.griefed.de/Griefed/ServerPackCreator/raw/branch/main/misc/windows-runner/domain.xml
# edit uuid, mac, source network, and the memory backing if you chose hugepages
virsh define domain.xml
```

## 5. Install Windows — **workstation**

```sh
virsh start "$VM"
virt-viewer "$VM"
# no virt-viewer? `virsh vncdisplay "$VM"` gives :0, i.e. port 5900:
#   ssh -fNL 5900:127.0.0.1:5900 <user>@<host> && remote-viewer vnc://127.0.0.1:5900
```

In that console:

- At *"Where do you want to install Windows?"* the disk list is empty, because the virtio driver is
  not in Microsoft's installer. **Load driver → Browse →** virtio CD → `amd64\w11`.
- Name the local account **`runner`**. If setup insists on a Microsoft account: Shift+F10,
  `OOBE\BYPASSNRO`, reboot.
- **Device Manager →** each unknown device **→ Update driver →** virtio CD, for network and balloon.
- **Install the guest agent** — virtio CD → `guest-agent\qemu-ga-x86_64.msi`. `domain.xml` already
  carries the channel it talks over; without the MSI that channel stays silent. It is what makes the
  guest answerable from the host when Windows itself is not.
- **Enable RDP** — Settings → System → Remote Desktop. Step 6 pastes a UUID and a token, and a VNC
  console has no shared clipboard.

RDP answers in the guest, not on the host, so the tunnel names the guest's address on vmnet1:

```sh
virsh domifaddr "$VM" --source agent   # or omit --source once a DHCP lease exists
ssh -fNL 3389:<guest-ip>:3389 <user>@<host>
xfreerdp /v:127.0.0.1 /u:runner
```

`-f` backs ssh into the background once authenticated, so the tunnel outlives the command.

## 6. Tune and provision the guest — **guest**

Tune first, provision second. `optimise.ps1` turns off VBS and the hypervisor launch type, which only
take effect across a reboot, and the reboot is free before there is a daemon running on the machine.

UUID and Token come from Forgejo: **Settings → Actions → Runners → Create new runner**. In an
Administrator PowerShell:

```powershell
# Strips the consumer apps and tunes the guest. Nothing in it is fatal.
iwr https://git.griefed.de/Griefed/ServerPackCreator/raw/branch/main/misc/windows-runner/optimise.ps1 -OutFile optimise.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\optimise.ps1
Restart-Computer        # the VBS and hypervisor settings do not take effect before this

iwr https://git.griefed.de/Griefed/ServerPackCreator/raw/branch/main/misc/windows-runner/provision.ps1 -OutFile provision.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\provision.ps1 -Uuid <uuid> -Token <token>
```

`provision.ps1` installs Node, Git and Go, builds `forgejo-runner.exe` from source, writes
`C:\forgejo-runner\config.yml`, registers a scheduled task that starts the daemon at boot. Safe to
re-run. The runner then appears under Settings → Actions → Runners as `windows-latest:host`.

Two things in it that look wrong and are not:

- **A scheduled task, not a Windows service.** `forgejo-runner daemon` has no Service Control Manager
  support, so `sc.exe` kills it after ~30 s and Forgejo reports it as *offline* rather than failed.
- **`host.workdir_parent` is set explicitly.** Left empty, the task runs as SYSTEM and jobs unpack into
  `C:\WINDOWS\system32\config\systemprofile\.cache\act\` — the most aggressively scanned path on the
  machine, and where run 1005's 2570-file checkout stalled.

### Windows Update is yours to decide, and both answers are wrong somewhere

Left alone, Windows installs updates and reboots unattended — which, on a machine whose job is to be
available when a release pipeline asks for it, is a runner that goes offline mid-job with no error
anywhere. Turned off, it is an unpatched machine with outbound internet access. `optimise.ps1`
deliberately does neither; it is a policy decision about this host, not a tuning default. Make it now,
while the machine has no jobs to lose:

```powershell
# Download, never install unattended. The operator installs and reboots on their own schedule.
New-Item 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsUpdate\AU' -Force
Set-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsUpdate\AU' AUOptions 3 -Type DWord
Set-ItemProperty 'HKLM:\SOFTWARE\Policies\Microsoft\Windows\WindowsUpdate\AU' NoAutoUpdate 0 -Type DWord
```

## 7. Confirm it took — **workstation** and **guest**

Each setting below fails *silently* when it does not apply: the guest boots, the jobs pass, and the
tuning simply is not in effect. None of them announces itself, so run these once, on the first boot
after provisioning, and again after any change to `domain.xml`.

```sh
# The discard_granularity override is applied. Expect 33554432, not 4096.
virsh qemu-monitor-command "$VM" --hmp \
  'qom-get /machine/peripheral/ua-vda discard_granularity'
```

If that answers `Device '/machine/peripheral/ua-vda' not found`, the running QEMU predates the
definition — domain XML applies when QEMU is *launched*, and a reboot inside Windows does not relaunch
it. `virsh shutdown`, wait for `virsh domstate` to say `shut off`, `virsh start`. If it still says not
found, `virsh qemu-monitor-command "$VM" --hmp 'qom-list /machine/peripheral'` names the ids QEMU
actually assigned; `-set` addresses the disk by exactly the `<alias>` on it, and the two have to agree.

```powershell
# In the guest: VBS must read 0. 2 means it is running and this guest is a nested hypervisor.
(Get-CimInstance Win32_DeviceGuard -Namespace root\Microsoft\Windows\DeviceGuard).VirtualizationBasedSecurityStatus
```

```sh
grep Huge /proc/meminfo     # only if you chose hugepages in step 4
```

`HugePages_Free` must drop by ~8192 once the guest starts. That is the only thing that says the
backing took — the domain starts either way.

```sh
virsh cpu-stats "$VM" --total                       # twice, a minute apart, with the guest idle
virsh domstats "$VM" --block --interface
virsh domblkstat "$VM" vda                          # 'vda' for qcow2 or raw alike
```

An idle Windows guest with the Hyper-V enlightenments in `domain.xml` should cost the host a fraction
of one core. If it costs more, the guest is busy-waiting on something — the synthetic timers
(`<stimer/>`) are the usual missing piece, and `<spinlocks/>` the next.

### The one exclusion that cannot be written in advance

`optimise.ps1` excludes `C:\forgejo-runner`, which covers the runner, its work directory and anything
act places beneath them. It does **not** know where `actions/setup-java` unpacks the JDK, because that
is the runner's tool cache and nothing here fixes its location. That unpack is tens of thousands of
small files and it is the single biggest thing Defender could be scanning.

The first job's log prints the path it installed to. If it is not under `C:\forgejo-runner`, add it:

```powershell
Add-MpPreference -ExclusionPath '<the path from the log>'
(Get-MpPreference).ExclusionPath
```

## 8. Operate — **workstation**

Eject the installation media first, or a reboot lands back in Windows setup:

```sh
virsh change-media "$VM" sda --eject --config      # `virsh domblklist "$VM"` names them
virsh change-media "$VM" sdb --eject --config
virsh autostart "$VM"
```

```sh
virsh start "$VM"
virsh domstate "$VM"
virsh shutdown "$VM"        # graceful; `destroy` is the power cord
```

When it hangs, these answer in seconds and headless:

```sh
virsh domstate "$VM" --reason                      # paused? pmsuspended? crashed?
virsh domblkstat "$VM" vda                         # is the disk actually moving?
virsh qemu-monitor-command "$VM" --hmp 'info status'
```

A flat `domblkstat` with the domain still `running` is a guest wedged on I/O — a different fix from
anything inside Windows. Note that `<on_crash>restart</on_crash>` has the domain back up before you
look; set `preserve` in `domain.xml` while chasing a crash.

### Rolling back a bad provision

```sh
virsh shutdown "$VM"
sudo cp --reflink=auto /var/lib/libvirt/images/$VM.qcow2 /var/lib/libvirt/images/$VM.bak.qcow2
```

`virsh snapshot-create-as` would be nicer, but internal snapshots of a UEFI domain additionally need
the NVRAM in qcow2 format and a libvirt new enough to allow it. The copy works everywhere.

## Appendix A: retrofitting a guest that already exists

Nothing here is needed for a guest built from steps 0–8. It is for one defined before this file
carried the tuning.

Every domain change is a **cold** edit: `virsh shutdown "$VM"`, wait for `virsh domstate` to report
`shut off`, `virsh edit "$VM"`, `virsh start "$VM"`. `virsh reboot` is an ACPI reboot inside the guest
and relaunches nothing. Roll back the way §8 does: copy the image aside first.

### Two changes to know about before you make them

- **The CPU topology changes shape**, from 2 cores x 2 threads to 4 cores x 1 thread. It is the same
  four vcpus and the same socket count, but it is a different hardware description, and a Windows
  digital licence is bound to a hardware hash. Have the account that owns the licence to hand, and
  check Settings → System → Activation on the next boot.
- **Withholding `vmx`/`svm` turns a feature off underneath a guest that may be using it.** Windows
  boots without VBS and without complaint, but anything in the guest that needed a hypervisor — WSL2,
  Hyper-V, Docker Desktop — stops working rather than degrading. On this runner there is nothing in
  that set; confirm that before assuming it of another guest you copy this file to.

### `virsh edit` rejects `<qemu:commandline>` until you declare the namespace

`virsh edit` hands you the *stored* XML, and a domain defined before this change has no `xmlns:qemu`
on its root element — so pasting the block in gives you an element whose prefix is undeclared. The
first line has to become:

```xml
<domain type='kvm' xmlns:qemu='http://libvirt.org/schemas/domain/qemu/1.0'>
```

**Read the error with suspicion: it names the wrong element.** A child libvirt does not recognise
makes its *parent* fail to validate, and the message reports the parent:

```
error: XML document failed to validate against schema: Unable to validate doc against
/usr/share/libvirt/schemas/domain.rng
Extra element features in interleave
Element domain failed to validate content
```

That is `<features>` being blamed for an unknown element *inside* it. To find the real one, validate
the file directly instead of bisecting through `virsh edit`'s prompt — this gives a line number:

```sh
virsh dumpxml "$VM" > /tmp/$VM.xml      # then edit /tmp/$VM.xml
xmllint --noout --relaxng /usr/share/libvirt/schemas/domain.rng /tmp/$VM.xml
virsh define /tmp/$VM.xml
```

### Converting the disk image in place

An image created before step 3 has 64 KiB clusters and no subclusters. `qemu-img convert` rewrites it,
with the guest **shut off** — run against a disk a live QEMU has open, it copies a torn image with no
warning, and you find out at the next chkdsk.

```sh
virsh domblklist "$VM"                  # convert the file the domain actually uses
qemu-img info --backing-chain /var/lib/libvirt/images/$VM.qcow2 | grep -iE 'snapshot|backing|cluster'
```

Internal snapshots are dropped silently by the conversion unless you pass `-s`; that `grep` is how you
find out you had any. Then:

```sh
qemu-img convert -p -O qcow2 -o extended_l2=on,cluster_size=128k \
  /var/lib/libvirt/images/$VM.qcow2 /var/lib/libvirt/images/$VM.new.qcow2

qemu-img info  /var/lib/libvirt/images/$VM.new.qcow2   # cluster_size 131072, extended l2: true
qemu-img check /var/lib/libvirt/images/$VM.new.qcow2   # "No errors were found"
```

`extended l2: true` is the line that matters. A mis-parsed `-o` leaves you a perfectly valid 128 KiB
image with no subclusters, which is the one combination you do not want.

Swap by rename rather than by editing the domain. Keeping the path identical means you touch neither
the XML nor AppArmor — `virt-aa-helper` regenerates the per-domain profile from the XML path at start,
and there is no reason to re-prove a non-default image directory today:

```sh
mv /var/lib/libvirt/images/$VM.qcow2     /var/lib/libvirt/images/$VM.old.qcow2
mv /var/lib/libvirt/images/$VM.new.qcow2 /var/lib/libvirt/images/$VM.qcow2
chown --reference=/var/lib/libvirt/images/$VM.old.qcow2 /var/lib/libvirt/images/$VM.qcow2
chmod --reference=/var/lib/libvirt/images/$VM.old.qcow2 /var/lib/libvirt/images/$VM.qcow2
```

libvirt's `dynamic_ownership` chowns the disk to `libvirt-qemu:kvm` at start and hands it back at
shutdown, so the ownership copy is belt-and-braces — but a root-owned image is exactly how this fails,
with a `Permission denied` that reads like a path problem.

Start, run one job, and only then delete `$VM.old.qcow2`. It is the only rollback there is, and
keeping it costs nothing but space.

Converting to raw is the same procedure with `-O raw` and no `-o`, plus the two `domain.xml` edits
from step 3.
