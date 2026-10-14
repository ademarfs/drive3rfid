# SensorSerialYan - Aplicação de Leitura de Tags RFID

## 📖 Descrição

**SensorSerialYan** é uma aplicação Spring Boot que comunica com sensores RFID sem fio (leitores Fonkan FI-801, via TCP/Wi-Fi) para ler e gerenciar tags. A aplicação fornece uma API REST para consultar tags de sensores específicos e organiza os dados por setor.

---

## 🚀 Como Começar

### Pré-requisitos
- Java 17+
- Maven **não** precisa estar instalado: o projeto inclui o Maven Wrapper (`./mvnw` no Linux, `.\mvnw.cmd` no Windows), que baixa o Maven automaticamente na primeira compilação.

#### Pré-requisitos adicionais no Windows
- JDK 17 instalado, com `java` no `PATH` (e `JAVA_HOME` apontando para o JDK, usado pelo `mvnw.cmd`).
- Um adaptador Wi-Fi por sensor, cada um conectado ao ponto de acesso do seu sensor.
- Cada sensor deve estar em uma **sub-rede distinta** (ex.: `192.168.5.x`, `192.168.6.x`): o `ipLocal` é ignorado, então é o Windows que escolhe a interface pela tabela de rotas.
- Se a API for acessada por outras máquinas, libere a porta 8080 no Firewall do Windows (PowerShell como administrador):
  ```powershell
  New-NetFirewallRule -DisplayName "API RFID 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
  ```

### Compilar o Projeto
**Linux:**
```bash
cd /caminho/para/drive3rfid   # a pasta do projeto
./mvnw clean package
```
**Windows (PowerShell):**
```powershell
cd C:\caminho\para\drive3rfid
.\mvnw.cmd clean package
```
O resultado é o arquivo `target/SensorSerialYan-1.0-SNAPSHOT.jar`.

### Executar a Aplicação
**Linux:**
```bash
./executar-servidor.sh
```
**Windows (PowerShell):**
```powershell
.\executar-servidor.ps1
# se a política de execução bloquear scripts:
powershell -ExecutionPolicy Bypass -File .\executar-servidor.ps1
```
Os scripts já usam as opções da JVM abaixo, que reduzem o uso de memória (~110 MB) para servidores com poucos recursos:
```
-Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m
```
(Equivale a `java <opções acima> -jar ./target/SensorSerialYan-1.0-SNAPSHOT.jar`.)

A aplicação iniciará em: `http://localhost:8080`

### Perfis
O perfil ativo é definido em `application.yml` (`spring.profiles.active: ${APP_PROFILE:prod}`). Sem configurar nada, o perfil usado é **`prod`**, que:
- inicializa os componentes de forma preguiçosa (*lazy initialization*), só quando são usados pela primeira vez;
- limita o Tomcat a no máximo 8 threads (mínimo 1 ociosa) e fila de 20 conexões;
- desativa a configuração automática de banco de dados, JPA e e-mail.

Recomenda-se manter o padrão `prod`. Para usar outro perfil: `APP_PROFILE=local ./executar-servidor.sh` (Windows: `$env:APP_PROFILE = 'local'; .\executar-servidor.ps1`).

---

## ▶️ Como Iniciar o Serviço

### Pré-requisito: compilar
O script de inicialização **não compila** o projeto. Compile antes de iniciar:
```bash
./mvnw clean package          # Linux
.\mvnw.cmd clean package      # Windows (PowerShell)
```

> Sempre que editar `sensors.json`, **recompile e reinicie** o serviço (o arquivo fica dentro do JAR e é lido uma única vez).

### Opção 1 — Terminal (para testes)
```bash
./executar-servidor.sh        # Linux
.\executar-servidor.ps1       # Windows (PowerShell)
```
- Quando aparecer no log `Started Application in X seconds`, a API está pronta.
- A mensagem `Loaded N sensors from sensors.json` **não** aparece na inicialização: ela só surge na **primeira requisição** (por causa da inicialização preguiçosa do perfil `prod`). Isso é normal.
- No terminal aparece `CLI DO SENSOR ATIVO` (menu antigo, legado). Ele aceita `1` = testar conexão e `2` = listar tags (pergunta IP e porta); entradas inválidas apenas pedem de novo. Não é necessário usá-lo — a API funciona independentemente.
- Para parar: `Ctrl+C`. O serviço também para ao fechar o terminal.

