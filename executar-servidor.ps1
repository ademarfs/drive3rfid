# Script para executar o servidor RFID (Windows)
# Uso: .\executar-servidor.ps1

Set-Location $PSScriptRoot

Write-Host "=========================================="
Write-Host "  Iniciando Servidor RFID Sensor"
Write-Host "=========================================="
Write-Host ""
Write-Host "O servidor será iniciado na porta 8080 (padrão Spring Boot)"
Write-Host "Para parar o servidor, pressione Ctrl+C"
Write-Host ""
Write-Host "Endpoints disponíveis:"
Write-Host "  - GET http://localhost:8080/sensor/tags/{ip}/{porta}"
Write-Host "  - GET http://localhost:8080/sensor/tags/{ip}/{porta}/{tagId}"
Write-Host "  - GET http://localhost:8080/sensor/testar-todos"
Write-Host ""
Write-Host "=========================================="
Write-Host ""

# Opções da JVM para servidores com poucos recursos (sobrescreva via variável de ambiente JAVA_OPTS)
$javaOpts = $env:JAVA_OPTS
if (-not $javaOpts) {
    $javaOpts = '-Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m'
}

& java ($javaOpts -split '\s+' | Where-Object { $_ }) -jar target\SensorSerialYan-1.0-SNAPSHOT.jar
exit $LASTEXITCODE
