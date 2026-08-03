param(
    [Parameter(Mandatory = $true)]
    [string]$MsiPath,
    [Parameter(Mandatory = $true)]
    [string]$ExpectedVersion,
    [string]$ExpectedUpgradeCode = '{63E8B74E-A8E8-49F0-9E52-9DF7B48C6F95}'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $MsiPath -PathType Leaf)) {
    throw "MSI not found: $MsiPath"
}

$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.OpenDatabase((Resolve-Path -LiteralPath $MsiPath).Path, 0)

function Get-MsiScalar([string]$Sql, [int]$Field = 1) {
    $view = $db.OpenView($Sql)
    try {
        [void]$view.Execute()
        $record = $view.Fetch()
        if ($null -eq $record) { return $null }
        return $record.StringData($Field)
    } finally {
        [void]$view.Close()
    }
}

function Get-MsiStreamSize([string]$Table, [string]$KeyColumn, [string]$Key, [string]$StreamColumn) {
    $view = $db.OpenView("SELECT $StreamColumn FROM $Table WHERE $KeyColumn='$Key'")
    try {
        [void]$view.Execute()
        $record = $view.Fetch()
        if ($null -eq $record) { return 0 }
        return [int]$record.DataSize(1)
    } finally {
        [void]$view.Close()
    }
}

function Assert-Msi([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "MSI validation failed: $Message" }
}

$productName = Get-MsiScalar "SELECT Value FROM Property WHERE Property='ProductName'"
$productVersion = Get-MsiScalar "SELECT Value FROM Property WHERE Property='ProductVersion'"
$productCode = Get-MsiScalar "SELECT Value FROM Property WHERE Property='ProductCode'"
$upgradeCode = Get-MsiScalar "SELECT Value FROM Property WHERE Property='UpgradeCode'"
Assert-Msi ($productName -eq 'ZSilent Core') "unexpected ProductName '$productName'"
Assert-Msi ($productVersion -eq $ExpectedVersion) "expected version $ExpectedVersion, found '$productVersion'"
Assert-Msi (-not [string]::IsNullOrWhiteSpace($productCode)) 'ProductCode is missing'
Assert-Msi ($upgradeCode -eq $ExpectedUpgradeCode) "UpgradeCode changed to '$upgradeCode'"

# A normal MSI with ProductCode, RemoveExistingProducts and no ARPNOREMOVE is
# registered by Windows Installer in Apps & Features with an official
# MsiExec.exe /X{ProductCode} uninstall command.
$removeExisting = Get-MsiScalar "SELECT Action FROM InstallExecuteSequence WHERE Action='RemoveExistingProducts'"
$arpNoRemove = Get-MsiScalar "SELECT Value FROM Property WHERE Property='ARPNOREMOVE'"
Assert-Msi ($removeExisting -eq 'RemoveExistingProducts') 'upgrade/uninstall sequence is missing RemoveExistingProducts'
Assert-Msi ($arpNoRemove -ne '1') 'ARPNOREMOVE disables the official uninstaller'

$startDirectory = Get-MsiScalar "SELECT Directory FROM Directory WHERE Directory_Parent='ProgramMenuFolder'"
$startDirectoryName = if ($startDirectory) {
    Get-MsiScalar "SELECT DefaultDir FROM Directory WHERE Directory='$startDirectory'"
} else { $null }
$startShortcut = if ($startDirectory) {
    Get-MsiScalar "SELECT Shortcut FROM Shortcut WHERE Directory_='$startDirectory'"
} else { $null }
Assert-Msi ($startDirectoryName -eq 'ZSilent Core') "Start-menu group is '$startDirectoryName'"
Assert-Msi (-not [string]::IsNullOrWhiteSpace($startShortcut)) 'Start-menu shortcut is missing'

$arpIcon = Get-MsiScalar "SELECT Value FROM Property WHERE Property='ARPPRODUCTICON'"
$arpIconSize = if ($arpIcon) { Get-MsiStreamSize 'Icon' 'Name' $arpIcon 'Data' } else { 0 }
Assert-Msi ($arpIconSize -gt 1024) 'Apps & Features icon is missing or empty'

$dialogBitmapSize = Get-MsiStreamSize 'Binary' 'Name' 'WixUI_Bmp_Dialog' 'Data'
$bannerBitmapSize = Get-MsiStreamSize 'Binary' 'Name' 'WixUI_Bmp_Banner' 'Data'
Assert-Msi ($dialogBitmapSize -gt 100000) 'dark dialog bitmap was not embedded'
Assert-Msi ($bannerBitmapSize -gt 10000) 'dark banner bitmap was not embedded'
Assert-Msi ((Get-MsiScalar "SELECT TextStyle FROM TextStyle WHERE TextStyle='ZSilent_Dark_Title'") -eq 'ZSilent_Dark_Title') 'dark MSI typography is missing'

$launchAction = Get-MsiScalar "SELECT Action FROM CustomAction WHERE Action='LaunchApplication'"
$launchEvent = Get-MsiScalar "SELECT Dialog_ FROM ControlEvent WHERE Dialog_='ExitDialog' AND Argument='LaunchApplication'"
Assert-Msi ($launchAction -eq 'LaunchApplication') 'launch-after-install custom action is missing'
Assert-Msi ($launchEvent -eq 'ExitDialog') 'launch-after-install Finish event is missing'

$placeholderLicensePath = Get-MsiScalar "SELECT Dialog_ FROM ControlEvent WHERE Dialog_='WelcomeDlg' AND Argument='LicenseAgreementDlg'"
Assert-Msi ([string]::IsNullOrWhiteSpace($placeholderLicensePath)) 'placeholder EULA remains in the first-install flow'

Write-Host "Validated MSI $productName $productVersion"
Write-Host "  ProductCode: $productCode"
Write-Host "  UpgradeCode: $upgradeCode"
Write-Host "  Start menu: $startDirectoryName"
Write-Host "  Dialog/banner streams: $dialogBitmapSize / $bannerBitmapSize bytes"
Write-Host "  Official uninstall: enabled"
