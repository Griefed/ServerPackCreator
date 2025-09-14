#!/usr/bin/env bash
############################################许可证#################################################
# 版权所有 (C) 2024 Griefed
#
# 本脚本是自由软件；您可以根据自由软件基金会发布的 GNU  Lesser General Public License（版本 2.1 或更高版本）的条款重新分发和/或修改它。
#
# 本库的发布旨在有用,但不提供任何保证；甚至不包含适销性或特定用途适用性的默示保证。详见 GNU  Lesser General Public License 了解更多信息。
#
# 您应该已收到本库附带的 GNU  Lesser General Public License；如果没有,请写信给 Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
#
# 完整许可证可在 https://github.com/Griefed/ServerPackCreator/blob/main/LICENSE 查看
############################################描述#################################################
#
# 用于轻松运行服务器包的启动脚本。为了更方便地运行此脚本,可使用随服务器包一同提供的 start.bat 文件。
#
# 此启动脚本支持 Forge、NeoForge、Fabric、Quilt 和 LegacyFabric 及其支持的 Minecraft 版本。
#
# 本脚本会根据附带的 variables.txt 中的设置下载并安装模组加载器服务器。
#
# 如果未找到合适的 Java 安装,且 $JAVA 变量设置为 "java",则会自动下载并提供适合此服务器包的 Java 安装。
#
# 您可以通过在 variables.txt 中设置 RESTART 为 true 来让服务器自动重启。有关该文件中各种设置的更多信息,请查看该文件。
#
############################################注意事项#################################################
#
# 启动脚本由 ServerPackCreator SPC_SERVERPACKCREATOR_VERSION_SPC 生成。
# 用于生成此脚本的模板可在以下位置找到:
#   https://github.com/Griefed/ServerPackCreator/blob/SPC_SERVERPACKCREATOR_VERSION_SPC/serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/default_template.sh
#
# Linux 脚本旨在使用 bash 运行（由顶部的 `#!/usr/bin/env bash` 指示）,
# 即只需调用 `./start.sh` 或 `bash start.sh`。
# 使用其他方法可能可行,但也可能导致意外行为。
# 曾在 MacOS 上运行过 Linux 脚本,但 ServerPackCreator 的开发者未对其进行测试。
# 结果可能有所不同,不提供保证。
#
# 根据所设置的模组加载器,会运行不同的检查以确保服务器能够正常启动。
# 如果模组加载器检查和设置通过,将运行 Minecraft 和 EULA 检查。
# 如果一切正常,服务器将启动。
#

# 根据 Minecraft 版本,您需要不同的 Java 版本来运行服务器。
#   1.16.5 及更早版本需要 Java 8（Java 11 运行效果更好,且与 99% 的模组兼容,不妨一试）
#     Linux:
#       您可以在此处获取 Java 8 安装:https://adoptium.net/temurin/releases/?variant=openjdk8&version=8&package=jdk&arch=x64&os=linux
#       您可以在此处获取 Java 11 安装:https://adoptium.net/temurin/releases/?variant=openjdk11&version=11&package=jdk&arch=x64&os=linux
#     macOS:
#       您可以在此处获取 Java 8 安装:https://adoptium.net/temurin/releases/?variant=openjdk8&version=8&package=jdk&arch=x64&os=mac
#       您可以在此处获取 Java 11 安装:https://adoptium.net/temurin/releases/?variant=openjdk11&version=11&package=jdk&arch=x64&os=mac
#   1.18.2 及更新版本需要 Java 17（Java 18 运行效果更好,且与 99% 的模组兼容,不妨一试）
#     Linux:
#       您可以在此处获取 Java 17 安装:https://adoptium.net/temurin/releases/?variant=openjdk17&version=17&package=jdk&arch=x64&os=linux
#       您可以在此处获取 Java 18 安装:https://adoptium.net/temurin/releases/?variant=openjdk18&version=18&package=jdk&arch=x64&os=linux
#     macOS:
#       您可以在此处获取 Java 17 安装:https://adoptium.net/temurin/releases/?variant=openjdk17&version=17&package=jdk&arch=x64&os=mac
#   1.20.5 及更新版本需要 Java 21
#     Linux:
#       您可以在此处获取 Java 21 安装:https://adoptium.net/temurin/releases/?variant=openjdk21&version=21&package=jdk&arch=x64&os=linux
#     macOS:
#       您可以在此处获取 Java 21 安装:https://adoptium.net/temurin/releases/?variant=openjdk21&version=21&package=jdk&arch=x64&os=mac

