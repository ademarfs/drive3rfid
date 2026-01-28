# 📡 API de Sensores RFID - Endpoints

## 🔧 Gerenciamento de Sensores

### 1. Cadastrar Sensor
**POST** `/sensor/cadastrar`

Cadastra um novo sensor no sistema.

**Body (JSON):**
```json
{
  "ip": "192.168.2.1",
  "porta": 8000,
  "nome": "Sensor Principal",
  "ipLocal": "192.168.2.2"
}
```
- `ipLocal` (opcional): IP **desta máquina** na interface que alcança o sensor. Use quando houver **vários adaptadores de rede** (cada sensor em uma rede).

**Exemplo:**
```bash
curl -X POST http://localhost:8080/sensor/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"ip":"192.168.2.1","porta":8000,"nome":"Sensor Principal"}'
```

**Resposta:** `201 Created`
```json
{
  "ip": "192.168.2.1",
  "porta": 8000,
  "nome": "Sensor Principal"
}
```

---

### 2. Listar Sensores
**GET** `/sensor/listar`

Lista todos os sensores cadastrados.

**Exemplo:**
```bash
curl http://localhost:8080/sensor/listar
```

**Resposta:** `200 OK`
```json
[
  {
    "ip": "192.168.2.1",
    "porta": 8000,
    "nome": "Sensor Principal"
  },
  {
    "ip": "192.168.2.2",
    "porta": 8000,
    "nome": "Sensor Secundário"
  }
]
```

---

### 3. Remover Sensor
**DELETE** `/sensor/{ip}/{porta}`

Remove um sensor cadastrado.

**Exemplo:**
```bash
curl -X DELETE http://localhost:8080/sensor/192.168.2.1/8000
```

**Resposta:** `204 No Content` (sucesso) ou `404 Not Found` (sensor não encontrado)

---

## 📋 Leitura de Tags

### 4. Buscar Todas as Tags (com parâmetros)
**GET** `/sensor/tags?ip={ip}&porta={porta}&ipLocal={ipLocal}`

Busca todas as tags de um sensor específico. Use `ipLocal` quando tiver vários adaptadores.

**Exemplo:**
```bash
curl "http://localhost:8080/sensor/tags?ip=192.168.2.1&porta=8000"
curl "http://localhost:8080/sensor/tags?ip=192.168.2.5&porta=9000&ipLocal=192.168.2.10"
```

**Resposta:** `200 OK`
```json
[
  {
    "id": "E20012345678901234567890",
    "setor": "Retifica Mecânica"
  }
]
```

---

### 5. Buscar Todas as Tags (path variables)
**GET** `/sensor/tags/{ip}/{porta}?ipLocal={ipLocal}`

Busca todas as tags de um sensor. Use `?ipLocal=` quando tiver vários adaptadores.

**Exemplo:**
```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
curl "http://localhost:8080/sensor/tags/192.168.2.5/9000?ipLocal=192.168.2.10"
```

**Resposta:** `200 OK` (mesmo formato do endpoint anterior)

---

### 6. Buscar Tag Específica
**GET** `/sensor/tags/{ip}/{porta}/{tagId}?ipLocal={ipLocal}`

Busca uma tag específica em um sensor. Use `?ipLocal=` quando tiver vários adaptadores.

**Exemplo:**
```bash
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890
curl "http://localhost:8080/sensor/tags/192.168.2.5/9000/E20012345678901234567890?ipLocal=192.168.2.10"
```

**Resposta:** `200 OK`
```json
{
  "id": "E20012345678901234567890",
  "setor": "Setor 01"
}
```

---

## 📝 Notas

- Os sensores são armazenados em memória (não persistem após reiniciar o servidor)
- O campo `nome` no cadastro de sensores é opcional
- Todos os endpoints retornam JSON
- CORS está habilitado para todas as origens (`*`)
