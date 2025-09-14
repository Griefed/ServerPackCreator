::###############################################################################################
:: Copyright (C) 2024  Griefed
::
:: 本脚本是自由软件；您可以根据自由软件基金会发布的GNU Lesser General Public License条款
:: 重新分发和/或修改它，该许可证的版本为2.1或（由您选择）任何更高版本。
::
:: 本库的分发旨在有用，但不提供任何担保；甚至不包含适销性或特定用途适用性的隐含担保。
:: 有关更多详细信息，请参阅GNU Lesser General Public License。
::
:: 您应该已收到GNU Lesser General Public License的副本；如果没有，请写信给
:: 美国马萨诸塞州波士顿市第五街51号自由软件基金会，邮编02110-1301。
::
:: 完整许可证可在https:github.com/Griefed/ServerPackCreator/blob/main/LICENSE查看
::##############################################################################################
::
:: 由ServerPackCreator SPC_SERVERPACKCREATOR_VERSION_SPC生成的启动脚本。
:: 用于生成此脚本的模板可在以下位置找到：
::   https://github.com/Griefed/ServerPackCreator/blob/SPC_SERVERPACKCREATOR_VERSION_SPC/serverpackcreator-api/src/main/resources/de/griefed/resources/server_files/default_template.bat
::

:: 这个批处理脚本将运行start.ps1脚本，无需用户手动处理管理员相关事务。
:: 不要删除PowerShell（.ps1）文件，否则此脚本将停止工作！
@ECHO OFF

PUSHD %~dp0

SET SCRIPTDIR=%~dp0
SET PSSCRIPTPATH=%SCRIPTDIR%start.ps1
PowerShell -NoProfile -ExecutionPolicy Bypass -Command "& '%PSSCRIPTPATH%' %1";