# pause
# 暂停脚本执行。需要用户按下任意键才能继续执行。
pause() {
  read -n 1 -s -r -p "按任意键继续"
}

# crashServer(reason)
# 终止脚本执行,退出代码为 1。将 $1 打印到控制台。
crashServer() {
  echo "${1}"
  pause
  exit 1
}

# commandAvailable(command)
# 检查命令 $1 是否可执行。可在 if 语句中使用。
commandAvailable() {
  command -v "$1" > /dev/null 2>&1
}

# getJavaVersion
# 通过使用 -fullversion 检查 $JAVA 来设置 $JAVA_VERSION。仅存储主版本,例如 8、11、17、21。
getJavaVersion() {
  JAVA_VERSION=$("${JAVA}" -fullversion 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
  if [[ "$JAVA_VERSION" -eq 1 ]];then
    JAVA_VERSION=$("${JAVA}" -fullversion 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f2)
  fi
}

# installJava
# 运行配套脚本 "install_java.sh" 以安装此模组化 Minecraft 服务器所需的 Java 版本。
installJava() {
  echo "在您的系统上未找到合适的 Java 安装。正在进行 Java 安装。"
  . install_java.sh || crashServer "Java 安装脚本失败。请手动安装 Java $RECOMMENDED_JAVA_VERSION,或在 variables.txt 中编辑 JAVA 以指向该版本的 Java 安装。"
  if ! commandAvailable "$JAVA";then
    crashServer "Java 安装失败。找不到 $JAVA。"
  fi
}

# downloadIfNotExist(fileToCheck,fileToDownload,downloadURL)
# 检查文件 $1 是否存在。如果不存在,则从 $3 下载并存储为 $2。可在 if 语句中使用。
downloadIfNotExist() {
  if [[ ! -s "${1}" ]]; then

    echo "${1} 未找到。" >&2
    echo "正在下载 ${2}" >&2
    echo "来源:${3}" >&2

    if commandAvailable curl ; then
      curl -# -L -o "./${2}" "${3}"
    elif commandAvailable wget ; then
      wget --show-progress -O "./${2}" "${3}"
    else
      crashServer "[错误] 需要 wget 或 curl 来下载文件。"
    fi

    if [[ -s "${2}" ]]; then
      echo "下载完成。" >&2
      echo "true"
    else
      echo "false"
    fi

  else
    echo "${1} 已存在。" >&2
    echo "false"
  fi
}

# runJavaCommand(command)
# 使用 $JAVA 中设置的 Java 安装运行命令 $1。
runJavaCommand() {
  # shellcheck disable=SC2086
  "$JAVA" ${1}
}

# refreshServerJar
# 刷新用于运行 Forge 和 NeoForge 服务器的 ServerStarterJar。
# 根据 variables.txt 中的 SERVERSTARTERJAR_FORCE_FETCH 值,强制刷新 server.jar。
# 即:如果为 true,将删除 server.jar 然后重新下载。
# 根据 variables.txt 中的 SERVERSTARTERJAR_VERSION 值获取不同版本。有关此值的更多信息,请参见 variables.txt。
refreshServerJar() {
  if [[ "${SERVERSTARTERJAR_FORCE_FETCH}" == "true" ]]; then
    rm -f server.jar
  fi

  if [[ "${SERVERSTARTERJAR_VERSION}" == "latest" ]]; then
    SERVERSTARTERJAR_DOWNLOAD_URL="https://github.com/neoforged/ServerStarterJar/releases/latest/download/server.jar"
  else
    SERVERSTARTERJAR_DOWNLOAD_URL="https://github.com/neoforged/ServerStarterJar/releases/download/${SERVERSTARTERJAR_VERSION}/server.jar"
  fi

  downloadIfNotExist "server.jar" "server.jar" "${SERVERSTARTERJAR_DOWNLOAD_URL}" >/dev/null
}

# cleanServerFiles
# 清理安装程序或模组加载器服务器创建的文件,但保留服务器包文件。
# 允许更改和重新安装模组加载器、Minecraft 和模组加载器版本。
cleanServerFiles() {
  FILES_TO_REMOVE=(
    "libraries"
    "run.sh"
    "run.bat"
    "*installer.jar"
    "*installer.jar.log"
    "server.jar"
    ".mixin.out"
    "ldlib"
    "local"
    "fabric-server-launcher.jar"
    "fabric-server-launch.jar"
    ".fabric-installer"
    "fabric-installer.jar"
    "legacyfabric-installer.jar"
    ".fabric"
    "versions"
  )

  for FILE_TO_REMOVE in "${FILES_TO_REMOVE[@]}"
  do
    rm -r -v \
      "$FILE_TO_REMOVE" 2> /dev/null \
      && echo "已删除 $FILE_TO_REMOVE"
  done
}

# setupForge
# 为 $MODLOADER_VERSION 下载并安装 Forge 服务器。对于 Minecraft 1.17 及更高版本,使用 NeoForge 组的 ServerStarterJar。
# 这样做的好处是使此服务器包与大多数托管公司兼容。
setupForge() {
  echo ""
  echo "正在运行 Forge 检查和设置..."
  FORGE_INSTALLER_URL="https://files.minecraftforge.net/maven/net/minecraftforge/forge/${MINECRAFT_VERSION}-${MODLOADER_VERSION}/forge-${MINECRAFT_VERSION}-${MODLOADER_VERSION}-installer.jar"
  FORGE_JAR_LOCATION="do_not_manually_edit"

  if [[ ${SEMANTICS[1]} -le 16 ]]; then
    FORGE_JAR_LOCATION="forge.jar"
    LAUNCHER_JAR_LOCATION="forge.jar"
    SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION} nogui"

    if [[ $(downloadIfNotExist "${FORGE_JAR_LOCATION}" "forge-installer.jar" "${FORGE_INSTALLER_URL}") == "true" ]]; then

        echo "Forge 安装程序已下载。正在安装..."
        runJavaCommand "-jar forge-installer.jar --installServer"

        echo "将 forge-${MINECRAFT_VERSION}-${MODLOADER_VERSION}.jar 重命名为 forge.jar"
        mv forge-"${MINECRAFT_VERSION}"-"${MODLOADER_VERSION}".jar forge.jar
        mv forge-"${MINECRAFT_VERSION}"-"${MODLOADER_VERSION}-universal".jar forge.jar

        if [[ -s "${FORGE_JAR_LOCATION}" ]]; then

            rm -f forge-installer.jar
            echo "安装完成。forge-installer.jar 已删除。"
        else
            rm -f forge-installer.jar
            crashServer "服务器安装过程中出现问题。请几分钟后重试,并检查您的互联网连接。"
        fi
    fi
  else
    if [[ "${USE_SSJ}" == "false" ]]; then
      FORGE_JAR_LOCATION="libraries/net/minecraftforge/forge/${MINECRAFT_VERSION}-${MODLOADER_VERSION}/forge-${MINECRAFT_VERSION}-${MODLOADER_VERSION}-server.jar"
      SERVER_RUN_COMMAND="@user_jvm_args.txt @libraries/net/minecraftforge/forge/${MINECRAFT_VERSION}-${MODLOADER_VERSION}/win_args.txt nogui"

      if [[ $(downloadIfNotExist "${FORGE_JAR_LOCATION}" "forge-installer.jar" "${FORGE_INSTALLER_URL}") == "true" ]]; then
        echo "Forge 安装程序已下载。正在安装..."
        runJavaCommand "-jar forge-installer.jar --installServer"
      fi

      echo "生成 user_jvm_args.txt 从变量..."
      echo "在 variables.txt 中编辑 JAVA_ARGS。不要直接编辑 user_jvm_args.txt!"
      echo "对 user_jvm_args.txt 的手动更改将丢失!"
      rm -f user_jvm_args.txt
      {
        echo "# Xmx 和 Xms 分别设置最大和最小 RAM 使用量。"
        echo "# 它们可以是任何数字,后跟 M 或 G。"
        echo "# M 表示兆字节,G 表示千兆字节。"
        echo "# 例如,将最大值设置为 3GB:-Xmx3G"
        echo "# 将最小值设置为 2.5GB:-Xms2500M"
        echo "# 模组化服务器的一个不错的默认值是 4GB。"
        echo "# 取消下一行的注释进行设置。"
        echo "# -Xmx4G"
        echo "${JAVA_ARGS}"
      } >>user_jvm_args.txt
    else
      SERVER_RUN_COMMAND="@user_jvm_args.txt -Djava.security.manager=allow -jar server.jar --installer-force --installer ${FORGE_INSTALLER_URL} nogui"
      # 下载 ServerStarterJar 到 server.jar
      refreshServerJar

      echo "生成 user_jvm_args.txt 从变量..."
      echo "在 variables.txt 中编辑 JAVA_ARGS。不要直接编辑 user_jvm_args.txt!"
      echo "对 user_jvm_args.txt 的手动更改将丢失!"
      rm -f user_jvm_args.txt
      {
        echo "# Xmx 和 Xms 分别设置最大和最小 RAM 使用量。"
        echo "# 它们可以是任何数字,后跟 M 或 G。"
        echo "# M 表示兆字节,G 表示千兆字节。"
        echo "# 例如,将最大值设置为 3GB:-Xmx3G"
        echo "# 将最小值设置为 2.5GB:-Xms2500M"
        echo "# 模组化服务器的一个不错的默认值是 4GB。"
        echo "# 取消下一行的注释进行设置。"
        echo "# -Xmx4G"
        echo "${JAVA_ARGS}"
      } >>user_jvm_args.txt
    fi
  fi
}

