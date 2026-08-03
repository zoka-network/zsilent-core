param(
    [Parameter(Mandatory = $true)]
    [string]$MsiPath,
    [string]$DialogBitmap = "",
    [string]$BannerBitmap = ""
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($DialogBitmap)) {
    $DialogBitmap = Join-Path $repoRoot 'packaging\windows\zsilent-installer-dialog.bmp'
}
if ([string]::IsNullOrWhiteSpace($BannerBitmap)) {
    $BannerBitmap = Join-Path $repoRoot 'packaging\windows\zsilent-installer-banner.bmp'
}
foreach ($asset in @($DialogBitmap, $BannerBitmap)) {
    if (-not (Test-Path -LiteralPath $asset -PathType Leaf)) {
        throw "Missing MSI branding asset: $asset"
    }
}

$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.OpenDatabase($MsiPath, 1)

function Invoke-MsiSql([string]$Sql) {
    $view = $db.OpenView($Sql)
    try {
        [void]$view.Execute()
    } finally {
        [void]$view.Close()
    }
}

function Get-MsiScalar([string]$Sql, [int]$Field = 1) {
    $view = $db.OpenView($Sql)
    try {
        [void]$view.Execute()
        $record = $view.Fetch()
        if ($null -eq $record) { return $null }
        $value = $record.StringData($Field)
        if ([string]::IsNullOrWhiteSpace($value)) { return $null }
        return $value
    } finally {
        [void]$view.Close()
    }
}

function Set-MsiProperty([string]$Name, [string]$Value) {
    $existing = Get-MsiScalar "SELECT Property FROM Property WHERE Property='$Name'"
    if ($existing) {
        Invoke-MsiSql "UPDATE Property SET Value='$Value' WHERE Property='$Name'"
    } else {
        Invoke-MsiSql "INSERT INTO Property (Property, Value) VALUES ('$Name', '$Value')"
    }
}

function ConvertTo-MsiSqlString([string]$Value) {
    return $Value.Replace("'", "''")
}

function Set-MsiBinaryStream([string]$Name, [string]$Path) {
    $existing = Get-MsiScalar "SELECT Name FROM Binary WHERE Name='$Name'"
    if (-not $existing) {
        throw "MSI Binary table does not contain $Name."
    }
    $view = $db.OpenView("UPDATE Binary SET Data=? WHERE Name='$Name'")
    $record = $installer.CreateRecord(1)
    try {
        [void]$record.SetStream(1, (Resolve-Path -LiteralPath $Path).Path)
        [void]$view.Execute($record)
    } finally {
        [void]$view.Close()
    }
}

function Set-MsiTextStyle(
    [string]$Name,
    [string]$Face,
    [int]$Size,
    [int]$Color,
    [int]$StyleBits
) {
    $existing = Get-MsiScalar "SELECT TextStyle FROM TextStyle WHERE TextStyle='$Name'"
    if ($existing) {
        Invoke-MsiSql "UPDATE TextStyle SET FaceName='$Face', Size=$Size, Color=$Color, StyleBits=$StyleBits WHERE TextStyle='$Name'"
    } else {
        Invoke-MsiSql "INSERT INTO TextStyle (TextStyle, FaceName, Size, Color, StyleBits) VALUES ('$Name', '$Face', $Size, $Color, $StyleBits)"
    }
}

function Set-MsiControlText([string]$Dialog, [string]$Control, [string]$Text) {
    $safeText = ConvertTo-MsiSqlString $Text
    Invoke-MsiSql "UPDATE Control SET Text='$safeText' WHERE Dialog_='$Dialog' AND Control='$Control'"
}

function Set-MsiControlTransparent([string]$Dialog, [string]$Control, [int]$Attributes) {
    $transparent = $Attributes -bor 65536
    Invoke-MsiSql "UPDATE Control SET Attributes=$transparent WHERE Dialog_='$Dialog' AND Control='$Control'"
}

