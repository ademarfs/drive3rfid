# Documenta Alterações: Implementação de Busca Dinâmica de Setor

## 📋 Problema Inicial

A aplicação retornava valores **fixos** e **codificados** para o setor dos sensores:
- Todas as tags retornavam setor = **"Retifica Mecânica"** (hardcoded no código)
- Outras chamadas retornavam setor = **"Setor 01"** (hardcoded no código)
- Não era possível configurar diferentes setores para diferentes sensores sem alterar o código Java

## 🎯 Solução Implementada

Implementamos um sistema de **busca dinâmica** que lê os setores de um arquivo de configuração (`sensors.json`), permitindo alterar setores sem modificar o código Java.

---

## 📁 Arquivos Modificados

### 1️⃣ **SensorDTO.java** 
**Localização:** `src/main/java/org/Yan/infra/DTO/SensorDTO.java`

**Alteração:** Adicionado o campo `setor` ao DTO (Data Transfer Object)

```java
// Antes: Não tinha campo setor
public class SensorDTO {
    private String ip;
    private int porta;
    private String nome;
    private String ipLocal;
}

// Depois: Adicionado campo setor
public class SensorDTO {
    private String ip;
    private int porta;
    private String nome;
    private String ipLocal;
    private String setor;  // ← NOVO CAMPO

    // Novos construtores
    public SensorDTO(String ip, int porta, String nome, String setor) {
        this.ip = ip;
        this.porta = porta;
        this.nome = nome;
        this.setor = setor;
    }

    // Getter e Setter para setor
    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}
```

---

### 2️⃣ **SensorConfigService.java**
**Localização:** `src/main/java/org/Yan/service/SensorConfigService.java`

**Alteração:** Adicionado método `buscarSetorPorIpEPorta()` para buscar dinamicamente o setor

```java
@Service
public class SensorConfigService {
    // ... código existente ...

    // NOVO MÉTODO
    public String buscarSetorPorIpEPorta(String ip, int port) {
        return sensores.stream()
                .filter(sensor -> sensor.getIp().equals(ip) && sensor.getPorta() == port)
                .map(SensorDTO::getSetor)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Setor não encontrado para o IP: " + ip + " e porta: " + port
                ));
    }
}
```

**Como funciona:**
1. Percorre a lista de sensores carregada do `sensors.json`
2. Filtra pelo IP e porta fornecidos
3. Extrai o campo `setor` do sensor encontrado
4. Se encontrar, retorna o setor
5. Se não encontrar, lança exceção com mensagem descritiva

---

### 3️⃣ **SensorService.java**
**Localização:** `src/main/java/org/Yan/service/SensorService.java`

**Alterações:**
- Injetado o `SensorConfigService` via `@Autowired`
- Alterado método `GetAll()` para usar busca dinâmica
- Alterado método `GetById()` para usar busca dinâmica

```java
@Service
public class SensorService implements ISensorService {
    // ... código existente ...

    @Autowired
    private SensorConfigService sensorConfigService;  // ← NOVO

    @Override
    public List<TagDto> GetAll(String ip, int port) {
        // ... código para conectar ao sensor e buscar tags ...
        
        // ANTES (hardcoded):
        // tags.forEach(tag -> {
        //     response.add(new TagDto(tag, "Retifica Mecânica"));
        // });

        // DEPOIS (dinâmico):
        String setor = sensorConfigService.buscarSetorPorIpEPorta(ip, port);  // ← NOVO
        tags.forEach(tag -> {
            response.add(new TagDto(tag, setor));  // ← ALTERADO
        });

        return response;
    }

    @Override
    public TagDto GetById(String tagId, String ip, int port) {
        // ... código para conectar ao sensor e buscar tags ...
        
        // ANTES (hardcoded):
        // findedTag = new TagDto(tag.get(), "Setor 01");

        // DEPOIS (dinâmico):
        String setor = sensorConfigService.buscarSetorPorIpEPorta(ip, port);  // ← NOVO
        findedTag = new TagDto(tag.get(), setor);  // ← ALTERADO

        return findedTag;
    }
}
```

---

### 4️⃣ **sensors.json**
**Localização:** `src/main/resources/sensors.json`

**Alteração:** Adicionado campo `setor` para cada sensor