# setupNeoForge
# 为 $MODLOADER_VERSION 下载并安装 NeoForge 服务器。使用 NeoForge 组的 ServerStarterJar。
# 这样做的好处是使此服务器包与大多数托管公司兼容。
setupNeoForge() {
  echo ""
  echo "正在运行 NeoForge 检查和设置..."
  echo "生成 user_jvm_args.txt 从变量..."
  echo "在 variables.txt 中编辑 JAVA_ARGS。不要直接编辑 user_jvm_args.txt!"
  echo "对 user_jvm_args.txt 的手动更改将丢失!"
  rm -f user_jvm_args.txt
  {
    echo "# Xmx 和 Xms 分别设置最大和最小 RAM 使用量。"
    echo "# 它们可以是任何数字,后跟 M 或 G。"
    echo "# M 表示兆字节,G 表示千兆字节。"
    echo "# 例如,将最大值设置为 3GB:-Xmx3G"
    echo "# 将最小值设置为 2.5GB:-Xms2500M"
    echo "# 模组化服务器的一个不错的默认值是 4GB。"
    echo "# 取消下一行的注释进行设置。"
    echo "# -Xmx4G"
    echo "${JAVA_ARGS}"
  } >>user_jvm_args.txt

  if [[ ${SEMANTICS[1]} -eq 20 ]] && [[ ${#SEMANTICS[@]} -eq 2 || ${SEMANTICS[2]} -eq 1 ]]; then
    SERVER_RUN_COMMAND="@user_jvm_args.txt -jar server.jar --installer-force --installer https://maven.neoforged.net/releases/net/neoforged/forge/${MINECRAFT_VERSION}-${MODLOADER_VERSION}/forge-${MINECRAFT_VERSION}-${MODLOADER_VERSION}-installer.jar nogui"
  else
    SERVER_RUN_COMMAND="@user_jvm_args.txt -jar server.jar --installer-force --installer ${MODLOADER_VERSION} nogui"
  fi

  refreshServerJar
}

# setupFabric
# 为 $MODLOADER_VERSION 下载并安装 Fabric 服务器。如果 $MINECRAFT_VERSION 和 $MODLOADER_VERSION 对应的 Fabric 启动器可用,
# 则下载并使用它,否则下载并使用常规的 Fabric 安装程序。
# 还会检查 Fabric 是否支持 $MINECRAFT_VERSION 和 $MODLOADER_VERSION。
setupFabric() {
  echo ""
  echo "正在运行 Fabric 检查和设置..."

  FABRIC_INSTALLER_URL="https://maven.fabricmc.net/net/fabricmc/fabric-installer/${FABRIC_INSTALLER_VERSION}/fabric-installer-${FABRIC_INSTALLER_VERSION}.jar"
  FABRIC_CHECK_URL="https://meta.fabricmc.net/v2/versions/loader/${MINECRAFT_VERSION}/${MODLOADER_VERSION}/server/json"
  IMPROVED_FABRIC_LAUNCHER_URL="https://meta.fabricmc.net/v2/versions/loader/${MINECRAFT_VERSION}/${MODLOADER_VERSION}/${FABRIC_INSTALLER_VERSION}/server/jar"

  if commandAvailable curl ; then
    FABRIC_AVAILABLE=$(curl -s -o /dev/null -w "%{http_code}" "${FABRIC_CHECK_URL}")
  elif commandAvailable wget ; then
    FABRIC_AVAILABLE=$(wget --server-response "${FABRIC_CHECK_URL}" 2>&1 | awk '/^  HTTP/{print $2}')
  fi

  if [[ "${FABRIC_AVAILABLE}" -eq "404" ]]; then
    crashServer "Fabric 不支持 Minecraft ${MINECRAFT_VERSION},Fabric ${MODLOADER_VERSION}。"
  else
    if commandAvailable curl ; then
      IMPROVED_FABRIC_LAUNCHER_AVAILABLE=$(curl -s -o /dev/null -w "%{http_code}" "${IMPROVED_FABRIC_LAUNCHER_URL}")
    elif commandAvailable wget ; then
      IMPROVED_FABRIC_LAUNCHER_AVAILABLE=$(wget --server-response "${IMPROVED_FABRIC_LAUNCHER_URL}" 2>&1 | awk '/^  HTTP/{print $2}')
    fi

    if [[ "${IMPROVED_FABRIC_LAUNCHER_AVAILABLE}" -eq "200" ]]; then
      if [[ $(downloadIfNotExist "fabric-server-launcher.jar" "fabric-server-launcher.jar" "${IMPROVED_FABRIC_LAUNCHER_URL}") == "true" ]]; then
        echo "Fabric 启动器已下载。"
      fi
      LAUNCHER_JAR_LOCATION="fabric-server-launcher.jar"
      SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION} nogui"
    else
      if [[ $(downloadIfNotExist "fabric-server-launch.jar" "fabric-installer.jar" "${FABRIC_INSTALLER_URL}") == "true" ]]; then
        echo "Fabric 安装程序已下载。正在安装..."
        runJavaCommand "-jar fabric-installer.jar server -mcversion ${MINECRAFT_VERSION} -loader ${MODLOADER_VERSION} -downloadMinecraft"

        if [[ -s "fabric-server-launch.jar" ]]; then
          rm fabric-installer.jar
          echo "安装完成。fabric-installer.jar 已删除。"
        else
          rm -f fabric-installer.jar
          crashServer "未找到 fabric-server-launch.jar。可能是 Fabric 服务器出现问题。请几分钟后重试,并检查您的互联网连接。"
        fi
      fi
      LAUNCHER_JAR_LOCATION="fabric-server-launch.jar"
      SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION} nogui"
    fi
  fi
}

# setupQuilt
# 为 $MODLOADER_VERSION 下载并安装 Quilt 服务器。
# 还会检查 Quilt 是否支持 $MINECRAFT_VERSION。
setupQuilt() {
  echo ""
  echo "正在运行 Quilt 检查和设置..."

  QUILT_INSTALLER_URL="https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/${QUILT_INSTALLER_VERSION}/quilt-installer-${QUILT_INSTALLER_VERSION}.jar"
  QUILT_CHECK_URL="https://meta.quiltmc.org/v3/versions/loader/${MINECRAFT_VERSION}"

  if commandAvailable curl ; then
    QUILT_AVAILABLE=$(curl -s -o /dev/null -w "%{http_code}" "${QUILT_CHECK_URL}")
  elif commandAvailable wget ; then
    QUILT_AVAILABLE=$(wget --server-response "${QUILT_CHECK_URL}" 2>&1 | awk '/^  HTTP/{print $2}')
  fi

  if [[ "${QUILT_AVAILABLE}" -eq "404" ]]; then
    crashServer "Quilt 不支持 Minecraft ${MINECRAFT_VERSION},Quilt ${MODLOADER_VERSION}。"
  elif [[ $(downloadIfNotExist "quilt-server-launch.jar" "quilt-installer.jar" "${QUILT_INSTALLER_URL}") == "true" ]]; then
    echo "Quilt 安装程序已下载。正在安装..."
    runJavaCommand "-jar quilt-installer.jar server -mcversion ${MINECRAFT_VERSION} -loader ${MODLOADER_VERSION} -downloadMinecraft"

    if [[ -s "quilt-server-launch.jar" ]]; then
      rm quilt-installer.jar
      echo "安装完成。quilt-installer.jar 已删除。"
    else
      rm -f quilt-installer.jar
      crashServer "未找到 quilt-server-launch.jar。可能是 Quilt 服务器出现问题。请几分钟后重试,并检查您的互联网连接。"
    fi
  fi

  LAUNCHER_JAR_LOCATION="quilt-server-launch.jar"
  SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION} nogui"
}

