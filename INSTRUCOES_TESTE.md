# 🧪 Instruções para Testar a Aplicação

## 📦 Executar a Aplicação

### Opção 1: Compilar e executar
```bash
cd /home/painel01/drive3rfid
mvn clean package
java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
```

### Opção 2: Executar o script (se disponível)
```bash
cd /home/painel01/drive3rfid
./executar-servidor.sh
```

A aplicação será iniciada na porta **8080**.

---

## 🧪 Teste 1: Buscar Todas as Tags de um Sensor

### Via cURL (Terminal)

```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
```

### Via Postman

1. **Método:** GET
2. **URL:** `http://localhost:8080/sensor/tags/192.168.2.1/8000`
3. Clique em **Send**

### Resposta Esperada

```json
[
  {
    "tag": "E20012345678901234567890",
    "setor": "Setor 01"
  },
  {
    "tag": "E20098765432109876543210",
    "setor": "Setor 01"
  }
]
```

---

## 🧪 Teste 2: Buscar uma Tag Específica

### Via cURL (Terminal)

```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890
```

### Via Postman

1. **Método:** GET
2. **URL:** `http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890`
3. Clique em **Send**

### Resposta Esperada

```json
{
  "tag": "E20012345678901234567890",
  "setor": "Setor 01"
}
```

---

## 🔄 Teste 3: Testar Múltiplos Sensores

A aplicação suporta vários sensores cadastrados em `sensors.json`. Para testar diferentes sensores:

```bash
# Sensor 1
curl http://localhost:8080/sensor/tags/192.168.2.1/8000

# Sensor 2
curl http://localhost:8080/sensor/tags/192.168.20.1/9000

# Sensor 3
curl http://localhost:8080/sensor/tags/192.168.3.1/8003
```

Cada sensor retornará suas tags com o setor correspondente.

---

## ⚙️ Antes de Testar

### 1. Verificar Configuração de Sensores

Os sensores devem estar cadastrados em `sensors.json`:

**Localização:** `src/main/resources/sensors.json`

```json
[
  { "ip": "192.168.2.1",  "porta": 8000, "nome": "sensor-1", "setor": "Setor 01" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2", "setor": "Setor 02" },
  { "ip": "192.168.3.1",  "porta": 8003, "nome": "sensor-3", "setor": "Setor 03" }
]
```

### 2. Verificar Sensores Físicos

Antes de testar, certifique-se de que:
- Os sensores RFID estão **ligados**
- Os sensores estão **conectados à rede**
- Você conhece o **IP e porta** de cada sensor
- Os dados em `sensors.json` estão **corretos**

### 3. Testar Conectividade

Para verificar se o sensor é acessível:

```bash
# Teste de ping
ping 192.168.2.1

# Teste de porta (com netcat ou telnet)
nc -zv 192.168.2.1 8000
```

---

## 🐛 Troubleshooting

### Erro: "Setor não encontrado para o IP"

**Possíveis causas:**
- O IP e porta não estão cadastrados em `sensors.json`
- Você não recompilou o projeto após alterar `sensors.json`

**Solução:**
1. Verifique `sensors.json` e confirme que o IP/porta estão corretos
2. Recompile: `mvn clean package`
3. Execute novamente: `java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar`

### Erro: "Timeout ao conectar ao sensor"

**Possíveis causas:**
- O sensor está desligado
- A porta está incorreta
- O sensor não está acessível na rede

**Solução:**
1. Verifique se o sensor está ligado
2. Verifique a conectividade: `ping [IP_DO_SENSOR]`
3. Teste a porta: `nc -zv [IP_DO_SENSOR] [PORTA]`
4. Confirme IP e porta em `sensors.json`

### Erro: "Tag não encontrada"

**Possíveis causas:**
- A tag não existe no sensor
- O ID da tag está incorreto

**Solução:**
1. Busque todas as tags: `curl http://localhost:8080/sensor/tags/192.168.2.1/8000`
2. Verifique o ID exato da tag
3. Use o ID correto na busca específica

### Porta 8080 já está em uso

```bash
# Encontre o processo usando a porta 8080
lsof -i :8080

# Mate o processo
kill -9 [PID]
```

---

## 📝 Exemplo Completo de Teste

```bash
# 1. Compilar
mvn clean package

# 2. Executar
java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar

# 3. Em outro terminal, testar
# Buscar todas as tags do sensor 1
curl http://localhost:8080/sensor/tags/192.168.2.1/8000

# 4. Buscar uma tag específica
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890

# 5. Testar outro sensor
curl http://localhost:8080/sensor/tags/192.168.20.1/9000
```

---

## ✅ Resultado Esperado

Se tudo estiver funcionando:
1. ✅ Busca de todas as tags retorna lista JSON
2. ✅ Busca de tag específica retorna um objeto JSON
3. ✅ Cada tag tem seu `setor` correto
4. ✅ Múltiplos sensores podem ser consultados
