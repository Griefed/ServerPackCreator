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
#   Minecraft <= 1.16.5 -> 8 ; 1.17 .. 1.17.1 -> 16 ; 1.18 .. 1.20.4 -> 17 ; 1.20.5 .. 1.21.x -> 21 ; 26.x -> 25
#
# Build:   docker build -t spc-grinder-runtime:latest serverpackcreator-grinder/docker
# Running is driven by ContainerServerRunner (command: `bash start.sh`, workdir: /srv/pack).

FROM debian:bookworm-slim@sha256:734a72b3d6142a71993b4bc580ca01d8ca687f77a2c422d96a961d3376b179bc

# Tools the SPC start.sh template invokes: bash, curl (wget as fallback), gawk, tar/gzip, and
# ca-certificates/gnupg for HTTPS downloads + the Adoptium apt key.
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        bash curl wget ca-certificates gawk tar gzip gnupg; \
    rm -rf /var/lib/apt/lists/*

# Eclipse Temurin JDKs 8/17/21/25 from the Adoptium apt repository. The image's full set (these plus
# Zulu 16 below) MUST match ImageJavaRuntimes.IMAGE_BUNDLED_MAJORS, which RuntimeImageJdkSetTest holds to the
# /opt/java-<major> links at the end of this file: the grinder only boots Minecraft versions whose SPC-declared
# required Java is bundled here (a too-new version is skipped, never run on the wrong JDK). 25 (LTS)
# covers the current release Minecraft 26.2 (java-runtime-epsilon); the Java-26 snapshots are skipped
# by the release-gate, so 26 is deliberately not bundled. To extend coverage to a newer Minecraft, add
# its JDK below AND to ImageJavaRuntimes.IMAGE_BUNDLED_MAJORS.
#
# The dearmored keyring is checked against the fingerprint Adoptium publishes at
# https://adoptium.net/installation/gpg-verification/ before apt reads it: it must hold exactly one primary key,
# and it must be that one. A key rotation fails the build here rather than being trusted silently.
ARG ADOPTIUM_KEY_FINGERPRINT=3B04D753C9050D9A5D343F39843C48A565F8F04B
RUN set -eux; \
    mkdir -p /etc/apt/keyrings; \
    curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg; \
    test "$(gpg --show-keys --with-colons /etc/apt/keyrings/adoptium.gpg \
        | awk -F: '$1 == "pub" { keys++ } $1 == "fpr" && !seen { primary = $10; seen = 1 } END { print keys ":" primary }')" \
        = "1:${ADOPTIUM_KEY_FINGERPRINT}"; \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" \
        > /etc/apt/sources.list.d/adoptium.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        temurin-8-jdk   \
        temurin-17-jdk  \
        temurin-21-jdk  \
        temurin-25-jdk; \
    rm -rf /var/lib/apt/lists/*

# Azul Zulu JDK 16 from Azul's apt repository, for Minecraft 1.17 and 1.17.1, whose server manifests declare
# Java 16 (start.sh accepts only the exact major). Adoptium publishes no Temurin 16 package for bookworm; Azul
# still serves zulu16 for amd64 and arm64. Installation follows https://docs.azul.com/core/install/debian, and the
# dearmored keyring is checked against the fingerprint Azul publishes at https://www.azul.com/product-signing-keys/
# exactly as the Adoptium key is above. The headless JDK is enough: a dedicated server opens no window.
ARG AZUL_KEY_FINGERPRINT=27BC0C8CB3D81623F59BDADCB1998361219BD9C9
RUN set -eux; \
    curl -fsSL https://repos.azul.com/azul-repo.key \
        | gpg --dearmor -o /etc/apt/keyrings/azul.gpg; \
    test "$(gpg --show-keys --with-colons /etc/apt/keyrings/azul.gpg \
        | awk -F: '$1 == "pub" { keys++ } $1 == "fpr" && !seen { primary = $10; seen = 1 } END { print keys ":" primary }')" \
        = "1:${AZUL_KEY_FINGERPRINT}"; \
    echo "deb [signed-by=/etc/apt/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" \
        > /etc/apt/sources.list.d/zulu.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends zulu16-ca-jdk-headless; \
    rm -rf /var/lib/apt/lists/*

# Stable, arch-independent symlinks the grinder points $JAVA at (Temurin installs to
# /usr/lib/jvm/temurin-N-jdk-<arch>, Zulu to /usr/lib/jvm/zulu16-ca-<arch>, e.g. -amd64 / -arm64).
RUN set -eux; \
    ln -s /usr/lib/jvm/temurin-8-jdk-*  /opt/java-8;  \
    ln -s /usr/lib/jvm/zulu16-ca-*      /opt/java-16; \
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