# setupLegacyFabric
# 为 $MODLOADER_VERSION 下载并安装 LegacyFabric 服务器。
# 还会检查 LegacyFabric 是否支持 $MINECRAFT_VERSION。
setupLegacyFabric() {
  echo ""
  echo "正在运行 LegacyFabric 检查和设置..."

  LEGACYFABRIC_INSTALLER_URL="https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/${LEGACYFABRIC_INSTALLER_VERSION}/fabric-installer-${LEGACYFABRIC_INSTALLER_VERSION}.jar"
  LEGACYFABRIC_CHECK_URL="https://meta.legacyfabric.net/v2/versions/loader/${MINECRAFT_VERSION}"

  if commandAvailable curl ; then
    LEGACYFABRIC_AVAILABLE=$(curl -s -o /dev/null -w "%{http_code}" "${LEGACYFABRIC_CHECK_URL}")
  elif commandAvailable wget ; then
    LEGACYFABRIC_AVAILABLE=$(wget --server-response ${LEGACYFABRIC_CHECK_URL}  2>&1 | awk '/^  HTTP/{print $2}')
  fi

  if [[ "${#LEGACYFABRIC_AVAILABLE}" -eq "2" ]]; then
    crashServer "LegacyFabric 不支持 Minecraft ${MINECRAFT_VERSION},LegacyFabric ${MODLOADER_VERSION}。"
  elif [[ $(downloadIfNotExist "fabric-server-launch.jar" "legacyfabric-installer.jar" "${LEGACYFABRIC_INSTALLER_URL}") == "true" ]]; then
    echo "安装程序已下载。正在安装..."
    runJavaCommand "-jar legacyfabric-installer.jar server -mcversion ${MINECRAFT_VERSION} -loader ${MODLOADER_VERSION} -downloadMinecraft"

    if [[ -s "fabric-server-launch.jar" ]]; then
      rm legacyfabric-installer.jar
      echo "安装完成。legacyfabric-installer.jar 已删除。"
    else
      rm -f legacyfabric-installer.jar
      crashServer "未找到 fabric-server-launch.jar。可能是 LegacyFabric 服务器出现问题。请几分钟后重试,并检查您的互联网连接。"
    fi

  fi

  LAUNCHER_JAR_LOCATION="fabric-server-launch.jar"
  SERVER_RUN_COMMAND="${JAVA_ARGS} -jar ${LAUNCHER_JAR_LOCATION} nogui"
}

