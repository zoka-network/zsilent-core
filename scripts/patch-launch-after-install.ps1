param(
    [Parameter(Mandatory = $true)]
    [string]$MsiPath
)

$ErrorActionPreference = 'Stop'
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

[void]$db.Commit()
Write-Host "Patched launch-after-install option into $MsiPath"
