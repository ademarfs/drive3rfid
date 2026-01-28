#!/bin/bash

# Script para testar portas comuns do sensor RFID
# Uso: ./testar-portas-sensor.sh [IP_DO_SENSOR]

SENSOR_IP=${1:-192.168.2.1}

echo "=========================================="
echo "  Testando conectividade com o sensor"
echo "=========================================="
echo "IP do sensor: $SENSOR_IP"
echo ""

# Testa ping primeiro
echo "1. Testando ping..."
if ping -c 2 -W 2 $SENSOR_IP > /dev/null 2>&1; then
    echo "   ✓ Ping OK - Sensor está na rede"
else
    echo "   ✗ Ping FALHOU - Sensor não está acessível na rede"
    exit 1
fi

echo ""
echo "2. Testando portas comuns..."
echo ""

# Portas comuns para sensores RFID
PORTS=(8000 6000 8080 80 23 5000 4001)

for port in "${PORTS[@]}"; do
    echo -n "   Porta $port: "
    if timeout 2 bash -c "cat < /dev/null > /dev/tcp/$SENSOR_IP/$port" 2>/dev/null; then
        echo "✓ ABERTA"
    else
        echo "✗ Fechada ou inacessível"
    fi
done

echo ""
echo "=========================================="
echo "  Dicas:"
echo "=========================================="
echo "1. Acesse http://$SENSOR_IP no navegador para verificar"
echo "   a configuração do sensor"
echo ""
echo "2. Verifique o manual do sensor para confirmar a porta TCP"
echo ""
echo "3. Alguns sensores precisam ser configurados via interface web"
echo "   antes de aceitar conexões TCP"
echo ""
