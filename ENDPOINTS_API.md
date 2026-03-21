# 📡 API de Sensores RFID - Endpoints

## 📋 Endpoints Disponíveis

A aplicação fornece 2 endpoints para buscar tags de sensores RFID:

---

## 1. Buscar Todas as Tags de um Sensor

**GET** `/sensor/tags/{ip}/{porta}`

Retorna todas as tags RFID detectadas por um sensor específico.

### Parâmetros da URL

| Parâmetro | Tipo    | Descrição |
|-----------|---------|-----------|
| `ip`      | string  | IP do sensor RFID (ex: 192.168.2.1) |
| `porta`   | integer | Porta do sensor (ex: 8000) |

### Exemplo

```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
```

### Resposta (200 OK)

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

## 2. Buscar uma Tag Específica

**GET** `/sensor/tags/{ip}/{porta}/{tagId}`

Retorna uma tag específica do sensor, se encontrada.

### Parâmetros da URL

| Parâmetro | Tipo    | Descrição |
|-----------|---------|-----------|
| `ip`      | string  | IP do sensor RFID (ex: 192.168.2.1) |
| `porta`   | integer | Porta do sensor (ex: 8000) |
| `tagId`   | string  | ID da tag RFID (ex: E20012345678901234567890) |

### Exemplo

```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890
```

### Resposta (200 OK)

```json
{
  "tag": "E20012345678901234567890",
  "setor": "Setor 01"
}
```

---

## ❌ Respostas de Erro

### 404 - Tag Não Encontrada

Quando a tag solicitada não é encontrada no sensor:

```json
{
  "type": "about:blank",
  "title": "Tag não encontrada",
  "status": 404,
  "detail": "Tag de Id: E20012345678901234567890 não encontrada no setor"
}
```

### 404 - Lista de Tags Vazia

Quando o sensor não retorna nenhuma tag:

```json
{
  "type": "about:blank",
  "title": "Lista de tags não encontrada",
  "status": 404,
  "detail": "Nenhuma tag foi detectada pelo sensor"
}
```

### 500 - Erro de Conexão com Sensor

Quando não há conexão com o sensor:

```json
{
  "type": "about:blank",
  "title": "Erro de conexão",
  "status": 500,
  "detail": "Timeout ao conectar ao sensor em 192.168.2.1:8000"
}
```

---

## 📌 Notas Importantes

1. **Configuração de Sensores**: Os sensores devem estar cadastrados em `src/main/resources/sensors.json`
2. **Setor Dinâmico**: O setor retornado é definido em `sensors.json`, não no código
3. **Conectividade**: O sensor deve estar ligado e acessível na rede
4. **Timeout**: Se o sensor não responder em 5 segundos, retorna erro

---

## 🔧 Estrutura do Sensor

Cada sensor em `sensors.json` possui:

```json
{
  "ip": "192.168.2.1",
  "porta": 8000,
  "nome": "sensor-1",
  "setor": "Setor 01"
}
```

---

## 📝 Exemplo de Uso Completo

```bash
# Buscar todas as tags do sensor 1
curl http://localhost:8080/sensor/tags/192.168.2.1/8000

# Buscar uma tag específica
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890

# Buscar tags do sensor 2
curl http://localhost:8080/sensor/tags/192.168.20.1/9000
```
