#!/bin/bash

# Script para executar o servidor RFID
# Uso: ./executar-servidor.sh

cd "$(dirname "$0")"

echo "=========================================="
echo "  Iniciando Servidor RFID Sensor"
echo "=========================================="
echo ""
echo "O servidor será iniciado na porta 8080 (padrão Spring Boot)"
echo "Para parar o servidor, pressione Ctrl+C"
echo ""
echo "Endpoints disponíveis:"
echo "  - GET http://localhost:8080/sensor/tags/{ip}/{porta}"
echo "  - GET http://localhost:8080/sensor/tags/{ip}/{porta}/{tagId}"
echo "  - GET http://localhost:8080/sensor/testar-todos"
echo ""
echo "=========================================="
echo ""

# Opções da JVM para servidores com poucos recursos (sobrescreva via variável de ambiente JAVA_OPTS)
JAVA_OPTS="${JAVA_OPTS:--Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m}"

java $JAVA_OPTS -jar target/SensorSerialYan-1.0-SNAPSHOT.jar
