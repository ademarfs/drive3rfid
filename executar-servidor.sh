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
echo "  - GET http://localhost:8080/sensor"
echo "  - GET http://localhost:8080/sensor/{ip}/{porta}/{tagId}"
echo ""
echo "=========================================="
echo ""

java -jar target/SensorSerialYan-1.0-SNAPSHOT.jar
