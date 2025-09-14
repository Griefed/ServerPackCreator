###############################################################################################
# 版权所有 (C) 2024 Griefed
#
# 本脚本是自由软件；您可以根据自由软件基金会发布的 GNU  Lesser General Public License（版本 2.1 或更高版本）的条款重新分发和/或修改它。
#
# 本库的发布旨在有用，但不提供任何保证；甚至不包含适销性或特定用途适用性的默示保证。详见 GNU  Lesser General Public License 了解更多信息。
#
# 您应该已收到本库附带的 GNU  Lesser General Public License；如果没有，请写信给 Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
#
# 完整许可证可在 https://github.com/Griefed/ServerPackCreator/blob/main/LICENSE 查看
###############################################################################################

<#
    .SYNOPSIS

    用于轻松运行服务器包的启动脚本。为了更方便地运行此脚本，可运行随服务器包一同提供的 start.bat 文件。

    .DESCRIPTION

    支持 Forge、NeoForge、Fabric、Quilt 和 LegacyFabric 及其支持的 Minecraft 版本的启动脚本。

    本脚本会根据附带的 variables.txt 中的设置下载并安装模组加载器服务器。

    随服务器包一同提供的还有一个批处理文件，可更方便地运行此脚本。如果未找到合适的 Java 安装，且您的 $JAVA 变量设置为 "java"，
    则还会下载并提供适合此服务器包的 Java 安装。

    您可以通过在 variables.txt 中设置 RESTART 为 true 来让服务器自动重启。有关该文件中各种设置的更多信息，请查看该文件。

    .NOTES

    启动脚本由 ServerPackCreator SPC_SERVERPACKCREATOR_VERSION_SPC 生成。
    用于生成此脚本的模板可在以下位置找到：
    https://github.com/Griefed/ServerPackCreator/blob/SPC_SERVERPACKCREATOR_VERSION_SPC/serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/default_template.ps1

    默认情况下，在您的系统上可能禁用了从未知来源运行 Powershell 脚本的功能。
    因此，您可能暂时无法运行 start.ps1 脚本。您需要先允许运行未签名的脚本。有关如何
    启用/允许使用 Powershell 运行未签名脚本的简要说明，请参见 https://superuser.com/a/106363。
      您可以从常规 PowerShell 运行 `start-process PowerShell -verb runas "Set-ExecutionPolicy RemoteSigned"` 以允许运行启动脚本。
      注意：
          请记住，这会给您的系统带来安全风险。完成上述链接中的更改后，
          您可以运行任何 Powershell 脚本，因此可能会给您的系统带来各种安全风险。因此，运行未知来源的脚本时请务必小心。

    默认情况下，如果脚本路径包含空格，Powershell 脚本无法通过双击打开。如果您想解决此问题或想了解更多有关此行为的信息，
    本文详细介绍了这一点：https://blog.danskingdom.com/fix-problem-where-windows-powershell-cannot-run-script-whose-path-contains-spaces/
    这要归功于微软。ServerPackCreator 的开发者对此无能为力。
    您应该做的是：
      在资源管理器中，浏览到包含您的服务器包的目录。
      在目录内的空白处按住 Shift 键并右键单击
      点击"在此处打开 PowerShell 窗口"
      输入 ".\start.ps1"（不带引号），然后按 Enter 键

      注意：
          请记住，当处理路径中包含空格的文件时，事情可能仍然会出错。如果
          即使尝试了上述链接中的修复方法，路径中带有空格时仍然出现问题，那么我建议将文件移动到路径中不包含空格的文件夹中。

    根据所设置的模组加载器，会运行不同的检查以确保服务器能够正常启动。
    如果模组加载器检查和设置通过，将运行 Minecraft 和 EULA 检查。
    如果一切正常，服务器将启动。


    根据 Minecraft 版本，您需要不同的 Java 版本来运行服务器。
      1.16.5 及更早版本需要 Java 8（Java 11 运行效果更好，且与 99% 的模组兼容，不妨一试）
        您可以在此处获取 Java 8 安装：https://adoptium.net/temurin/releases/?variant=openjdk8&version=8&package=jdk&arch=x64&os=windows
        您可以在此处获取 Java 11 安装：https://adoptium.net/temurin/releases/?variant=openjdk11&version=11&package=jdk&arch=x64&os=windows
      1.18.2 及更新版本需要 Java 17（Java 18 运行效果更好，且与 99% 的模组兼容，不妨一试）
        您可以在此处获取 Java 17 安装：https://adoptium.net/temurin/releases/?variant=openjdk17&version=17&package=jdk&arch=x64&os=windows
        您可以在此处获取 Java 18 安装：https://adoptium.net/temurin/releases/?variant=openjdk18&version=18&package=jdk&arch=x64&os=windows
      1.20.5 及更新版本需要 Java 21
        您可以在此处获取 Java 21 安装：https://adoptium.net/temurin/releases/?variant=openjdk21&version=21&package=jdk&arch=x64&os=windows

    .INPUTS

    无。您不能将对象通过管道传递给 start.ps1。

    .OUTPUTS

    无。start.ps1 不会生成任何可用于管道的输出。
