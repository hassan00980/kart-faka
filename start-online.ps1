# ============================================================
#  كرت فكة — اللعب أونلاين من أي مكان في العالم (مجانًا 100%)
#  الطريقة: Cloudflare Tunnel — من غير كارت، من غير حساب
#  التشغيل:  powershell -File start-online.ps1
#  ============================================================
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# هل السيرفر شغال بالفعل على 3000؟ (مش هنبدأ واحد تاني — هنستخدم اللي شغال)
$server = $null
$alreadyRunning = $false
try {
  $resp = Invoke-WebRequest -Uri 'http://localhost:3000/' -TimeoutSec 3 -UseBasicParsing
  if ($resp.StatusCode -eq 200) { $alreadyRunning = $true }
} catch { $alreadyRunning = $false }

if ($alreadyRunning) {
  Write-Host '> سيرفر اللعبة شغال بالفعل — هكمل عليه.' -ForegroundColor DarkGray
} else {
  Write-Host '> تشغيل سيرفر اللعبة...' -ForegroundColor Cyan
  $server = Start-Process node -ArgumentList 'server.js' -WorkingDirectory $root -PassThru -WindowStyle Hidden
  Start-Sleep -Seconds 2
}

$cf = "$env:TEMP\opencode\cf-tunnel\cloudflared.exe"
if (-not (Test-Path $cf)) { throw 'cloudflared مش موجود! نزّله: https://github.com/cloudflare/cloudflared/releases' }

Write-Host '> فتح النفق على الإنترنت...' -ForegroundColor Cyan
$log = "$env:TEMP\opencode\tunnel.log"
$err = "$env:TEMP\opencode\tunnel-err.log"
Remove-Item $log, $err -ErrorAction SilentlyContinue
$tunnel = Start-Process $cf -ArgumentList 'tunnel','--url','http://localhost:3000','--no-autoupdate' `
  -RedirectStandardOutput $log -RedirectStandardError $err -PassThru -WindowStyle Hidden

# استخراج الرابط من اللوج (ياخد لحد 60 ثانية)
$url = $null
for ($i = 0; $i -lt 60 -and -not $url; $i++) {
  Start-Sleep -Seconds 1
  $txt = ''
  if (Test-Path $log) { $txt += Get-Content $log -Raw -ErrorAction SilentlyContinue }
  if (Test-Path $err) { $txt += Get-Content $err -Raw -ErrorAction SilentlyContinue }
  $m = [regex]::Match($txt, 'https://[a-z0-9-]+\.trycloudflare\.com')
  if ($m.Success) { $url = $m.Value }
}
if (-not $url) { throw 'ما قدرتش أستخرج الرابط — افتح اللوج واعمل إنشاء يدوي للرابط' }

Write-Host ''
Write-Host '======================' -ForegroundColor Green
Write-Host ' اللعبة شغالة أونلاين!' -ForegroundColor Green
Write-Host '======================' -ForegroundColor Green
Write-Host 'شارك الرابط ده مع أصحابك — أي مكان في العالم:' -ForegroundColor White
Write-Host "  $url" -ForegroundColor Yellow
Write-Host ''
Write-Host 'المضيف بيفتح نفس الرابط ويعمل أوضة، والباقيين يفتحوه ويكتبوا الكود.'
Write-Host 'صفحة المضيف لازم تفضل مفتوحة طول الجلسة.' -ForegroundColor DarkGray
Set-Clipboard -Value $url -ErrorAction SilentlyContinue
Write-Host 'اتنسخ الرابط في الحافظة تلقائيًا — الصقه في الواتساب/الماسنجر وابعته 📋'
Write-Host ''
Write-Host 'اضغط Enter لإيقاف الجلسة.'
try { Read-Host -Prompt '' } catch { }
Stop-Process -Id $tunnel.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host 'اتقفلت الجلسة. باي 👋'