### Opção 2 — Segundo plano (nohup, Linux)
No Windows, use a Opção 4 (serviço via NSSM).
```bash
# Iniciar
nohup ./executar-servidor.sh < /dev/null > servidor.log 2>&1 &

# Acompanhar o log
tail -f servidor.log

# Parar
pkill -f 'java .*SensorSerialYan'
```
- Continua rodando após fechar o terminal, mas **não inicia sozinho** quando o servidor é ligado.
- Sem terminal interativo, o log mostra `CLI desativado (sem terminal interativo)` — esperado.

### Opção 3 — Serviço do sistema (systemd, Linux) — recomendado para o servidor
Vantagens:
- Inicia automaticamente quando o servidor é ligado
- Reinicia sozinho em caso de falha (após 10 s)
- Não precisa de ninguém logado

1. Edite `deploy/sensor-rfid.service` e ajuste `User`, `WorkingDirectory` e `ExecStart` para o usuário e a pasta do projeto na máquina (os caminhos que vêm no arquivo são apenas de exemplo).
2. Instale e ative:
```bash
sudo cp deploy/sensor-rfid.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now sensor-rfid
```
O log mostrará `CLI desativado (sem terminal interativo)` — esperado.

Comandos do dia a dia:

| Ação | Comando |
|------|---------|
| Ver status | `systemctl status sensor-rfid` |
| Ver logs (ao vivo) | `journalctl -u sensor-rfid -f` |
| Parar | `sudo systemctl stop sensor-rfid` |
| Iniciar | `sudo systemctl start sensor-rfid` |
| Reiniciar (necessário após recompilar) | `sudo systemctl restart sensor-rfid` |
| Não iniciar mais no boot | `sudo systemctl disable sensor-rfid` |