#>


Function PauseScript
{
    Write-Host "按任意键继续" -ForegroundColor Yellow
    $host.ui.RawUI.ReadKey("NoEcho,IncludeKeyDown") > $null

<#
    .SYNOPSIS

    暂停脚本执行，直到用户按下任意键。
#>
}

Function CrashServer
{
    param ($Message)
    Write-Host "${Message}"
    PauseScript
    exit 1

<#
    .SYNOPSIS

    终止脚本执行，退出代码为 1。将 $Reason 打印到控制台。

    .PARAMETER Message
    导致服务器崩溃的原因。将打印到控制台.
#>
}

Function CommandAvailable
{
    param ($CmdName)
    return [bool](Get-Command -Name $CmdName -ErrorAction SilentlyContinue)

<#
    .SYNOPSIS

    检查命令 $cmdname 是否可执行。可在 if 语句中使用.

    .PARAMETER CmdName
    要检查是否可用的命令.
#>
}

Function GetJavaVersion()
{
    $JavaFullversion = CMD /C "`"${Java}`" -fullversion 2>&1"
    $JavaFullversion = $JavaFullversion.Substring($JavaFullversion.IndexOf('"')+1).TrimEnd('"').Split('.')
    $script:JavaVersion = $JavaFullversion[0]

    if ([int]$JavaFullversion[0] -eq 1)
    {
        $script:JavaVersion = $JavaFullversion[1]
    }

<#
    .SYNOPSIS

    通过使用 -fullversion 检查 $Java 来设置 $JavaVersion。仅存储主版本，例如 8、11、17、21.
#>
}

Function InstallJava()
{
    Write-Host "未在您的系统上找到合适的 Java 安装。正在进行 Java 安装."
    . .\install_java.ps1
    RunJavaInstallation
    if (!(CommandAvailable -cmdname "${Java}"))
    {
        CrashServer "Java 安装失败。找不到 ${Java}."
    }

<#
    .SYNOPSIS

    加载配套脚本 "install_java.ps1" 并运行其中包含的 "Global:RunJavaInstallation" 函数，以安装此模组化 Minecraft 服务器所需的 Java 版本.
#>
}

Function DeleteFileSilently
{
    param ($FileToDelete)
    $ErrorActionPreference = "SilentlyContinue";
    if ((Get-Item "${FileToDelete}").PSIsContainer)
    {
        Remove-Item "${FileToDelete}" -Recurse
    }
    else
    {
        Remove-Item "${FileToDelete}"
    }
    $ErrorActionPreference = "Continue";

<#
    .SYNOPSIS

    安静/默默地从文件系统中删除指定的文件。如果指定了文件夹，则会递归删除整个文件夹.

    .PARAMETER FileToDelete
    要静默删除的文件或文件夹，不在控制台打印消息或错误.
#>
}

Function WriteFileUTF8NoBom
{
    param ($FilePath, $Content)
    $AbsolutePath = Join-Path -Path "$BaseDir" -ChildPath "$FilePath"
    New-Item $AbsolutePath -type file
    $Utf8NoBomEncoding = New-Object System.Text.UTF8Encoding $False
    [IO.File]::WriteAllLines(($FilePath | Resolve-Path), $Content, $Utf8NoBomEncoding)

<#
    .SYNOPSIS

    使用 UTF-8 编码写入文本文件，但不包含 BOM。NeoForge 项目的 ServerStarterJar 在使用 "user_jvm_args.txt" 安装和运行 Forge 和 NeoForge 服务器时，需要无 BOM 的 UTF-8 文件".

    .PARAMETER FilePath
    要写入的文件的路径。此函数会创建该文件，因此无需自己创建。路径必须相对于脚本。函数会负责将其写入脚本工作目录的根目录.

    .PARAMETER Content
    要写入文件的内容.
#>
}

Function global:RunJavaCommand
{
    param ($CommandToRun)
    CMD /C "`"${Java}`" ${CommandToRun}"

<#
    .SYNOPSIS

    使用 $Java 中设置的 Java 安装运行传递的字符串作为 Java 命令.

    .PARAMETER CommandToRun
    要作为 Java 命令运行的命令.
#>
}

