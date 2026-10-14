# Script para testar portas comuns do sensor RFID (Windows)
# Uso: .\testar-portas-sensor.ps1 [IP_DO_SENSOR]

param([string]$SensorIp = '192.168.2.1')

Write-Host "=========================================="
Write-Host "  Testando conectividade com o sensor"
Write-Host "=========================================="
Write-Host "IP do sensor: $SensorIp"
Write-Host ""

# Testa ping primeiro
Write-Host "1. Testando ping..."
ping -n 2 -w 2000 $SensorIp > $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✓ Ping OK - Sensor está na rede"
} else {
    Write-Host "   ✗ Ping FALHOU - Sensor não está acessível na rede"
    exit 1
}

Write-Host ""
Write-Host "2. Testando portas comuns..."
Write-Host ""

# Portas comuns para sensores RFID
$ports = 8000, 6000, 8080, 80, 23, 5000, 4001

foreach ($port in $ports) {
    Write-Host -NoNewline "   Porta ${port}: "
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $aberta = $client.ConnectAsync($SensorIp, $port).Wait(2000) -and $client.Connected
    } catch {
        $aberta = $false
    } finally {
        $client.Close()
    }
    if ($aberta) {
        Write-Host "✓ ABERTA"
    } else {
        Write-Host "✗ Fechada ou inacessível"
    }
}

Write-Host ""
Write-Host "=========================================="
Write-Host "  Dicas:"
Write-Host "=========================================="
Write-Host "1. Acesse http://$SensorIp no navegador para verificar"
Write-Host "   a configuração do sensor"
Write-Host ""
Write-Host "2. Verifique o manual do sensor para confirmar a porta TCP"
Write-Host ""
Write-Host "3. Alguns sensores precisam ser configurados via interface web"
Write-Host "   antes de aceitar conexões TCP"
Write-Host ""
