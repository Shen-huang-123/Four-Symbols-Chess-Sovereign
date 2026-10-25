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
    $path=Join-Path $PSScriptRoot ('v3.2-players-'+$case.Name+'.txt')
    & $adb shell am instrument -w -e orientation $case.Orientation -e network $case.Network com.dorm.fourchess.test/com.dorm.fourchess.PlayerFlowProbe | Set-Content -LiteralPath $path -Encoding utf8
    $log=Get-Content -LiteralPath $path -Raw
    if($log -match 'FAIL|crashed' -or $log -notmatch 'port is reusable after leaving room'){Write-Output $log;throw ('Player flow failed: '+$case.Name)}
    Write-Output ($case.Name+' PASS '+[regex]::Matches($log,'(?m)^PASS ').Count+' checks')
    Write-Output (($log -split "`n"|Where-Object {$_ -match '^SIZE '}) -join "`n")
    $frames=@('name-form','local-match','exit-confirm','final-lobby');if($case.Network -eq 'true'){$frames+=@('host-roster','client-match')}
    foreach($frame in $frames){
        $info=[Diagnostics.ProcessStartInfo]::new($adb);$info.UseShellExecute=$false;$info.RedirectStandardOutput=$true;$info.Arguments='exec-out run-as com.dorm.fourchess cat files/players-'+$frame+'.png'
        $process=[Diagnostics.Process]::Start($info);$out=[IO.File]::Create((Join-Path $PSScriptRoot ('v3.2-'+$case.Name+'-'+$frame+'.png')))
        try{$process.StandardOutput.BaseStream.CopyTo($out)}finally{$out.Dispose()};$process.WaitForExit();if($process.ExitCode -ne 0){throw 'Screenshot export failed'}
    }
}