function Style-MsiTextControls(
    [string]$Dialog,
    [string]$NormalStyle,
    [string]$TitleStyle
) {
    $rows = New-Object System.Collections.Generic.List[object]
    $view = $db.OpenView("SELECT Control, Attributes, Text FROM Control WHERE Dialog_='$Dialog' AND Type='Text'")
    try {
        [void]$view.Execute()
        while ($record = $view.Fetch()) {
            $rows.Add([pscustomobject]@{
                Control = $record.StringData(1)
                Attributes = [int]$record.IntegerData(2)
                Text = $record.StringData(3)
            })
        }
    } finally {
        [void]$view.Close()
    }
    foreach ($row in $rows) {
        $text = $row.Text
        if ($text.StartsWith('{\WixUI_Font_Bigger}') -or $text.StartsWith('{\WixUI_Font_Title}')) {
            $close = $text.IndexOf('}')
            $text = "{\$TitleStyle}" + $text.Substring($close + 1)
        } elseif (-not $text.StartsWith('{\')) {
            $text = "{\$NormalStyle}" + $text
        }
        Set-MsiControlText $Dialog $row.Control $text
        Set-MsiControlTransparent $Dialog $row.Control $row.Attributes
    }
}

function Style-MsiBannerHeader([string]$Dialog) {
    foreach ($control in @('Title', 'Description')) {
        $text = Get-MsiScalar "SELECT Text FROM Control WHERE Dialog_='$Dialog' AND Control='$control'"
        if ($null -eq $text) { continue }
        if ($control -eq 'Title') {
            if ($text.StartsWith('{\')) {
                $close = $text.IndexOf('}')
                $text = '{\ZSilent_Banner_Title}' + $text.Substring($close + 1)
            } else {
                $text = '{\ZSilent_Banner_Title}' + $text
            }
        } else {
            $text = '{\ZSilent_Banner_Normal}' + $text
        }
        $attributes = [int](Get-MsiScalar "SELECT Attributes FROM Control WHERE Dialog_='$Dialog' AND Control='$control'")
        Set-MsiControlText $Dialog $control $text
        Set-MsiControlTransparent $Dialog $control $attributes
    }
}

$launcherFileId = $null
$view = $db.OpenView('SELECT File, FileName FROM File')
try {
    [void]$view.Execute()
    while ($true) {
        $record = $view.Fetch()
        if ($null -eq $record) { break }
        $fileName = $record.StringData(2)
        if ($fileName -like '*ZSilent Core.exe') {
            $launcherFileId = $record.StringData(1)
            break
        }
    }
} finally {
    [void]$view.Close()
}

if (-not $launcherFileId) {
    throw 'Could not find ZSilent Core.exe in MSI File table.'
}

$wixCa = Get-MsiScalar "SELECT Name FROM Binary WHERE Name='WixCA'"
if (-not $wixCa) {
    throw 'MSI Binary table does not contain WixCA; cannot add launch-after-install action.'
}

$launchAction = Get-MsiScalar "SELECT Action FROM CustomAction WHERE Action='LaunchApplication'"
if (-not $launchAction) {
    Invoke-MsiSql "INSERT INTO CustomAction (Action, Type, Source, Target) VALUES ('LaunchApplication', 1, 'WixCA', 'WixShellExec')"
}

Set-MsiProperty 'WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT' 'Launch ZSilent Core'
Set-MsiProperty 'WixShellExecTarget' "[#$launcherFileId]"

$event = Get-MsiScalar "SELECT Dialog_ FROM ControlEvent WHERE Dialog_='ExitDialog' AND Control_='Finish' AND Event='DoAction' AND Argument='LaunchApplication'"
if (-not $event) {
    Invoke-MsiSql "INSERT INTO ControlEvent (Dialog_, Control_, Event, Argument, Condition, Ordering) VALUES ('ExitDialog', 'Finish', 'DoAction', 'LaunchApplication', 'WIXUI_EXITDIALOGOPTIONALCHECKBOX = 1 and NOT Installed', 1)"
}

# Replace only the standard WiX visual streams. Product/component/upgrade
# identity stays untouched, so upgrades and the official MSI uninstaller keep
# using the same Windows Installer registration.
Set-MsiBinaryStream 'WixUI_Bmp_Dialog' $DialogBitmap
Set-MsiBinaryStream 'WixUI_Bmp_Banner' $BannerBitmap

# Full-image pages can safely use white type on the black dialog bitmap.
Set-MsiTextStyle 'ZSilent_Dark_Normal' 'Segoe UI' 9 16777215 0
Set-MsiTextStyle 'ZSilent_Dark_Title' 'Segoe UI' 14 16777215 1
Set-MsiTextStyle 'ZSilent_Banner_Normal' 'Segoe UI' 8 16777215 0
Set-MsiTextStyle 'ZSilent_Banner_Title' 'Segoe UI' 10 16777215 1

$darkDialogs = @(
    'FatalError',
    'UserExit',
    'ExitDialog',
    'PrepareDlg',
    'ResumeDlg',
    'WelcomeDlg',
    'MaintenanceWelcomeDlg'
)
foreach ($dialog in $darkDialogs) {
    Style-MsiTextControls $dialog 'ZSilent_Dark_Normal' 'ZSilent_Dark_Title'
}

$bannerDialogs = @(
    'BrowseDlg',
    'DiskCostDlg',
    'FilesInUse',
    'MsiRMFilesInUse',
    'ProgressDlg',
    'OutOfRbDiskDlg',
    'OutOfDiskDlg',
    'LicenseAgreementDlg',
    'VerifyReadyDlg',
    'InstallDirDlg',
    'MaintenanceTypeDlg'
)
foreach ($dialog in $bannerDialogs) {
    Style-MsiBannerHeader $dialog
}

Set-MsiControlText 'WelcomeDlg' 'Title' '{\ZSilent_Dark_Title}Install [ProductName]'
Set-MsiControlText 'WelcomeDlg' 'Description' '{\ZSilent_Dark_Normal}Private desktop control for the Zoka network. Choose Next to continue.'
Set-MsiControlText 'ExitDialog' 'Title' '{\ZSilent_Dark_Title}[ProductName] is ready'
Set-MsiControlText 'ExitDialog' 'Description' '{\ZSilent_Dark_Normal}Installation completed successfully.'
Set-MsiProperty 'WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT' '{\ZSilent_Dark_Normal}Launch ZSilent Core'
$exitCheckAttributes = [int](Get-MsiScalar "SELECT Attributes FROM Control WHERE Dialog_='ExitDialog' AND Control='OptionalCheckBox'")
Set-MsiControlTransparent 'ExitDialog' 'OptionalCheckBox' $exitCheckAttributes

# Compose/jpackage ships a placeholder EULA. A minimal installer must not ask
# users to accept placeholder legal text, so the normal first-install flow is
# Welcome -> Destination -> Ready. Repair/update/uninstall flows are unchanged.
Invoke-MsiSql "DELETE FROM ControlEvent WHERE Dialog_='WelcomeDlg' AND Control_='Next' AND Event='NewDialog' AND Argument='LicenseAgreementDlg'"
Invoke-MsiSql "DELETE FROM ControlEvent WHERE Dialog_='InstallDirDlg' AND Control_='Back' AND Event='NewDialog' AND Argument='LicenseAgreementDlg'"

# Compose 1.7.3 defaults an unset menu group to the literal folder 'Unknown'.
# Keep a deterministic Start-menu group even if a future jpackage invocation
# ignores the Gradle menuGroup setting.
$startMenuDirectory = Get-MsiScalar "SELECT Directory FROM Directory WHERE Directory_Parent='ProgramMenuFolder'"
if ($startMenuDirectory) {
    Invoke-MsiSql "UPDATE Directory SET DefaultDir='ZSilent Core' WHERE Directory='$startMenuDirectory'"
}

[void]$db.Commit()
Write-Host "Patched ZSilent branding, Start-menu metadata and launch-after-install into $MsiPath"
