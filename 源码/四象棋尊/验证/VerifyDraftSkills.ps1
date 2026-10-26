$ErrorActionPreference='Stop'
$adb='D:\Android Studio\Sdk\platform-tools\adb.exe'
$path=Join-Path $PSScriptRoot 'v4.3-draft.txt'
& $adb shell am instrument -w com.dorm.fourchess.test/com.dorm.fourchess.DraftSkillProbe | Set-Content -LiteralPath $path -Encoding utf8
$log=Get-Content -LiteralPath $path -Raw
if($log -match 'FAIL|crashed' -or $log -notmatch 'DRAFT COMPLETE'){Write-Output $log;throw 'Random skill suite failed'}
Write-Output ('DRAFT PASS '+[regex]::Matches($log,'(?m)^PASS ').Count+' checks')
