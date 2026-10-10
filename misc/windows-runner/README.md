# A Windows Forgejo runner on a Linux x86_64 host

One Windows guest on libvirt/KVM, because `jpackage` cannot cross-compile ServerPackCreator's Windows
app-image. Why the guest exists and what the job does:
[`../../claude-docs/WINDOWS-RUNNER.md`](../../claude-docs/WINDOWS-RUNNER.md).

Blocks are marked **host**, **workstation** or **guest**. Everything but the Windows installation
itself can be driven from your workstation.

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

## 1. Packages — **host**

```sh
sudo apt install -y qemu-system-x86 libvirt-daemon-system swtpm swtpm-tools ovmf
# Fedora/RHEL: qemu-kvm libvirt swtpm swtpm-tools edk2-ovmf

sudo usermod -aG libvirt "$USER" && newgrp libvirt
systemctl is-active libvirtd && test -w /dev/kvm && echo ready
```

`swtpm` is required: Windows 11 will not install without TPM 2.0. `virt-viewer` is not — it belongs on
your workstation.

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

## 3. Define the domain — **host**

[`domain.xml`](domain.xml) is the whole definition, commented inline.

```sh
sudo qemu-img create -f qcow2 /var/lib/libvirt/images/$VM.qcow2 128G
sudo mkdir -p /var/lib/libvirt/qemu/nvram

curl -LO https://git.griefed.de/Griefed/ServerPackCreator/raw/branch/main/misc/windows-runner/domain.xml
virsh define domain.xml
```

Site-specific, and the only values you should need to change: `<uuid>`, `<mac address>` and
`<source network>`. The guest has **one** NIC, on a NAT network: it reaches the internet, and
nothing outside the host can open a connection to it. Give it no bridged second NIC — that would
put an unauthenticated runner on the LAN for no gain.

Generate a fresh MAC and check it against the network's own definition, not just against the
other domains: `virsh net-dumpxml vmnet1` lists `<host mac=... ip=...>` reservations, and a MAC
already in one belongs to another machine.

```sh
printf '52:54:00:%02x:%02x:%02x\n' $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256))
```

Adding a reservation for it is worth it — the address stops moving, and step 4's tunnel can
name it.

## 4. Install Windows — **workstation**

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
- **Enable RDP** — Settings → System → Remote Desktop. Step 5 pastes a UUID and a token, and a VNC
  console has no shared clipboard.

RDP answers in the guest, not on the host, so the tunnel names the guest's address on vmnet1:

```sh
virsh domifaddr "$VM" --source agent   # or omit --source once a DHCP lease exists
ssh -fNL 3389:<guest-ip>:3389 <user>@<host>
xfreerdp /v:127.0.0.1 /u:runner
```

`-f` backs ssh into the background once authenticated, so the tunnel outlives the command.

## 5. Build the runner and register it — **guest**

UUID and Token come from Forgejo: **Settings → Actions → Runners → Create new runner**. In an
Administrator PowerShell:

```powershell
# optional: strips the consumer apps and tunes the guest. Nothing in it is fatal.
iwr https://git.griefed.de/Griefed/ServerPackCreator/raw/branch/main/misc/windows-runner/optimise.ps1 -OutFile optimise.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\optimise.ps1

iwr https://git.griefed.de/Griefed/ServerPackCreator/raw/branch/main/misc/windows-runner/provision.ps1 -OutFile provision.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\provision.ps1 -Uuid <uuid> -Token <token>
```

Installs Node, Git and Go, builds `forgejo-runner.exe` from source, writes
`C:\forgejo-runner\config.yml`, registers a scheduled task that starts the daemon at boot. Safe to
re-run. The runner then appears under Settings → Actions → Runners as `windows-latest:host`.

Two things in it that look wrong and are not:

- **A scheduled task, not a Windows service.** `forgejo-runner daemon` has no Service Control Manager
  support, so `sc.exe` kills it after ~30 s and Forgejo reports it as *offline* rather than failed.
- **`host.workdir_parent` is set explicitly.** Left empty, the task runs as SYSTEM and jobs unpack into
  `C:\WINDOWS\system32\config\systemprofile\.cache\act\` — the most aggressively scanned path on the
  machine, and where run 1005's 2570-file checkout stalled.

## 6. Operate — **workstation**

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
