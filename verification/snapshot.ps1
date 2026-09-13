param([string]$OutputName='device-after')
$adbPath='C:/Users/ladak/AppData/Local/Android/Sdk/platform-tools/adb.exe'
foreach($suffix in @('','-wal')) {
 $encodedDb = & $adbPath exec-out run-as com.voltwise base64 "databases/voltwise_database$suffix" 2>$null
 if($LASTEXITCODE -eq 0) {
  [IO.File]::WriteAllBytes((Join-Path $PSScriptRoot "$OutputName.db$suffix"),[Convert]::FromBase64String(($encodedDb -join '')))
 }
}
