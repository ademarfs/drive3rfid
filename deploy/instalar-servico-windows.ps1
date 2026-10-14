# Instala a API RFID SensorSerialYan como serviço do Windows via NSSM
# (equivalente ao deploy/sensor-rfid.service do Linux).
# Uso (PowerShell como administrador): .\deploy\instalar-servico-windows.ps1

$servico = 'sensor-rfid'
$raiz = (Resolve-Path "$PSScriptRoot\..").Path

$admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $admin) {
    Write-Host "ERRO: execute este script em um PowerShell aberto como administrador."
    exit 1
}
if (-not (Get-Command nssm -ErrorAction SilentlyContinue)) {
    Write-Host "ERRO: nssm não encontrado no PATH. Instale com 'winget install NSSM.NSSM' ou baixe de https://nssm.cc"
    exit 1
}
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "ERRO: java não encontrado no PATH. Instale o JDK 17."
    exit 1
}
if (Get-Service $servico -ErrorAction SilentlyContinue) {
    Write-Host "ERRO: o serviço '$servico' já existe. Para removê-lo: nssm stop $servico; nssm remove $servico confirm"
    exit 1
}

# Mesmas opções da JVM do executar-servidor.sh / executar-servidor.ps1
$javaOpts = '-Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m'
$jar = Join-Path $raiz 'target\SensorSerialYan-1.0-SNAPSHOT.jar'
$log = Join-Path $raiz 'logs\servico.log'
New-Item -ItemType Directory -Force (Join-Path $raiz 'logs') | Out-Null

# Aspas escapadas com \" para o caso de o caminho do jar ter espaços (modo legado = PowerShell 5.1)
$PSNativeCommandArgumentPassing = 'Legacy'
nssm install $servico $java.Source "$javaOpts -jar \`"$jar\`""
nssm set $servico AppDirectory $raiz
nssm set $servico AppStdin NUL
nssm set $servico AppStdout $log
nssm set $servico AppStderr $log
nssm set $servico AppRotateFiles 1
nssm set $servico AppRotateBytes 10485760
# Reinicia em caso de falha após 10 s
nssm set $servico AppExit Default Restart
nssm set $servico AppRestartDelay 10000
nssm set $servico Start SERVICE_AUTO_START

Write-Host "Serviço '$servico' instalado. Inicie com: nssm start $servico"