Function DownloadIfNotExists
{
    param ($FileToCheck, $FileToDownload, $DownloadURL)
    if (!(Test-Path -Path $FileToCheck -PathType Leaf))
    {
        Write-Host "${FileToCheck} 未找到."
        Write-Host "正在下载 ${FileToDownload}"
        Write-Host "来源 ${DownloadURL}"
        Invoke-WebRequest -URI "${DownloadURL}" -OutFile "${FileToDownload}"
        if (Test-Path -Path "${FileToDownload}" -PathType Leaf)
        {
            Write-Host "下载完成."
            return $true
        }
        else
        {
            return $false
        }
    }
    else
    {
        Write-Host "${FileToCheck} 已存在."
        return $false
    }

<#
    .SYNOPSIS

    检查 $FileToCheck 是否存在。如果不存在，则从 $DownloadURL 下载并存储为 $FileToDownload。可在 if 语句中使用.

    .PARAMETER FileToCheck
    要检查是否存在的文件.

    .PARAMETER FileToDownload
    用于存储下载内容的文件名.

    .PARAMETER DownloadURL
    从中下载文件的 URL.

    .OUTPUTS

    布尔值。如果文件已下载且存在，则为 $true，否则为 $false.
#>
}

Function global:RefreshServerJar
{
    if ("${ServerStarterJarForceFetch}" -eq "true")
    {
        DeleteFileSilently  'server.jar'
    }

    $ServerStarterJarDownloadURL = ""
    if ("${ServerStarterJarVersion}" -eq "latest")
    {
        $ServerStarterJarDownloadURL = "https://github.com/neoforged/ServerStarterJar/releases/latest/download/server.jar"
    }
    else
    {
        $ServerStarterJarDownloadURL = "https://github.com/neoforged/ServerStarterJar/releases/download/${ServerStarterJarVersion}/server.jar"
    }

    DownloadIfNotExists "server.jar" "server.jar" "${ServerStarterJarDownloadURL}"

<#
    .SYNOPSIS

   刷新用于运行 Forge 和 NeoForge 服务器的 ServerStarterJar。
    根据 variables.txt 中的 SERVERSTARTERJAR_FORCE_FETCH 值，强制刷新 server.jar。
    即：如果为 true，将删除 server.jar 然后重新下载。
    根据 variables.txt 中的 SERVERSTARTERJAR_VERSION 值获取不同版本。有关此值的更多信息，请参见 variables.txt.
#>
}