echo "启动脚本由 ServerPackCreator SPC_SERVERPACKCREATOR_VERSION_SPC 生成。"
echo "要更改此服务器的启动设置,如 JVM 参数/标志、Minecraft 版本、模组加载器版本等,请编辑 variables.txt 文件。"


# 感谢 StackOverflow 的帮助:https://stackoverflow.com/questions/59895/how-do-i-get-the-directory-where-a-bash-script-is-located-from-within-the-script/246128#246128
# 这个小片段确保我们在包含此脚本的目录中工作。
SOURCE=${BASH_SOURCE[0]}
while [ -L "$SOURCE" ]; do # 解析 $SOURCE 直到文件不再是符号链接
  DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )
  SOURCE=$(readlink "$SOURCE")
  [[ $SOURCE != /* ]] && SOURCE=$DIR/$SOURCE # 如果 $SOURCE 是相对符号链接,我们需要相对于符号链接文件所在的路径解析它
done
DIR=$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )
cd "${DIR}" >/dev/null 2>&1 || exit

# 检查此目录的路径是否包含空格。路径中的空格容易导致问题。
if [[ "${DIR}" == *" "*  ]]; then

    echo "警告!此脚本的当前位置包含空格。这可能导致服务器崩溃!"
    echo "强烈建议将此服务器包移动到路径中不包含空格的位置!"
    echo ""
    echo "当前路径:"
    echo "${PWD}"
    echo ""
    echo -n "您确定要继续吗？(是/否):"
    read -r WHY

    if [[ "${WHY}" == "是" ]]; then
        echo "好吧。准备好面对未知的后果吧,弗里曼先生..."
    else
        crashServer "用户不希望在路径包含空格的目录中运行服务器。"
    fi
fi


# 检查并警告用户是否使用了 32 位 Java 安装。实际上,这种情况应该越来越少,
# 但偶尔还是会发生。最好提醒人们注意这一点。
"$JAVA" "-version" 2>&1 | grep -i "32-Bit" && echo "警告!检测到 32 位 Java!强烈建议使用 64 位版本的 Java!"

# 加载变量文件
if [ -f "variables.txt" ]; then
  # shellcheck source=/dev/null
  . variables.txt
else
  crashServer "错误:未找到 variables.txt 文件。请确保该文件与启动脚本位于同一目录中。"
fi

# 检查是否需要清理
if [[ "$1" == "--cleanup" ]]; then
  echo "正在运行清理..."
  cleanServerFiles
elif [[ -f "./.previousrun" ]]; then
  source "./.previousrun"
  if [[ "$PREVIOUS_MINECRAFT_VERSION" != "$MINECRAFT_VERSION" || \
        "$PREVIOUS_MODLOADER" != "$MODLOADER" || \
        "$PREVIOUS_MODLOADER_VERSION" != "$MODLOADER_VERSION" ]]; then
    echo "Minecraft 版本、模组加载器或模组加载器版本已更改。正在清理..."
    cleanServerFiles
  fi
fi

# 保存当前运行信息
echo "PREVIOUS_MINECRAFT_VERSION=${MINECRAFT_VERSION}" >"./.previousrun"
echo "PREVIOUS_MODLOADER=${MODLOADER}" >>"./.previousrun"
echo "PREVIOUS_MODLOADER_VERSION=${MODLOADER_VERSION}" >>"./.previousrun"

# 解析 Minecraft 版本号
SEMANTICS=(${MINECRAFT_VERSION//./ })

# 根据模组加载器类型执行相应设置
case ${MODLOADER} in
  "Forge")
    setupForge
    ;;
  "NeoForge")
    setupNeoForge
    ;;
  "Fabric")
    setupFabric
    ;;
  "Quilt")
    setupQuilt
    ;;
  "LegacyFabric")
    setupLegacyFabric
    ;;
  *)
    crashServer "指定的模组加载器不正确:${MODLOADER}"
esac

echo ""
# 检查 EULA 同意情况
if [[ ! -s "eula.txt" ]]; then

  echo "尚未接受 Mojang 的 EULA。要运行 Minecraft 服务器,您必须接受 Mojang 的 EULA。"
  echo "Mojang 的 EULA 可在 https://aka.ms/MinecraftEULA 查看。"
  echo "如果您同意 Mojang 的 EULA,请输入 'I agree'"
  read -r ANSWER

  if [[ "${ANSWER}" == "I agree" ]]; then
    echo "用户同意了 Mojang 的 EULA。"
    echo "# 通过将以下设置更改为 TRUE,即表示您同意我们的 EULA（https://aka.ms/MinecraftEULA）。" > eula.txt
    echo "eula=true" >> eula.txt
  else
    crashServer "用户未同意 Mojang 的 EULA。输入:${ANSWER}。除非您同意 Mojang 的 EULA,否则无法运行 Minecraft 服务器。"
  fi
else
  if ! grep -q "eula=true" "eula.txt"; then
    crashServer "您必须同意 Mojang 的 EULA 才能运行服务器。请编辑 eula.txt 并将 eula=false 更改为 eula=true。"
  fi
fi

# 显示服务器信息
echo "正在启动服务器..."
echo "Minecraft 版本:              ${MINECRAFT_VERSION}"
echo "模组加载器:                  ${MODLOADER}"
echo "模组加载器版本:              ${MODLOADER_VERSION}"
echo "LegacyFabric 安装程序版本: ${LEGACYFABRIC_INSTALLER_VERSION}"
echo "Fabric 安装程序版本:       ${FABRIC_INSTALLER_VERSION}"
echo "Quilt 安装程序版本:        ${QUILT_INSTALLER_VERSION}"
echo "Java 参数:                  ${JAVA_ARGS}"
echo "附加参数:                   ${ADDITIONAL_ARGS}"
echo "Java 路径:                  ${JAVA}"
echo "等待用户输入:               ${WAIT_FOR_USER_INPUT}"

# 如果需要等待用户输入
if [[ "${WAIT_FOR_USER_INPUT}" == "true" ]]; then
  pause
fi

# 启动服务器的函数
start_server() {
  if [[ "${USE_SSJ}" == "true" && ( "${MODLOADER}" == "Forge" || "${MODLOADER}" == "NeoForge" ) ]]; then
    # 使用 ServerStarterJar 启动
    eval "\"${JAVA}\" ${SERVER_RUN_COMMAND} ${ADDITIONAL_ARGS}"
  else
    # 常规启动方式
    eval "\"${JAVA}\" ${SERVER_RUN_COMMAND} ${ADDITIONAL_ARGS}"
  fi
  return $?
}

# 启动服务器并处理重启逻辑
if [[ "${RESTART}" == "true" ]]; then
  while true; do
    start_server
    EXIT_CODE=$?
    echo "服务器已停止,退出代码:${EXIT_CODE}"
    if [[ ${EXIT_CODE} -eq 0 ]]; then
      echo "服务器正常退出,不重启。"
      break
    else
      echo "3 秒后重启服务器..."
      sleep 3
    fi
  done
else
  start_server
fi

# 完成后暂停（如果需要）
if [[ "${WAIT_FOR_USER_INPUT}" == "true" ]]; then
  pause
fi
