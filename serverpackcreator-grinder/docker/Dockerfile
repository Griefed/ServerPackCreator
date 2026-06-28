# Runtime image for the ServerPackCreator grinder.
#
# It boots an SPC-generated server pack (its `start.sh`) inside an isolated container. The pack's
# start.sh is SELF-CONTAINED — at first boot it installs the modloader and the Minecraft server
# itself:
#   * Forge / NeoForge (1.17+) -> the neoforged ServerStarterJar
#   * Fabric                   -> fabric-installer / fabric-server-launch(er).jar
#   * Quilt                    -> quilt-installer.jar
#   * LegacyFabric             -> the LegacyFabric installer
# ...so this image does NOT special-case loaders. It only needs the shell tooling the template
# relies on (bash, curl/wget, gawk, tar/gzip, ca-certificates) plus a JDK.
#
# Multiple JDKs are bundled so the grinder can point $JAVA at the version the modpack's Minecraft
# requires, with NO Java download (keeping the actual mod-boots runnable under `--network none`):
#   Minecraft <= 1.16  -> Java 8 ; 1.17 .. 1.20.4 -> Java 17 ; 1.20.5+ -> Java 21
#
# Build:   docker build -t spc-grinder-runtime:latest serverpackcreator-grinder/docker
# Running is driven by ContainerServerRunner (command: `bash start.sh`, workdir: /srv/pack).

FROM debian:bookworm-slim

# Tools the SPC start.sh template invokes: bash, curl (wget as fallback), gawk, tar/gzip, and
# ca-certificates/gnupg for HTTPS downloads + the Adoptium apt key.
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        bash curl wget ca-certificates gawk tar gzip gnupg; \
    rm -rf /var/lib/apt/lists/*

# Eclipse Temurin JDKs 8/17/21/25 from the Adoptium apt repository. This set MUST match
# ImageJavaRuntimes.bundledMajors: the grinder only boots Minecraft versions whose SPC-declared
# required Java is bundled here (a too-new version is skipped, never run on the wrong JDK). 25 (LTS)
# covers the current release Minecraft 26.2 (java-runtime-epsilon); the Java-26 snapshots are skipped
# by the release-gate, so 26 is deliberately not bundled. To extend coverage to a newer Minecraft, add
# its JDK below AND to ImageJavaRuntimes.bundledMajors.
RUN set -eux; \
    mkdir -p /etc/apt/keyrings; \
    curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg; \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" \
        > /etc/apt/sources.list.d/adoptium.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends temurin-8-jdk temurin-17-jdk temurin-21-jdk temurin-25-jdk; \
    rm -rf /var/lib/apt/lists/*

# Stable, arch-independent symlinks the grinder points $JAVA at (Temurin installs to
# /usr/lib/jvm/temurin-N-jdk-<arch>, e.g. -amd64 / -arm64).
RUN set -eux; \
    ln -s /usr/lib/jvm/temurin-8-jdk-*  /opt/java-8; \
    ln -s /usr/lib/jvm/temurin-17-jdk-* /opt/java-17; \
    ln -s /usr/lib/jvm/temurin-21-jdk-* /opt/java-21; \
    ln -s /usr/lib/jvm/temurin-25-jdk-* /opt/java-25

# Default to Java 21; the grinder overrides $JAVA per Minecraft version (via the pack's variables.txt).
ENV JAVA=/opt/java-21/bin/java
ENV PATH=/opt/java-21/bin:${PATH}

# Non-root by default: the boot runs unprivileged. uid/gid 1000 matches ContainerServerRunner's
# `--user` default; the bind-mounted /srv/pack must be writable by this uid on the host.
RUN groupadd -g 1000 grinder && useradd -u 1000 -g 1000 -m -s /bin/bash grinder
USER 1000:1000

WORKDIR /srv/pack

# Overridden by ContainerServerRunner (which passes `bash start.sh`); a sane default for manual runs.
CMD ["bash", "start.sh"]
