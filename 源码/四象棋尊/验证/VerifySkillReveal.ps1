$ErrorActionPreference='Stop'
$adb='D:\Android Studio\Sdk\platform-tools\adb.exe'
& $adb shell settings put secure immersive_mode_confirmations confirmed
$cases=@(
@{Name='portrait';Size='1272x2800';Density=480;Orientation='portrait';Font='1.0';Motion='1'},
@{Name='landscape';Size='1272x2800';Density=480;Orientation='landscape';Font='1.0';Motion='1'},
@{Name='low-height';Size='720x1280';Density=480;Orientation='landscape';Font='1.0';Motion='1'},
@{Name='font150';Size='1272x2800';Density=560;Orientation='portrait';Font='1.5';Motion='1'},
@{Name='tablet';Size='1600x2560';Density=320;Orientation='landscape';Font='1.0';Motion='1'},
@{Name='reduced-motion';Size='1272x2800';Density=480;Orientation='portrait';Font='1.0';Motion='0'}
)
try{foreach($case in $cases){
 & $adb shell wm size $case.Size
 $actual=(& $adb shell wm size) -join " "
 if(([regex]::Matches($actual,"(?:Physical|Override) size: (\d+x\d+)"))[-1].Groups[1].Value -ne $case.Size){throw ("Emulator size was clamped: "+$actual)}
 & $adb shell wm density $case.Density
 & $adb shell settings put system font_scale $case.Font
 & $adb shell settings put global animator_duration_scale $case.Motion
 & $adb shell am force-stop com.dorm.fourchess
 $path=Join-Path $PSScriptRoot ('v4.6-reveal-'+$case.Name+'.txt')
 & $adb shell am instrument -w -e orientation $case.Orientation com.dorm.fourchess.test/com.dorm.fourchess.SkillRevealProbe | Set-Content -LiteralPath $path -Encoding utf8
 $log=Get-Content -LiteralPath $path -Raw
 if($log -match 'FAIL|crashed' -or $log -notmatch 'REVEAL COMPLETE'){Write-Output $log;throw ('Reveal flow failed: '+$case.Name)}
 Write-Output ($case.Name+' PASS '+[regex]::Matches($log,'(?m)^PASS ').Count+' checks')
 foreach($frame in @('backs','results','rotated','live','safe-top')){
  $info=[Diagnostics.ProcessStartInfo]::new($adb);$info.UseShellExecute=$false;$info.RedirectStandardOutput=$true;$info.Arguments='exec-out run-as com.dorm.fourchess cat files/reveal-'+$frame+'.png'
  $process=[Diagnostics.Process]::Start($info);$out=[IO.File]::Create((Join-Path $PSScriptRoot ('v4.6-reveal-'+$case.Name+'-'+$frame+'.png')))
  try{$process.StandardOutput.BaseStream.CopyTo($out)}finally{$out.Dispose()};$process.WaitForExit();if($process.ExitCode -ne 0){throw 'Screenshot export failed'}
 }
}}finally{& $adb shell settings put global animator_duration_scale 1}
