$ErrorActionPreference='Stop'
$adb='D:\Android Studio\Sdk\platform-tools\adb.exe'
$cases=@(
@{Name='portrait';Size='1272x2800';Density=480;Orientation='portrait';Font='1.0';Network='true'},
@{Name='landscape';Size='1272x2800';Density=480;Orientation='landscape';Font='1.0';Network='false'},
@{Name='low-height';Size='720x1280';Density=480;Orientation='landscape';Font='1.0';Network='false'},
@{Name='tablet';Size='1600x2560';Density=320;Orientation='landscape';Font='1.0';Network='false'},
@{Name='font150';Size='1272x2800';Density=560;Orientation='portrait';Font='1.5';Network='false'}
)
foreach($case in $cases){
 & $adb shell wm size $case.Size
 & $adb shell wm density $case.Density
 & $adb shell settings put system font_scale $case.Font
 & $adb shell am force-stop com.dorm.fourchess
 $path=Join-Path $PSScriptRoot ('v5.8-career-'+$case.Name+'.txt')
 & $adb shell am instrument -w -e orientation $case.Orientation -e network $case.Network com.dorm.fourchess.test/com.dorm.fourchess.CareerProbe | Set-Content -LiteralPath $path -Encoding utf8
 $log=Get-Content -LiteralPath $path -Raw
 if($log -match 'FAIL|crashed' -or $log -notmatch 'CAREER COMPLETE'){throw $log}
 Write-Output ($case.Name+' PASS '+[regex]::Matches($log,'(?m)^PASS ').Count+' checks')
}
& $adb shell wm size reset
& $adb shell wm density reset
& $adb shell settings put system font_scale 1.0
