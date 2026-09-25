param([Parameter(Mandatory=$true)][string]$ApiKey)

# ============================================================
#  كرت فكة — إنشاء الخدمة على Render (Web Service) عبر API
#  الاستخدام: powershell -File render-deploy.ps1 -ApiKey "rnd_..."
#  ============================================================
$ErrorActionPreference = 'Stop'
$headers = @{ Authorization = "Bearer $ApiKey"; 'Content-Type' = 'application/json' }

# 1) عنوان الريبو العام على GitHub
$repo = 'https://github.com/hassan00980/kart-faka'

# 2) إنشاء الخدمة (نفس إعدادات render.yaml — Docker + مجاني + Frankfurt)
$body = @{
    type = 'web'
    name = 'kart-faka'
    repo = $repo
    branch = 'main'
    serviceDetails = @{
        env = 'docker'
        plan = 'free'
        region = 'frankfurt'
        healthCheckPath = '/'
        autoDeploy = $true
    }
} | ConvertTo-Json -Depth 6

Write-Host '> إنشاء الخدمة على Render...'
$resp = Invoke-RestMethod -Method Post -Uri 'https://api.render.com/v1/services' -Headers $headers -Body $body
$id = $resp.id
Write-Host "> تم الإنشاء — service id: $id"

# 3) بدء أول نشر مباشرة (عشان الخدمة تنهض فورًا من غير انتظار webhook)
Write-Host '> بدء النشر الأول...'
$dep = Invoke-RestMethod -Method Post -Uri "https://api.render.com/v1/services/$id/deploys" -Headers $headers -Body '{}'
Write-Host "> تم بدء النشر — deploy id: $($dep.id)"

Start-Sleep -Seconds 8
$svc = Invoke-RestMethod -Method Get -Uri "https://api.render.com/v1/services/$id" -Headers $headers
Write-Host "`n=== الخدمة جاهزة ==="
Write-Host "الاسم : $($svc.name)"
Write-Host "الحالة: $($svc.serviceDetails.envSpecificDetails ? 'docker' : 'docker')"
Write-Host "رابط  : https://$($svc.name).onrender.com"
Write-Host "`nللمتابعة: https://dashboard.render.com/web/$id"