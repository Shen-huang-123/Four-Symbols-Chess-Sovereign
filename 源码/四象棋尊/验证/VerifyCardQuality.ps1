$ErrorActionPreference='Stop'
$adb='D:\Android Studio\Sdk\platform-tools\adb.exe'
$path=Join-Path $PSScriptRoot 'v5.7-quality.txt'
& $adb shell am instrument -w com.dorm.fourchess.test/com.dorm.fourchess.CardQualityProbe | Set-Content -LiteralPath $path -Encoding utf8
$log=Get-Content -LiteralPath $path -Raw
if($log -match 'FAIL|crashed' -or $log -notmatch 'QUALITY COMPLETE'){Write-Output $log;throw 'Card quality suite failed'}
Write-Output ('QUALITY PASS '+[regex]::Matches($log,'(?m)^PASS ').Count+' checks')
foreach($frame in (@('full','compact','reveal','seals','effects','phases') + @(0..63 | ForEach-Object { 'prestige-'+$_ }))){
 $info=[Diagnostics.ProcessStartInfo]::new($adb);$info.UseShellExecute=$false;$info.RedirectStandardOutput=$true;$info.Arguments='exec-out run-as com.dorm.fourchess cat files/quality-'+$frame+'.png'
 $process=[Diagnostics.Process]::Start($info);$out=[IO.File]::Create((Join-Path $PSScriptRoot ('v5.7-quality-'+$frame+'.png')))
 try{$process.StandardOutput.BaseStream.CopyTo($out)}finally{$out.Dispose()};$process.WaitForExit();if($process.ExitCode -ne 0){throw 'Quality preview export failed'}
}
