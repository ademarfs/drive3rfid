# Instruções para Executar e Testar o Sensor RFID

## 📦 Executar o Servidor

### Opção 1: Usando o script (recomendado)
```bash
cd /home/user/Downloads/rfid/SensorSerialYan
./executar-servidor.sh
```

### Opção 2: Executar diretamente com Java
```bash
cd /home/user/Downloads/rfid/SensorSerialYan
java -jar target/SensorSerialYan-1.0-SNAPSHOT.jar
```

O servidor será iniciado na porta **8080** (padrão do Spring Boot).

---

## 🧪 Como Testar o Sensor

### 1. Testar via CLI (Interface de Linha de Comando)

Quando o servidor iniciar, você verá um menu interativo no terminal:

```
CLI DO SENSOR ATIVO
Lista de comando 
1 - Obter informações do sensor 
2 - Testar Leitura de Tags 
3 - Configurar Wifi do Sensor 
```

**Opção 1 - Obter informações do sensor:**
- Digite `1` e pressione Enter
- Informe o IP do sensor (exemplo: `192.168.2.1`)
- Informe a porta do sensor (exemplo: `8000`)
- O sistema testará a conexão com o sensor

**Opção 2 - Testar Leitura de Tags:**
- Digite `2` e pressione Enter
- Informe o IP do sensor (exemplo: `192.168.2.1`)
- Informe a porta do sensor (exemplo: `8000`)
- O sistema listará todas as tags RFID detectadas

---

### 2. Testar via API REST (de outro terminal)

Com o servidor rodando, você pode testar de outro terminal usando `curl` ou qualquer cliente HTTP.

#### 🔧 Gerenciar Sensores

**Cadastrar um sensor:**
```bash
curl -X POST http://localhost:8080/sensor/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"ip":"192.168.2.1","porta":8000,"nome":"Sensor Principal"}'
```

**Listar sensores cadastrados:**
```bash
curl http://localhost:8080/sensor/listar
```

**Remover um sensor:**
```bash
curl -X DELETE http://localhost:8080/sensor/192.168.2.1/8000
```

#### 📋 Ler Tags

**Obter todas as tags de um sensor:**
```bash
# Usando query parameters
curl "http://localhost:8080/sensor/tags?ip=192.168.2.1&porta=8000"

# Ou usando path variables
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
```

**Buscar uma tag específica:**
```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890
```

**Parâmetros:**
- `192.168.2.1` - IP do sensor RFID
- `8000` - Porta do sensor RFID
- `E20012345678901234567890` - ID da tag RFID que você quer buscar

> 📖 **Documentação completa:** Veja o arquivo `ENDPOINTS_API.md` para todos os endpoints disponíveis.

#### 🌐 Vários adaptadores de rede (dois sensores em redes diferentes)

Se o PC tem **dois adaptadores** (cada um na rede de um sensor), use **ipLocal** = IP do seu PC na interface que “enxerga” aquele sensor.

**Exemplo:** Sensor 1 em 192.168.2.1:8000 (seu PC nessa rede: 192.168.2.2); Sensor 2 em 192.168.2.5:9000 (seu PC nessa rede: 192.168.2.10).

Cadastrar com ipLocal:
```bash
curl -X POST http://localhost:8080/sensor/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"ip":"192.168.2.1","porta":8000,"nome":"Sensor 1","ipLocal":"192.168.2.2"}'

curl -X POST http://localhost:8080/sensor/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"ip":"192.168.2.5","porta":9000,"nome":"Sensor 2","ipLocal":"192.168.2.10"}'
```

Consultar o sensor 192.168.2.5:9000 forçando a interface:
```bash
curl "http://localhost:8080/sensor/tags/192.168.2.5/9000?ipLocal=192.168.2.10"
```

---

### 3. Testar de outro computador na rede

Se você quiser testar de outro computador na mesma rede:

1. Descubra o IP da máquina onde o servidor está rodando:
```bash
hostname -I
# ou
ip addr show
```

2. No outro computador, use o IP encontrado:
```bash
curl http://[IP_DO_SERVIDOR]:8080/sensor
```

**Nota:** Certifique-se de que o firewall permite conexões na porta 8080.

---

## 🔧 Configuração do Sensor RFID

Antes de testar, certifique-se de que:

1. **O sensor RFID está ligado e configurado na rede WiFi**
2. **Você conhece o IP e porta do sensor** (padrão parece ser `192.168.2.1:8000`)
3. **O sensor está na mesma rede que o servidor** (ou acessível via rede)

Para configurar o WiFi do sensor, acesse:
```
http://[IP_DO_SENSOR]
```

---

## 🐛 Troubleshooting

### Servidor não inicia
- Verifique se a porta 8080 está livre: `lsof -i :8080`
- Verifique se o Java está instalado: `java -version`

### Não consegue conectar ao sensor
- Verifique se o IP e porta do sensor estão corretos
- Verifique se o sensor está ligado e na mesma rede
- Teste a conectividade: `ping [IP_DO_SENSOR]`
- Teste a porta: `telnet [IP_DO_SENSOR] 8000` ou `nc -zv [IP_DO_SENSOR] 8000`

### Erro ao compilar
- Execute: `./mvnw clean package -DskipTests`
- Verifique se o Maven está funcionando: `./mvnw --version`

### Interpretando Erros HTTP da API

Agora o servidor retorna mensagens de erro mais claras:

**503 Service Unavailable** - Erro de conexão com o sensor:
```json
{
  "type": "about:blank",
  "title": "Erro de conexão com o sensor",
  "status": 503,
  "detail": "Não foi possível conectar ao sensor RFID. Verifique se o sensor está ligado e acessível na rede.",
  "path": "/sensor"
}
```
**Solução:** Verifique se o sensor está ligado, acessível na rede e se o IP/porta estão corretos.

**400 Bad Request** - IP do sensor inválido:
```json
{
  "type": "about:blank",
  "title": "Endereço do sensor inválido",
  "status": 400,
  "detail": "O IP do sensor não pôde ser resolvido: [IP]"
}
```
**Solução:** Verifique se o IP do sensor está correto e acessível.

**404 Not Found** - Tags não encontradas:
```json
{
  "type": "about:blank",
  "title": "Lista de tags não encontrada",
  "status": 404,
  "detail": "Nenhuma tag foi detectada pelo sensor"
}
```
**Solução:** Certifique-se de que há tags RFID próximas ao sensor.

**500 Internal Server Error** - Erro genérico:
```json
{
  "type": "about:blank",
  "title": "Erro interno do servidor",
  "status": 500,
  "detail": "Ocorreu um erro ao processar a requisição."
}
```
**Solução:** Verifique os logs do servidor para mais detalhes.

---

## 📝 Notas Importantes

- O servidor precisa ficar rodando para receber requisições
- O CLI funciona apenas no terminal onde o servidor está rodando
- A API REST pode ser acessada de qualquer lugar (se a rede permitir)
- O projeto usa Spring Boot 3.5.5 e Java 17
