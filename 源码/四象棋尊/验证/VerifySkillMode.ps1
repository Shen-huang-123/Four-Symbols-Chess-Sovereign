$ErrorActionPreference='Stop'
$adb='D:\Android Studio\Sdk\platform-tools\adb.exe'
& $adb shell settings put secure immersive_mode_confirmations confirmed
$cases=@(
@{Name='portrait';Size='1272x2800';Density=480;Orientation='portrait';Font='1.0';Network='true'},
@{Name='landscape';Size='1272x2800';Density=480;Orientation='landscape';Font='1.0';Network='false'},
@{Name='low-height';Size='720x1280';Density=480;Orientation='landscape';Font='1.0';Network='false'},
@{Name='font150';Size='1272x2800';Density=560;Orientation='portrait';Font='1.5';Network='false'}
)
foreach($case in $cases){
    & $adb shell wm size $case.Size
    & $adb shell wm density $case.Density
    & $adb shell settings put system font_scale $case.Font
    & $adb shell am force-stop com.dorm.fourchess
    $path=Join-Path $PSScriptRoot ('v4.3-skills-'+$case.Name+'.txt')
    & $adb shell am instrument -w -e orientation $case.Orientation -e network $case.Network com.dorm.fourchess.test/com.dorm.fourchess.SkillModeProbe | Set-Content -LiteralPath $path -Encoding utf8
    $log=Get-Content -LiteralPath $path -Raw
    if($log -match 'FAIL|crashed' -or $log -notmatch 'return to ordinary clears variant'){Write-Output $log;throw ('Skill flow failed: '+$case.Name)}
    Write-Output ($case.Name+' PASS '+[regex]::Matches($log,'(?m)^PASS ').Count+' checks')
    foreach($frame in @('lobby','draft','menu','targets','seal')){
        $info=[Diagnostics.ProcessStartInfo]::new($adb);$info.UseShellExecute=$false;$info.RedirectStandardOutput=$true;$info.Arguments='exec-out run-as com.dorm.fourchess cat files/skills-'+$frame+'.png'
        $process=[Diagnostics.Process]::Start($info);$out=[IO.File]::Create((Join-Path $PSScriptRoot ('v4.3-skills-'+$case.Name+'-'+$frame+'.png')))
        try{$process.StandardOutput.BaseStream.CopyTo($out)}finally{$out.Dispose()};$process.WaitForExit();if($process.ExitCode -ne 0){throw 'Screenshot export failed'}
    }
}