```json
// ANTES: Sem campo setor
[
  { "ip": "192.168.2.1",  "porta": 8000, "nome": "sensor-1" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2" },
  { "ip": "192.168.3.1",  "porta": 8003, "nome": "sensor-3" },
  { "ip": "192.168.4.1",  "porta": 8004, "nome": "sensor-4" }
]

// DEPOIS: Com campo setor
[
  { "ip": "192.168.2.1",  "porta": 8000, "nome": "sensor-1", "setor": "Setor 01" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2", "setor": "Setor 02" },
  { "ip": "192.168.3.1",  "porta": 8003, "nome": "sensor-3", "setor": "Setor 03" },
  { "ip": "192.168.4.1",  "porta": 8004, "nome": "sensor-4", "setor": "Setor 04" }
]
```

---

## 🔄 Fluxo de Funcionamento (Resumido)

### Antes (Hardcoded)
```
GET /sensor/tags/192.168.20.1/9000
    ↓
SensorService.GetAll()
    ↓
new TagDto(tag, "Retifica Mecânica")  ← Valor fixo no código
    ↓
Resposta: { "tag": "...", "setor": "Retifica Mecânica" }
```

### Depois (Dinâmico)
```
GET /sensor/tags/192.168.20.1/9000
    ↓
SensorService.GetAll()
    ↓
sensorConfigService.buscarSetorPorIpEPorta("192.168.20.1", 9000)
    ↓
% Lê sensors.json
% Encontra o sensor com IP=192.168.20.1 e porta=9000
% Extrai o campo "setor": "Setor 02"
    ↓
new TagDto(tag, "Setor 02")  ← Valor vem do arquivo de configuração
    ↓
Resposta: { "tag": "...", "setor": "Setor 02" }
```

---

## ✅ Vantagens da Solução

| Aspecto | Antes | Depois |
|---------|-------|--------|
| **Configuração** | Hardcoded no Java | Arquivo `sensors.json` |
| **Alteração** | Recompile código | Editar JSON + recompilar |
| **Flexibilidade** | Apenas um setor por endpoint | Múltiplos sensores, múltiplos setores |
| **Manutenção** | Modificar código fonte | Modificar arquivo de configuração |
| **Escalabilidade** | Difícil adicionar novos sensores | Fácil adicionar novos sensores |

---

## 📝 Instruções de Uso

### Para Adicionar um Novo Sensor

1. **Edite `sensors.json`:**
   ```json
   { "ip": "192.168.X.X", "porta": YYYY, "nome": "sensor-X", "setor": "Setor X" }
   ```

2. **Recompile o projeto:**
   ```bash
   mvn clean package
   ```

3. **Execute:**
   ```bash
   java -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar
   ```

### Para Alterar Setor de um Sensor

1. **Edite `sensors.json`** (altere o campo `setor`)
2. **Recompile:** `mvn clean package`
3. **Execute novamente**

⚠️ **IMPORTANTE:** Sempre recompile com `mvn clean package` para incluir as alterações de `sensors.json` no JAR!

---

## 🔗 Dependências Entre Arquivos

```
SensorConfigService
    ├─ Lê: sensors.json
    └─ Método: buscarSetorPorIpEPorta()
        ↓
SensorService
    ├─ Injeta: SensorConfigService
    ├─ Método GetAll()
    └─ Método GetById()
        ↓
SensorDTO
    ├─ Campo: setor
    └─ Getter/Setter: getSetor()
```

---

## 📊 Resumo das Alterações

| Arquivo | Tipo | Alteração |
|---------|------|-----------|
| `SensorDTO.java` | Modificado | Adicionado campo `setor` |
| `SensorConfigService.java` | Modificado | Adicionado método `buscarSetorPorIpEPorta()` |
| `SensorService.java` | Modificado | Injeção de `SensorConfigService` + Uso em 2 métodos |
| `sensors.json` | Modificado | Adicionado campo `setor` para cada sensor |

**Total de alterações:** 4 arquivos modificados

---

## ✨ Conclusão

A solução implementada **desacoplou a lógica de configuração de setor do código Java**, permitindo que os setores sejam gerenciados através de um arquivo de configuração (`sensors.json`). Isso torna a aplicação mais flexível, fácil de manter e escalável!
