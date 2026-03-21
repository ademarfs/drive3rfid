# SensorSerialYan - Aplicação de Leitura de Tags RFID

## 📖 Descrição

**SensorSerialYan** é uma aplicação Spring Boot que comunica com sensores RFID sem fio para ler e gerenciar tags. A aplicação fornece uma API REST para consultar tags de sensores específicos e organiza os dados por setor.

---

## 🚀 Como Começar

### Pré-requisitos
- Java 17+
- Maven 3.6+

### Compilar o Projeto
```bash
cd /home/painel01/drive3rfid
mvn clean package
```

### Executar a Aplicação
```bash
java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
```

A aplicação iniciará em: `http://localhost:8080`

---

## 📡 API Endpoints

### 1. Buscar Todas as Tags de um Sensor
```bash
GET /sensor/tags/{ip}/{porta}

Exemplo:
curl http://localhost:8080/sensor/tags/192.168.2.1/8000
```

**Resposta:**
```json
[
  { "tag": "E20012345678901234567890", "setor": "Setor 01" },
  { "tag": "E20098765432109876543210", "setor": "Setor 01" }
]
```

### 2. Buscar uma Tag Específica
```bash
GET /sensor/tags/{ip}/{porta}/{tagId}

Exemplo:
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890
```

**Resposta:**
```json
{ "tag": "E20012345678901234567890", "setor": "Setor 01" }
```

---

## ⚙️ Configuración

### Cadastro de Sensores
Os sensores são cadastrados no arquivo `sensors.json`:

**Localização:** `src/main/resources/sensors.json`

**Exemplo:**
```json
[
  { "ip": "192.168.2.1",  "porta": 8000, "nome": "sensor-1", "setor": "Setor 01" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2", "setor": "Setor 02" },
  { "ip": "192.168.3.1",  "porta": 8003, "nome": "sensor-3", "setor": "Setor 03" }
]
```

### Adicionar um Novo Sensor
1. Edite `sensors.json` e adicione um novo sensor:
```json
{ "ip": "192.168.X.X", "porta": YYYY, "nome": "sensor-novo", "setor": "Setor XX" }
```

2. Recompile o projeto:
```bash
mvn clean package
```

3. Execute novamente:
```bash
java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
```

---

## 📁 Estrutura do Projeto

```
src/main/java/org/Yan/
├── Application.java                 # Classe principal Spring Boot
├── driver/
│   ├── Reader.java                 # Interface base para leitura
│   └── WirelessReader.java         # Implementação para sensores sem fio
├── exceptions/
│   ├── ApiExceptionHandler.java    # Tratamento global de erros
│   └── apiExceptions/              # Exceções da API
│       ├── TagListNotFoundException
│       └── TagNotFoundException
├── infra/
│   ├── controller/
│   │   └── SensorController.java   # Endpoints REST
│   ├── DTO/
│   │   ├── TagDto.java            # DTO de tags
│   │   ├── SensorDTO.java         # DTO de sensores
│   │   └── SensorResponseDTO.java
│   └── security/
│       └── RequestFilter.java     # Filtro de requisições
└── service/
    ├── ISensorService.java        # Interface do serviço
    ├── SensorService.java         # Implementação principal
    └── SensorConfigService.java   # Serviço de configuração de sensores

src/main/resources/
├── sensors.json                   # Arquivo de configuração dos sensores
├── application.yml               # Configuração geral
├── application-local.yml         # Configuração local
└── application-prod.yml          # Configuração produção
```

---

## 🔄 Fluxo de Uso

### Consultar Tags de um Sensor

```
Cliente faz requisição GET /sensor/tags/192.168.2.1/8000
    ↓
SensorController recebe a requisição
    ↓
SensorService.GetAll() é chamado
    ↓
WirelessReader conecta ao sensor via Socket TCP
    ↓
Sensor retorna lista de tags distintas
    ↓
SensorConfigService busca o setor do sensor em sensors.json
    ↓
Cada tag é retornada com seu setor correspondente
    ↓
Resposta JSON é enviada ao cliente
```

---

## 💡 Como Funciona a Busca Dinâmica de Setor

A aplicação **não tem valores de setor fixos no código**. Em vez disso:

1. **Cada sensor** é cadastrado em `sensors.json` com seu `setor`
2. **Quando uma requisição chega**, a aplicação:
   - Conecta ao sensor (IP + porta)
   - Busca as tags
   - **Consulta o arquivo `sensors.json`** para encontrar o setor
   - Retorna as tags com o setor correto

**Vantagem:** Você pode mudar setores editando `sensors.json` sem alterar o código!

---

## 🛠️ Ferramentas Utilizadas

- **Spring Boot 3.5.5** - Framework web
- **Maven** - Gerenciador de dependências
- **Jackson** - Processamento de JSON
- **SLF4J** - Logging

---

## 📝 Exemplos de Uso

### Com cURL

```bash
# Listar todas as tags do sensor 1
curl http://localhost:8080/sensor/tags/192.168.2.1/8000

# Buscar uma tag específica
curl http://localhost:8080/sensor/tags/192.168.2.1/8000/E20012345678901234567890
```

### Com Postman

1. **Método:** GET
2. **URL:** `http://localhost:8080/sensor/tags/{ip}/{porta}`
3. **Clique em Send**

---

## ⚠️ Pontos Importantes

1. **Recompile sempre** que alterar `sensors.json`:
   ```bash
   mvn clean package
   java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
   ```

2. **O arquivo `sensors.json` é empacotado no JAR**:
   - Alterações em `src/main/resources/sensors.json` requerem recompilação
   - Execute `mvn clean package` para incluir as mudanças

3. **Conexão com sensores**:
   - Certifique-se de que os sensores estão ligados
   - Verifique a rede (IP e porta corretos)
   - Use ports conhecidas (ex: 8000-8004)

---

## 🔍 Troubleshooting

### Erro: "Setor não encontrado para o IP"
- Verifique se o IP e porta estão cadastrados em `sensors.json`
- Recompile: `mvn clean package`

### Erro: "Timeout ao conectar ao sensor"
- Verifique se o sensor está ligado
- Verifique a conectividade de rede
- Confirme IP e porta

### Tags vazias
- Sensor pode não ter tags
- Verifique se o sensor está funcionando

---

## 📄 Licença e Informações

Projeto desenvolvido para gerenciamento de sensores RFID em ambientes industriais.

---

**Última atualização:** 21 de março de 2026
