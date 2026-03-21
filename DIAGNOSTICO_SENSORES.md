# 🔍 Diagnóstico de Sensores

## 📋 Visão Geral

Este documento ajuda a diagnosticar problemas com sensores RFID na aplicação.

---

## ✅ Verificar se a Aplicação Iniciou

### 1. Confirmar que o servidor está rodando

```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
```

Se receber uma resposta, significa que o servidor está ativo.

---

## ✅ Verificar Configuração de Sensores

### 1. Confirmar Sensores Cadastrados

Os sensores devem estar em `src/main/resources/sensors.json`:

```bash
cat src/main/resources/sensors.json
```

Exemplo esperado:

```json
[
  { "ip": "192.168.2.1",  "porta": 8000, "nome": "sensor-1", "setor": "Setor 01" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2", "setor": "Setor 02" },
  { "ip": "192.168.3.1",  "porta": 8003, "nome": "sensor-3", "setor": "Setor 03" }
]
```

### 2. Se Precisar Alterar `sensors.json`

1. Edite o arquivo: `src/main/resources/sensors.json`
2. **Importantes:** Recompile o projeto:
   ```bash
   mvn clean package
   ```
3. Reinicie a aplicação:
   ```bash
   java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
   ```

⚠️ Se não recompilar, o arquivo antigo dentro do JAR será usado!

---

## ✅ Diagnosticar Problemas de Conexão com Sensor

### 1. Testar Ping ao Sensor

```bash
ping 192.168.2.1
```

Se o ping falhar:
- Sensor pode estar desligado
- Sensor pode não estar na mesma rede
- Rede pode estar desconfigurada

### 2. Testar Porta do Sensor

```bash
# Com netcat
nc -zv 192.168.2.1 8000

# Ou com telnet
telnet 192.168.2.1 8000
```

Se a porta estiver **aberta**, verá: `succeeded`  
Se a porta estiver **fechada**, verá: `refused` ou `timeout`

### 3. Testar Sensor Diretamente

```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
```

Se receber erro, veja o tipo de erro abaixo.

---

## 🐛 Erros Comuns e Soluções

### Erro: "Setor não encontrado para o IP"

**Significa:** O IP e porta não estão em `sensors.json`

**Solução:**
1. Edite `src/main/resources/sensors.json`
2. Adicione o sensor:
   ```json
   { "ip": "192.168.X.X", "porta": YYYY, "nome": "sensor-novo", "setor": "Setor X" }
   ```
3. **Recompile:** `mvn clean package`
4. **Reinicie:** `java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar`

---

### Erro: "Timeout ao conectar ao sensor"

**Significa:** Não consegue conectar ao sensor no tempo limite (5 segundos)

**Causas comuns:**
- Sensor está desligado
- IP errado
- Porta errada
- Sensor não está na mesma rede
- Firewall está bloqueando

**Solução:**
1. Verifique se o sensor está ligado
2. Confirme IP e porta: `ping 192.168.2.1`
3. Teste porta: `nc -zv 192.168.2.1 8000`
4. Confirme dados em `sensors.json`

---

### Erro: "Conexão recusada pelo sensor"

**Significa:** Sensor está online, mas a porta está fechada

**Causas:**
- Porta errada
- Sensor não está escutando naquela porta
- Firewall está bloqueando

**Solução:**
1. Confirme que a porta está correta em `sensors.json`
2. Tente acessar o sensor diretamente pelo browser: `http://192.168.2.1`
3. Verifique documentação do sensor

---

### Erro: "Tag não encontrada"

**Significa:** A tag solicitada não existe no sensor

**Solução:**
1. Busque todas as tags: `curl http://localhost:8080/sensor/tags/192.168.2.1/8000`
2. Verifique o ID correto
3. Use o ID correto na busca

---

## 🔧 Checklist de Diagnóstico

- [ ] Servidor está rodando? (porta 8080 acessível)
- [ ] Sensores estão em `sensors.json`?
- [ ] Sensor está ligado?
- [ ] Sensor responde ao ping?
- [ ] Porta do sensor está aberta? (nc -zv)
- [ ] IP e porta estão corretos em `sensors.json`?
- [ ] Você recompilou após alterar `sensors.json`?
- [ ] Tag existe no sensor? (busque todas as tags)

---

## 📝 Logs do Servidor

Ao iniciar, você verá nos logs:

```
INFO o.y.s.SensorConfigService - Loaded 3 sensors from sensors.json
```

Se aparecer "0 sensors", significa que `sensors.json` está vazio ou com erro de formatação JSON.

---

## 🆘 Ainda Não Funciona?

Se tudo acima falhar:

1. **Reinicie tudo:**
   ```bash
   mvn clean package
   java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
   ```

2. **Verifique os logs** - procure por mensagens de erro

3. **Teste manualmente:**
   - Acesse o sensor pelo browser: `http://192.168.X.X`
   - Use um cliente TCP separado para testar porta
   - Verifique se há tags perto do sensor

4. **Consulte documentação do sensor** - pode ter configurações específicas