### Opção 4 — Serviço do Windows (NSSM) — recomendado para servidor Windows
O script `deploy\instalar-servico-windows.ps1` registra a API como serviço do Windows usando o [NSSM](https://nssm.cc), com o mesmo comportamento da unidade systemd: inicia no boot, reinicia sozinho em caso de falha (após 10 s), diretório de trabalho na raiz do projeto e sem entrada interativa. O serviço chama o `java` diretamente (com as mesmas opções da JVM padrão), então não depende da política de execução do PowerShell.

1. Instale o NSSM e confira que ele está no `PATH` (abra um novo terminal depois de instalar):
   ```powershell
   winget install NSSM.NSSM
   ```
   (ou baixe de https://nssm.cc e coloque o `nssm.exe` em uma pasta do `PATH`.)
2. Compile o projeto (`.\mvnw.cmd clean package`).
3. Em um PowerShell **aberto como administrador**, na pasta do projeto:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\deploy\instalar-servico-windows.ps1
   nssm start sensor-rfid
   ```

O log fica em `logs\servico.log` (rotacionado pelo NSSM a cada ~10 MB, ao reiniciar o serviço) e mostrará `CLI desativado (sem terminal interativo)` — esperado.

Comandos do dia a dia (PowerShell como administrador):

| Ação | Comando |
|------|---------|
| Ver status | `nssm status sensor-rfid` |
| Ver logs (ao vivo) | `Get-Content logs\servico.log -Wait -Tail 50` |
| Parar | `nssm stop sensor-rfid` |
| Iniciar | `nssm start sensor-rfid` |
| Reiniciar (necessário após recompilar) | `nssm restart sensor-rfid` |
| Remover o serviço | `nssm stop sensor-rfid; nssm remove sensor-rfid confirm` |

> Para recompilar com o serviço rodando, pare-o antes (`nssm stop sensor-rfid`): no Windows o `.jar` em uso fica bloqueado e o `clean` falha.

### Verificar se está funcionando
**Linux:**
```bash
# Testa todos os sensores em paralelo (leva ~8–10 s no total)
curl http://localhost:8080/sensor/testar-todos

# Tags de um sensor (leva pelo menos 8 s)
curl http://localhost:8080/sensor/tags/<ip>/<porta>

# Uso de memória (esperado: RSS ~110000 KB)
ps -o pid,rss,nlwp,cmd -p $(pgrep -f 'java .*SensorSerialYan')
```
**Windows (PowerShell):** no PowerShell, `curl` é um apelido de `Invoke-WebRequest`; use `curl.exe` ou `Invoke-RestMethod`.
```powershell
# Testa todos os sensores em paralelo (leva ~8–10 s no total)
curl.exe http://localhost:8080/sensor/testar-todos

# Tags de um sensor (leva pelo menos 8 s)
Invoke-RestMethod http://localhost:8080/sensor/tags/<ip>/<porta>

# Uso de memória (esperado: ~110 MB)
Get-Process java | Select-Object Id, @{n='RSS_MB';e={[math]::Round($_.WorkingSet64/1MB)}}
```

### Ajustar memória
As opções da JVM podem ser trocadas pela variável `JAVA_OPTS` (padrão: `-Xmx64m`). Atenção: `JAVA_OPTS` **substitui** todas as opções padrão (não acrescenta). Por isso, informe o conjunto completo, mudando só o valor desejado. Exemplo aumentando o limite de memória de 64 MB para 128 MB:
```bash
JAVA_OPTS="-Xms16m -Xmx128m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m" ./executar-servidor.sh
```
No Windows (PowerShell):
```powershell
$env:JAVA_OPTS = '-Xms16m -Xmx128m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m'
.\executar-servidor.ps1
```
No serviço do Windows (NSSM), o `JAVA_OPTS` não é usado: as opções ficam nos parâmetros do serviço. Para alterá-las, edite com `nssm edit sensor-rfid` (aba *Application*, campo *Arguments*) e reinicie o serviço.
No systemd, adicione na seção `[Service]` de `deploy/sensor-rfid.service`:
```ini
Environment="JAVA_OPTS=-Xms16m -Xmx128m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m"
```
Depois reinstale e reinicie (o `enable --now` **não** reinicia um serviço que já está rodando):
```bash
sudo cp deploy/sensor-rfid.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl restart sensor-rfid
```

---

## 📡 API Endpoints

Todos os endpoints são `GET`, ficam sob `/sensor` e aceitam chamadas de qualquer origem (CORS `*`).

> **Tempo de resposta:** cada leitura de sensor mantém uma janela fixa de **8 segundos** para coletar as tags. Por isso, uma consulta a um sensor acessível leva **pelo menos 8 s**. Se o sensor não responder, a conexão desiste após ~5 s.

### 1. Buscar Todas as Tags de um Sensor
```bash
GET /sensor/tags/{ip}/{porta}

Exemplo:
curl http://localhost:8080/sensor/tags/192.168.5.1/8005
```

**Resposta (200):**
```json
[
  { "tag": "E2806995000050136FD97182", "setor": "Setor 05" },
  { "tag": "E20098765432109876543210", "setor": "Setor 05" }
]
```

### 2. Buscar uma Tag Específica
```bash
GET /sensor/tags/{ip}/{porta}/{tagId}

Exemplo:
curl http://localhost:8080/sensor/tags/192.168.5.1/8005/E2806995000050136FD97182
```
A comparação do `tagId` ignora maiúsculas/minúsculas.

**Resposta (200):**
```json
{ "tag": "E2806995000050136FD97182", "setor": "Setor 05" }
```

> Os endpoints 1 e 2 aceitam um parâmetro opcional `?ipLocal=` (ex.: `...?ipLocal=192.168.5.10`). Ele está **reservado e é ignorado** atualmente: a ideia era escolher qual adaptador Wi-Fi da máquina usar, mas isso não foi implementado.

### 3. Testar Todos os Sensores
```bash
GET /sensor/testar-todos

Exemplo:
curl http://localhost:8080/sensor/testar-todos
```
Lê **todos** os sensores de `sensors.json` **em paralelo** (até 8 ao mesmo tempo). O tempo total fica em ~8–10 s, independentemente da quantidade de sensores.

Sempre responde **HTTP 200**, mesmo que algum sensor falhe. O corpo é um objeto com uma chave `"ip:porta"` por sensor:
```json
{
  "192.168.5.1:8005": {
    "status": "OK",
    "tagsEncontradas": 12,
    "tags": [ { "tag": "E2806995000050136FD97182", "setor": "Setor 05" } ]
  },
  "192.168.2.1:8000": {
    "status": "ERRO",
    "mensagem": "Timeout ao conectar ao sensor em 192.168.2.1:8000. Verifique se o sensor está ligado e acessível."
  }
}
```
Se `sensors.json` estiver vazio, responde **400** com o texto `Nenhum sensor cadastrado em sensors.json.`

### Respostas de Erro (endpoints 1 e 2)
Os erros chegam no formato *ProblemDetail* (`application/problem+json`), com os campos `type`, `title`, `status`, `detail`, `instance`, `path` (e `errorType` nos erros de conexão e no 500). Exemplo real:
```json
{
  "type": "about:blank",
  "title": "Tag não encontrada",
  "status": 404,
  "detail": "Tag de Id: NAOEXISTE123 não encontrada no setor",
  "instance": "/sensor/tags/192.168.5.1/8005/NAOEXISTE123",
  "path": "/sensor/tags/192.168.5.1/8005/NAOEXISTE123"
}
```

| Status | `title` | Quando acontece |
|--------|---------|-----------------|
| 404 | `Tag não encontrada` | O sensor leu tags, mas não a tag pedida |
| 404 | `Lista de tags não encontrada` | O sensor não leu nenhuma tag (`detail`: `Nenhuma Tag encontrada no sensor de IP: X`) |
| 503 | `Erro de conexão com o sensor` | Tempo esgotado (~5 s) ou conexão recusada. `detail` termina com `Erro: Connect timed out` (ou similar); `errorType`: `SocketTimeoutException` ou `ConnectException` |
| 400 | `Endereço do sensor inválido` | O IP/endereço informado não pôde ser resolvido |
| 500 | `Erro interno do servidor` | Qualquer outro erro — principalmente `Setor não encontrado para o IP: X e porta: Y` |

---

## ⚙️ Configuração

### Cadastro de Sensores
Os sensores são cadastrados no arquivo `sensors.json`:

**Localização:** `src/main/resources/sensors.json`

**Conteúdo atual (6 sensores):**
```json
[
  { "ip": "192.168.2.1",  "porta": 8000, "nome": "sensor-1", "setor": "Setor 01" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2", "setor": "Setor 02" },
  { "ip": "192.168.3.1",  "porta": 8003, "nome": "sensor-3", "setor": "Setor 03" },
  { "ip": "192.168.4.1",  "porta": 8004, "nome": "sensor-4", "setor": "Setor 04" },
  { "ip": "192.168.5.1",  "porta": 8005, "nome": "sensor-5", "setor": "Setor 05" },
  { "ip": "192.168.6.1",  "porta": 8006, "nome": "sensor-6", "setor": "Setor 06" }
]
```
Cada sensor também aceita um campo opcional `"ipLocal"`, que hoje **não é usado**.

Como o arquivo funciona:
- Ele é **empacotado dentro do JAR** na compilação.
- É lido **uma única vez** (na primeira requisição) e mantido em memória.
- Se não puder ser lido (ex.: JSON inválido), a aplicação registra um aviso no log e continua com a lista **vazia** — nesse caso `/sensor/testar-todos` responde 400 e as consultas de tags terminam em erro 500 ("Setor não encontrado").

### Adicionar um Novo Sensor
1. Edite `src/main/resources/sensors.json` e adicione um novo sensor (cuidado com as vírgulas entre os itens):
```json
{ "ip": "192.168.X.X", "porta": YYYY, "nome": "sensor-novo", "setor": "Setor XX" }
```

2. Recompile o projeto:
```bash
./mvnw clean package          # Linux
.\mvnw.cmd clean package      # Windows (pare o serviço antes: nssm stop sensor-rfid)
```

3. Reinicie o serviço:
```bash
sudo systemctl restart sensor-rfid    # se estiver usando systemd
nssm start sensor-rfid                # Windows com NSSM (após o stop do passo 2)
# ou pare (Ctrl+C / pkill) e rode novamente ./executar-servidor.sh (ou .\executar-servidor.ps1)
```

---

## 📁 Estrutura do Projeto

```
src/main/java/org/Yan/
├── Application.java                 # Classe principal Spring Boot + menu CLI legado
├── driver/
│   ├── Reader.java                 # Classe legada (@deprecated), não usada
│   └── WirelessReader.java         # Protocolo TCP do leitor Fonkan FI-801
├── exceptions/
│   ├── ApiExceptionHandler.java    # Tratamento global de erros (ProblemDetail)
│   ├── apiExceptions/              # Exceções da API (404)
│   │   ├── TagNotFoundException.java
│   │   └── TagListNotFoundException.java
│   └── domainExceptions/
│       ├── InvalidResponseReaderException.java
│       └── NetWorkErrorException.java
├── infra/
│   ├── controller/
│   │   └── SensorController.java   # Endpoints REST
│   ├── DTO/
│   │   ├── TagDto.java            # DTO de tags (tag + setor)
│   │   ├── SensorDTO.java         # DTO de sensores (itens do sensors.json)
│   │   ├── SensorDataDTO.java     # Não usado
│   │   └── SensorResponseDTO.java # Não usado
│   └── security/
│       └── RequestFilter.java     # Só registra no log cada requisição (método, caminho, IP, status, duração)
└── service/
    ├── ISensorService.java        # Interface do serviço
    ├── SensorService.java         # Implementação principal (leitura das tags)
    ├── SensorConfigService.java   # Leitura do sensors.json e busca de setor
    └── SensorManagerService.java  # Não usado

src/main/resources/
├── sensors.json                   # Arquivo de configuração dos sensores
├── application.yml               # Configuração geral (perfil ativo)
├── application-prod.yml          # Perfil prod (padrão, enxuto)
└── application-local.yml         # Perfil local

Raiz do projeto:
├── executar-servidor.sh          # Inicia o servidor (com as opções de JVM) — Linux
├── executar-servidor.ps1         # Inicia o servidor (com as opções de JVM) — Windows
├── testar-portas-sensor.sh       # Diagnóstico de rede de um sensor — Linux
├── testar-portas-sensor.ps1      # Diagnóstico de rede de um sensor — Windows
├── deploy/sensor-rfid.service    # Unidade systemd (Linux)
├── deploy/instalar-servico-windows.ps1  # Instala o serviço do Windows via NSSM
├── logs/                         # Log do serviço do Windows (criada pelo instalador; ignorada pelo git)
├── .gitattributes                # Fins de linha: LF para .sh/mvnw, CRLF para .ps1/.cmd/.bat
├── docker-compose.yml            # Sobe um Postgres (legado, não usado pela aplicação)
├── mvnw / mvnw.cmd               # Maven Wrapper (Linux / Windows)
├── ENDPOINTS_API.md
├── DIAGNOSTICO_SENSORES.md
└── INSTRUCOES_TESTE.md
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
WirelessReader conecta ao sensor via Socket TCP (timeout de ~5 s)
    ↓
Sensor é lido por 8 s e retorna a lista de tags distintas
    ↓
SensorConfigService busca o setor do sensor (lista carregada do sensors.json)
    ↓
Cada tag é retornada com seu setor correspondente
    ↓
Resposta JSON é enviada ao cliente
```

---

## 💡 Como Funciona a Busca Dinâmica de Setor

A aplicação **não tem valores de setor fixos no código**. Em vez disso:

1. **Cada sensor** é cadastrado em `sensors.json` com seu `setor`
2. **Na primeira requisição**, a aplicação carrega `sensors.json` para a memória
3. **Quando uma requisição chega**, a aplicação:
   - Conecta ao sensor (IP + porta)
   - Busca as tags
   - **Procura na lista em memória** o setor do par IP + porta (comparação exata do texto do IP e da porta). Isso só acontece se o sensor tiver lido alguma tag.
   - Retorna as tags com o setor correto

**Vantagem:** Você pode mudar setores editando `sensors.json` sem alterar o código Java — mas é preciso **recompilar e reiniciar** o serviço para a mudança valer.

---

## 🛠️ Ferramentas Utilizadas

- **Java 17**
- **Spring Boot 3.5.5** (web) - Framework web
- **Maven** (via Maven Wrapper `./mvnw`) - Gerenciador de dependências
- **Jackson** - Processamento de JSON
- **SLF4J** - Logging

Observações:
- `spring-data-jpa` e `postgresql` estão no `pom.xml`, e o `docker-compose.yml` sobe um Postgres, mas a aplicação **não usa banco de dados** (a configuração automática está desativada). São resquícios legados.
- `jSerialComm` só é usado pela classe legada `Reader.java` (não usada).

---

## 📝 Exemplos de Uso

### Com cURL

```bash
# Listar todas as tags do sensor 5
curl http://localhost:8080/sensor/tags/192.168.5.1/8005

# Buscar uma tag específica
curl http://localhost:8080/sensor/tags/192.168.5.1/8005/E2806995000050136FD97182

# Testar todos os sensores
curl http://localhost:8080/sensor/testar-todos
```

### Com Postman

1. **Método:** GET
2. **URL:** `http://localhost:8080/sensor/tags/{ip}/{porta}`
3. **Clique em Send** (aguarde pelo menos 8 s pela resposta)

---

## ⚠️ Pontos Importantes

1. **Recompile e reinicie sempre** que alterar `sensors.json`:
   ```bash
   ./mvnw clean package
   sudo systemctl restart sensor-rfid    # ou reinicie ./executar-servidor.sh
   ```
   No Windows: `nssm stop sensor-rfid`, `.\mvnw.cmd clean package`, `nssm start sensor-rfid`.

2. **O arquivo `sensors.json` é empacotado no JAR**:
   - Alterações em `src/main/resources/sensors.json` requerem recompilação
   - Ele é lido uma única vez; só um reinício faz a aplicação enxergar as mudanças

3. **Conexão com sensores**:
   - Certifique-se de que os sensores estão ligados
   - Verifique a rede (IP e porta corretos)
   - Use as portas configuradas em `sensors.json` (hoje: 8000, 9000 e 8003–8006)

4. **Tempo de resposta**: cada consulta a um sensor leva pelo menos 8 s. Configure o cliente/integração com um tempo limite de pelo menos ~15 s.

---

## 🔍 Troubleshooting

Para diagnosticar a rede de um sensor, use:
```bash
./testar-portas-sensor.sh <IP>      # Linux
.\testar-portas-sensor.ps1 <IP>     # Windows (PowerShell)
```
O script faz um `ping` e testa portas comuns (8000, 6000, 8080, 80, 23, 5000, 4001), com tempo limite de 2 s por porta. Se a porta do seu sensor não estiver nessa lista, teste-a com `curl http://localhost:8080/sensor/tags/<ip>/<porta>`.

### Erro 500: "Setor não encontrado para o IP"
- Resposta 500 (`Erro interno do servidor`), com `detail` contendo `Setor não encontrado para o IP: X e porta: Y`.
- O par IP + porta da URL não está em `sensors.json` (a comparação é exata).
- Só aparece depois da leitura de 8 s e se o sensor leu alguma tag.
- Corrija `sensors.json`, recompile (`./mvnw clean package` ou `.\mvnw.cmd clean package`) e reinicie o serviço.

### Erro 503: "Timeout ao conectar ao sensor"
- Resposta 503 (`Erro de conexão com o sensor`), `detail` terminando em `Erro: Connect timed out`, `errorType`: `SocketTimeoutException`. No `/sensor/testar-todos` aparece como `"status": "ERRO"` com a mensagem `Timeout ao conectar ao sensor em IP:PORTA...`.
- Verifique se o sensor está ligado
- Verifique a conectividade de rede (`./testar-portas-sensor.sh <IP>` ou `.\testar-portas-sensor.ps1 <IP>`)
- Confirme IP e porta

### Erro 503: "Conexão recusada"
- Resposta 503 (`Erro de conexão com o sensor`), `errorType`: `ConnectException`. No `/sensor/testar-todos` aparece como `Conexão recusada pelo sensor em IP:PORTA...`.
- O equipamento responde na rede, mas não aceita conexões nessa porta: confira a porta configurada no módulo Wi-Fi do sensor e em `sensors.json`.

### Erro 400: "Endereço do sensor inválido"
- O IP informado na URL não pôde ser resolvido. Confira se foi digitado corretamente.

### Tags vazias (erro 404)
- Resposta 404 (`Lista de tags não encontrada`), `detail`: `Nenhuma Tag encontrada no sensor de IP: X`.
- Sensor pode não ter tags ao alcance
- Verifique se o sensor está funcionando

### Erro 404: "Tag não encontrada"
- O sensor leu tags, mas não a tag pedida. Confira o ID da tag (maiúsculas/minúsculas não importam).

---

## 📚 Outros Documentos

- `ENDPOINTS_API.md` — detalhes dos endpoints
- `DIAGNOSTICO_SENSORES.md` — diagnóstico de sensores
- `INSTRUCOES_TESTE.md` — instruções de teste

> Esses documentos podem estar menos atualizados que este README. Em caso de divergência, vale o que está aqui.

---

## 📄 Licença e Informações

Projeto desenvolvido para gerenciamento de sensores RFID em ambientes industriais.

---

**Última atualização:** 13 de outubro de 2026