Function global:CleanServerFiles
{
    $FilesToRemove = @(
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
    $ErrorActionPreference = "SilentlyContinue";
    ForEach ($FileToRemove in $FilesToRemove) {
        Remove-Item "${FileToRemove}" -Recurse -Verbose -ErrorAction SilentlyContinue
    }
    $ErrorActionPreference = "Continue";

<#
    .SYNOPSIS

    清理安装程序或模组加载器服务器创建的文件，但保留服务器包文件。
    允许更改和重新安装模组加载器、Minecraft 和模组加载器版本.
#>
}

Function global:SetupForge
{
    ""
    "正在运行 Forge 检查和设置..."
    $ForgeInstallerUrl = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/${MinecraftVersion}-${ModLoaderVersion}/forge-${MinecraftVersion}-${ModLoaderVersion}-installer.jar"
    $ForgeJarLocation = "do_not_manually_edit"
    if ([int]$Semantics[1] -le 16)
    {
        $ForgeJarLocation = "forge.jar"
        $script:LauncherJarLocation = "forge.jar"
        $script:ServerRunCommand = "${JavaArgs} -jar ${LauncherJarLocation} nogui"

        if ((DownloadIfNotExists "${ForgeJarLocation}" "forge-installer.jar" "${ForgeInstallerUrl}"))
        {
            "Forge 安装程序已下载。正在安装..."
            RunJavaCommand "-jar forge-installer.jar --installServer"

            "将 forge-${MinecraftVersion}-${ModLoaderVersion}.jar 重命名为 forge.jar"
            Move-Item "forge-${MinecraftVersion}-${ModLoaderVersion}.jar" 'forge.jar'
            Move-Item "forge-${MinecraftVersion}-${ModLoaderVersion}-universal.jar" 'forge.jar'

            if ((Test-Path -Path "${ForgeJarLocation}" -PathType Leaf))
            {
                DeleteFileSilently  'forge-installer.jar'
                "安装完成. forge-installer.jar 已删除."
            }
            else
            {
                DeleteFileSilently  'forge-installer.jar'
                CrashServer "服务器安装过程中出现问题.请几分钟后重试,并检查您的互联网连接."
            }
        }
    }
    else
    {
        if (${UseSSJ} -eq "false")
        {
            $ForgeJarLocation = "libraries/net/minecraftforge/forge/${MinecraftVersion}-${ModLoaderVersion}/forge-${MinecraftVersion}-${ModLoaderVersion}-server.jar"
            $script:ServerRunCommand = "@user_jvm_args.txt @libraries/net/minecraftforge/forge/${MinecraftVersion}-${ModLoaderVersion}/win_args.txt nogui"
            if ((DownloadIfNotExists "${ForgeJarLocation}" "forge-installer.jar" "${ForgeInstallerUrl}"))
            {
                "Forge 安装程序已下载。正在安装..."
                RunJavaCommand "-jar forge-installer.jar --installServer"
            }
        }
        else
        {
            $script:ServerRunCommand = "@user_jvm_args.txt -Djava.security.manager=allow -jar server.jar --installer-force --installer ${ForgeInstallerUrl} nogui"
            # Download ServerStarterJar to server.jar
            RefreshServerJar
        }

        Write-Host "生成 user_jvm_args.txt 从变量..."
        Write-Host "在 variables.txt 中编辑 JAVA_ARGS,不要直接编辑 user_jvm_args.txt!"
        Write-Host "对 user_jvm_args.txt 的手动更改将丢失!"
        DeleteFileSilently  'user_jvm_args.txt'
        $Content = "# Xmx 和 Xms 分别设置最大和最小 RAM 使用量`n" +
                "# 它们可以是任何数字，后跟 M 或 G.`n" +
                "# M 表示兆字节,G 表示千兆字节.`n" +
                "# 例如，将最大值设置为 3GB:-Xmx3G`n" +
                "# 将最小值设置为 2.5GB:-Xms2500M`n" +
                "# 模组化服务器的一个不错的默认值是 4GB.`n" +
                "# 取消下一行的注释进行设置.`n" +
                "# -Xmx4G`n" +
                "${script:JavaArgs}"
        WriteFileUTF8NoBom "user_jvm_args.txt" $Content
    }

<#
    .SYNOPSIS

    为 $ModLoaderVersion 下载并安装 Forge 服务器.对于 Minecraft 1.17 及更高版本,使用 NeoForge 组的 ServerStarterJar.
    这样做的好处是使此服务器包与大多数托管公司兼容.
#>
}

# 如果模组加载器 = NeoForge，运行 NeoForge 特定检查
Function global:SetupNeoForge
{
    ""
    "正在运行 NeoForge 检查和设置..."
    Write-Host "生成 user_jvm_args.txt 从变量..."
    Write-Host "在 variables.txt 中编辑 variables.txt. 不要直接编辑 user_jvm_args.txt!"
    Write-Host "对 user_jvm_args.txt 的手动更改将丢失!"
    DeleteFileSilently  'user_jvm_args.txt'
    $Content = "# Xmx 和 Xms 分别设置最大和最小 RAM 使用量`n" +
            "# 它们可以是任何数字，后跟 M 或 G.`n" +
            "# M 表示兆字节,G 表示千兆字节.`n" +
            "# 例如，将最大值设置为 3GB:-Xmx3G`n" +
            "# 将最小值设置为 2.5GB:-Xms2500M`n" +
            "# 模组化服务器的一个不错的默认值是 4GB.`n" +
            "# 取消下一行的注释进行设置.`n" +
            "# -Xmx4G`n" +
            "${script:JavaArgs}"
    WriteFileUTF8NoBom "user_jvm_args.txt" $Content

    if ([int]$Semantics[1] -eq 20 -And ($Semantics.count -eq 2 -Or [int]$Semantics[2] -eq 1))
    {
        $script:ServerRunCommand = "@user_jvm_args.txt -jar server.jar --installer-force --installer https://maven.neoforged.net/releases/net/neoforged/forge/${MinecraftVersion}-${ModLoaderVersion}/forge-${MinecraftVersion}-${ModLoaderVersion}-installer.jar nogui"
    }
    else
    {
        $script:ServerRunCommand = "@user_jvm_args.txt -jar server.jar --installer-force --installer ${ModLoaderVersion} nogui"
    }

    RefreshServerJar

<#
    .SYNOPSIS

    下载并安装适用于 $ModLoaderVersion 的 NeoForge 服务端，使用 NeoForge 官方提供的 ServerStarterJar。
    这样做的好处是：该服务端包能与绝大多数主机商的控制面板兼容.
#>
}

Function global:SetupFabric
{
    ""
    "正在运行 Fabric 检查和设置..."
    $FabricInstallerUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/${FabricInstallerVersion}/fabric-installer-${FabricInstallerVersion}.jar"
    $ImprovedFabricLauncherUrl = "https://meta.fabricmc.net/v2/versions/loader/${MinecraftVersion}/${ModLoaderVersion}/${FabricInstallerVersion}/server/jar"
    $ErrorActionPreference = "SilentlyContinue";
    $script:ImprovedFabricLauncherAvailable = [int][System.Net.WebRequest]::Create("${ImprovedFabricLauncherUrl}").GetResponse().StatusCode
    $ErrorActionPreference = "Continue";
    if ("${ImprovedFabricLauncherAvailable}" -eq "200")
    {
        "改进版 Fabric 服务器启动器已可用……"
        "改进后的启动器将用于运行此 Fabric 服务器."
        $script:LauncherJarLocation = "fabric-server-launcher.jar"
        (DownloadIfNotExists "${script:LauncherJarLocation}" "${script:LauncherJarLocation}" "${ImprovedFabricLauncherUrl}") > $null
    }
    else
    {
        try
        {
            $ErrorActionPreference = "SilentlyContinue";
            $FabricAvailable = [int][System.Net.WebRequest]::Create("https://meta.fabricmc.net/v2/versions/loader/${MinecraftVersion}/${ModLoaderVersion}/server/json").GetResponse().StatusCode
            $ErrorActionPreference = "Continue";
        }
        catch
        {
            $FabricAvailable = "400"
        }
        if ("${FabricAvailable}" -ne "200")
        {
            CrashServer "Fabric 不支持 Minecraft ${MinecraftVersion}, Fabric ${ModLoaderVersion}."
        }
        if ((DownloadIfNotExists "fabric-server-launch.jar" "fabric-installer.jar" "${FabricInstallerUrl}"))
        {
            "Fabric 安装程序已下载..."
            $script:LauncherJarLocation = "fabric-server-launch.jar"
            RunJavaCommand "-jar fabric-installer.jar server -mcversion ${MinecraftVersion} -loader ${ModLoaderVersion} -downloadMinecraft"
            if ((Test-Path -Path 'fabric-server-launch.jar' -PathType Leaf))
            {
                DeleteFileSilently '.fabric-installer' -Recurse
                DeleteFileSilently 'fabric-installer.jar'
                "安装完成。fabric-installer.jar 已删除."
            }
            else
            {
                DeleteFileSilently  'fabric-installer.jar'
                CrashServer "未找到 fabric-server-launch.jar. 可能是 Fabric 服务器出现问题. 请几分钟后重试，并检查您的互联网连接."
            }
        }
        else
        {
            "fabric-server-launch.jar 文件已存在.继续进行..."
            $script:LauncherJarLocation = "fabric-server-launch.jar"
        }
    }
    $script:ServerRunCommand = "${script:JavaArgs} -jar ${script:LauncherJarLocation} nogui"

<#
    .SYNOPSIS

    下载并安装适用于 $ModLoaderVersion 的 Fabric 服务器.
    如果 Fabric 启动器适用于 $MinecraftVersion 和 $ModLoaderVersion,将下载并使用它;否则,将下载并使用常规的 Fabric 安装程序.
    同时还会进行检查，以确定 Fabric 是否适用于 $MinecraftVersion 和 $ModLoaderVersion.
#>
}

Function global:SetupQuilt
{
    ""
    "正在运行 Quilt 检查和设置..."
    $QuiltInstallerUrl = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/${QuiltInstallerVersion}/quilt-installer-${QuiltInstallerVersion}.jar"
    if ((ConvertFrom-JSON (Invoke-WebRequest -Uri "https://meta.fabricmc.net/v2/versions/intermediary/${MinecraftVersion}")).Length -eq 0)
    {
        CrashServer "Quilt 不支持 Minecraft ${MinecraftVersion}, Quilt ${ModLoaderVersion}."
    }
    elseif ((DownloadIfNotExists "quilt-server-launch.jar" "quilt-installer.jar" "${QuiltInstallerUrl}"))
    {
        "Quilt 安装程序已下载。正在安装..."
        RunJavaCommand "-jar quilt-installer.jar install server ${MinecraftVersion} --download-server --install-dir=."
        if ((Test-Path -Path 'quilt-server-launch.jar' -PathType Leaf))
        {
            DeleteFileSilently 'quilt-installer.jar'
            "安装完成。quilt-installer.jar 已删除."
        }
        else
        {
            DeleteFileSilently 'quilt-installer.jar'
            CrashServer "未找到 quilt-server-launch.jar. 可能是 Quilt 服务器出现问题. 请几分钟后重试，并检查您的互联网连接."
        }
    }
    $script:LauncherJarLocation = "quilt-server-launch.jar"
    $script:ServerRunCommand = "${JavaArgs} -jar ${LauncherJarLocation} nogui"

<#
    .SYNOPSIS

    下载并安装适用于 $ModLoaderVersion 的 Quilt 服务器.
    同时也会进行检查以确定 Quilt 是否适用于 $MinecraftVersion.
#>
}

Function global:SetupLegacyFabric
{
    ""
    "正在运行 LegacyFabric 检查和设置..."
    $LegacyFabricInstallerUrl = "https://maven.legacyfabric.net/net/legacyfabric/fabric-installer/${LegacyFabricInstallerVersion}/fabric-installer-${LegacyFabricInstallerVersion}.jar"
    if ((ConvertFrom-JSON (Invoke-WebRequest -Uri "https://meta.legacyfabric.net/v2/versions/loader/${MinecraftVersion}")).Length -eq 0)
    {
        CrashServer "LegacyFabric 不支持 Minecraft ${MinecraftVersion}, LegacyFabric ${ModLoaderVersion}."
    }
    elseif ((DownloadIfNotExists "fabric-server-launch.jar" "legacyfabric-installer.jar" "${LegacyFabricInstallerUrl}"))
    {
        "安装程序已下载.正在安装..."
        RunJavaCommand "-jar legacyfabric-installer.jar server -mcversion ${MinecraftVersion} -loader ${ModLoaderVersion} -downloadMinecraft"
        if ((Test-Path -Path 'fabric-server-launch.jar' -PathType Leaf))
        {
            DeleteFileSilently 'legacyfabric-installer.jar'
            "安装完成. legacyfabric-installer.jar 已删除."
        }
        else
        {
            DeleteFileSilently 'legacyfabric-installer.jar'
            CrashServer "未找到 fabric-server-launch.jar. 可能是 LegacyFabric 服务器出现问题. 请几分钟后重试，并检查您的互联网连接."
        }
    }
    $script:LauncherJarLocation = "fabric-server-launch.jar"
    $script:ServerRunCommand = "${JavaArgs} -jar ${LauncherJarLocation} nogui"

<#
    .SYNOPSIS

    下载并安装适用于 $ModLoaderVersion 的 LegacyFabric 服务器.
    同时也会进行检查以确定 LegacyFabric 是否适用于 $MinecraftVersion
#>
}

Write-Host "启动脚本由 ServerPackCreator SPC_SERVERPACKCREATOR_VERSION_SPC 生成."
Write-Host "要更改此服务器的启动设置, 如 JVM 参数/标志、Minecraft 版本、模组加载器版本等., 请编辑 variables.txt 文件."

# 确保我们在包含此脚本的目录中工作.
$BaseDir = Split-Path -parent $script:MyInvocation.MyCommand.Path
Push-Location $BaseDir

# 检查此目录的路径是否包含空格。路径中的空格容易导致问题.
if ( ${BaseDir}.Contains(" "))
{
    "警告! 此脚本的当前位置包含空格. 这可能导致服务器崩溃!"
    "强烈建议将此服务器包移动到路径中不包含空格的位置!"
    "当前路径: ${BaseDir}"
    $WhyMustPowerShellBeThisWayLikeSeriouslyWhatTheFrag = Read-Host -Prompt 'Are you sure you want to continue? (Yes/No): '
    if (${WhyMustPowerShellBeThisWayLikeSeriouslyWhatTheFrag} -eq "Yes")
    {
        "好吧. 准备好面对未知的后果吧, 弗里曼先生..."
    }
    else
    {
        CrashServer "用户不希望在路径包含空格的目录中运行服务器."
    }
}

# 不建议使用管理员权限运行服务器，因为这会给您的系统带来安全风险.
# 使用普通用户即可.
if ( (New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator))
{
    Write-Host "警告! 不建议使用管理员权限运行."
}

$ExternalVariablesFile = -join ("${BaseDir}", "\variables.txt");
if (!(Test-Path -Path $ExternalVariablesFile -PathType Leaf))
{
    CrashServer "错误! variables.txt 不存在. 没有它,服务器无法安装、配置或启动."
}

$ExternalVariables = Get-Content -raw -LiteralPath $ExternalVariablesFile | ConvertFrom-StringData
$MinecraftVersion = $ExternalVariables['MINECRAFT_VERSION']
$ModLoader = $ExternalVariables['MODLOADER']
$ModLoaderVersion = $ExternalVariables['MODLOADER_VERSION']
$LegacyFabricInstallerVersion = $ExternalVariables['LEGACYFABRIC_INSTALLER_VERSION']
$FabricInstallerVersion = $ExternalVariables['FABRIC_INSTALLER_VERSION']
$QuiltInstallerVersion = $ExternalVariables['QUILT_INSTALLER_VERSION']
$JavaArgs = $ExternalVariables['JAVA_ARGS']
$Java = $ExternalVariables['JAVA']
$WaitForUserInput = $ExternalVariables['WAIT_FOR_USER_INPUT']
$AdditionalArgs = $ExternalVariables['ADDITIONAL_ARGS']
$Restart = $ExternalVariables['RESTART']
$SkipJavaCheck = $ExternalVariables['SKIP_JAVA_CHECK']
$RecommendedJavaVersion = $ExternalVariables['RECOMMENDED_JAVA_VERSION']
$ServerStarterJarForceFetch = $ExternalVariables['SERVERSTARTERJAR_FORCE_FETCH']
$ServerStarterJarVersion = $ExternalVariables['SERVERSTARTERJAR_VERSION']
$UseSSJ = $ExternalVariables['USE_SSJ']
$LauncherJarLocation = "do_not_manually_edit"
$ServerRunCommand = "do_not_manually_edit"
$JavaVersion = "do_not_manually_edit"
$Semantics = ${MinecraftVersion}.Split(".")

if ($Java[0] -eq '"')
{
    $Java = $Java.Substring(1, $Java.Length - 1)
}
if ($Java[$Java.Length - 1] -eq '"')
{
    $Java = $Java.Substring(0, $Java.Length - 1)
}
if ($JavaArgs[0] -eq '"')
{
    $JavaArgs = $JavaArgs.Substring(1, $JavaArgs.Length - 1)
}
if ($JavaArgs[$JavaArgs.Length - 1] -eq '"')
{
    $JavaArgs = $JavaArgs.Substring(0, $JavaArgs.Length - 1)
}

# 如果需要进行 Java 检查,则将可用的 Java 版本与 Minecraft 服务器所需的版本进行比较.
# 如果未找到 Java,或可用版本不正确,则通过运行 install Java 安装所需版本.
if ("${SkipJavaCheck}" -eq "true")
{
    "跳过 Java 版本检查."
}
else
{
    if ("${Java}" -eq "java")
    {
        if (!(CommandAvailable -cmdname "${Java}"))
        {
            InstallJava
        }
        else
        {
            GetJavaVersion
            if ($script:JavaVersion -match '[0-9]+')
            {
                if ($script:JavaVersion -ne $RecommendedJavaVersion)
                {
                    InstallJava
                }
            }
            else
            {
                InstallJava
            }
        }
    }
    else
    {
        GetJavaVersion
        Write-Host "检测到 $($Semantics[0]).$($Semantics[1]).$($Semantics[2]) - Java $($JavaVersion)"
        if ($script:JavaVersion -ne $RecommendedJavaVersion)
        {
            $script:Java = "java"
            InstallJava
        }
    }
}

# 检查并警告用户是否使用了 32 位 Java 安装.实际上,这种情况应该越来越少.
# 但偶尔还是会发生.最好提醒人们注意这一点.
$Bit = CMD /C "`"${Java}`" -version 2>&1"
if (( ${Bit} | Select-String "32-Bit").Length -gt 0)
{
    Write-Host "警告!检测到 32 位 Java!强烈建议使用 64 位版本的 Java!"
}

$ReInstall = $args
$PreviousRunFile = -join ("${BaseDir}", "\.previousrun");

if ($ReInstall -eq '--cleanup')
{
    Write-Host "正在运行清理..."
    CleanServerFiles
}
elseif (Test-Path -Path $PreviousRunFile -PathType Leaf)
{
    $PreviousRunValues = Get-Content -raw -LiteralPath $PreviousRunFile | ConvertFrom-StringData
    $PreviousMinecraftVersion = $PreviousRunValues['$PREVIOUS_MINECRAFT_VERSION']
    $PreviousModLoader = $PreviousRunValues['$PREVIOUS_MODLOADER']
    $PreviousModLoaderVersion = $PreviousRunValues['$PREVIOUS_MODLOADER_VERSION']
    if (!("${PreviousMinecraftVersion}" -eq "${MinecraftVersion}") -or
        !("${PreviousModLoader}" -eq "${ModLoader}") -or
        !("${PreviousModLoaderVersion}" -eq "${ModLoaderVersion}"))
    {
        Write-Host "Minecraft 版本,模组加载器或模组加载器版本已更改。正在清理..."
        CleanServerFiles
    }
}

"PREVIOUS_MINECRAFT_VERSION=${MinecraftVersion}`n" +
"PREVIOUS_MODLOADER=${ModLoader}`n" +
"PREVIOUS_MODLOADER_VERSION=${ModLoaderVersion}" | Out-File $PreviousRunFile -encoding utf8

switch (${ModLoader})
{
    Forge
    {
        SetupForge
    }
    NeoForge
    {
        SetupNeoForge
    }
    Fabric
    {
        SetupFabric
    }
    Quilt
    {
        SetupQuilt
    }
    LegacyFabric
    {
        SetupLegacyFabric
    }
    default
    {
        CrashServer "指定的模组加载器不正确: ${ModLoader}"
    }
}

if (!(Test-Path -Path 'eula.txt' -PathType Leaf))
{
    "尚未接受 Mojang 的 EULA.要运行 Minecraft 服务器,您必须接受 Mojang 的 EULA."
    "Mojang 的 EULA 可在 https://aka.ms/MinecraftEULA 查看."
    "如果您同意 Mojang 的 EULA,请输入 'I agree'"
    $Answer = Read-Host -Prompt 'Answer'
    if (${Answer} -eq "I agree")
    {
        "用户同意了 Mojang 的 EULA."
        "#通过将以下设置更改为 TRUE 即表示您同意我们的 EULA(https://aka.ms/MinecraftEULA).`n" +
                "eula=true" | Out-File eula.txt -encoding utf8
    }
    else
    {
        CrashServer "用户未同意 Mojang 的 EULA。输入. Entered: ${Answer}. 除非您同意 Mojang 的 EULA ,否则无法运行 Minecraft 服务器."
    }
}

""
"正在启动服务器..."
"Minecraft 版本:                ${MinecraftVersion}"
"模组加载器:                    ${ModLoader}"
"模组加载器版本:                ${ModLoaderVersion}"
"LegacyFabric 安装程序版本:     ${LegacyFabricInstallerVersion}"
"Fabric 安装程序版本:           ${FabricInstallerVersion}"
"Quilt 安装程序版本:            ${QuiltInstallerVersion}"
"Java 参数:                     ${JavaArgs}"
"附加参数:                      ${AdditionalArgs}"
"Java 路径:                     ${Java}"
"等待用户输入:                  ${WaitForUserInput}"
if (!("${LauncherJarLocation}" -eq "do_not_manually_edit"))
{
    "Launcher JAR:                   ${LauncherJarLocation}"
}
"Run Command:       ${Java} ${AdditionalArgs} ${ServerRunCommand}"
"Java version:"
RunJavaCommand "-version"
""

#根据 $Restart 的设置，服务器将以循环模式运行，确保崩溃后自动重启。
#如需强制退出，可连续按下 CTRL+C。
#注意：变量在每次服务器运行之间不会重新加载。
#如需重载变量，请先退出脚本，然后重新运行.
while ($true)
{
    RunJavaCommand "${AdditionalArgs} ${ServerRunCommand}"
    if ("${SkipJavaCheck}" -eq "true")
    {
        "Java 版本检查被跳过了.服务器停止或崩溃是因为 Java 版本不匹配吗?请确保您使用的是正确的 Java 版本."
        "检测到版本为 $($Semantics[0]).$($Semantics[1]).$($Semantics[2]) 当前 Java 版本为 $($JavaVersion), 推荐版本为 $($RecommendedJavaVersion)"
    }
    if (!("${Restart}" -eq "true"))
    {
        Write-Host "正在退出..."
        if ("${WaitForUserInput}" -eq "true")
        {
            PauseScript
        }
        exit 0
    }
    "服务器将在 5 秒后自动重启.按 Ctrl + C 取消并退出."
    Start-Sleep -Seconds 5
}

""
