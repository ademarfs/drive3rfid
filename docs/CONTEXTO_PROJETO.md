# Contexto do projeto (handoff entre sessões)

> Este arquivo é o contexto de continuidade para sessões do Claude Code (e para o usuário). Atualize-o ao final de cada sessão de trabalho.

## Propósito e visão geral

- **SensorSerialYan**: API REST *stateless* que lê tags RFID de leitores **Fonkan FI-801** via socket TCP (sobre Wi-Fi) e associa cada tag ao **setor** do leitor.
- Stack: Java 17, Spring Boot 3.5.5, Maven (`./mvnw`), pacote `org.Yan`, jar `target/SensorSerialYan-1.0-SNAPSHOT.jar`, porta 8080.
- Rede: cada sensor cria seu próprio ponto de acesso Wi-Fi; o servidor tem **um adaptador Wi-Fi por sensor**. Cada sensor tem IP/porta distintos (ex.: `192.168.5.1:8005`, `192.168.6.1:8006`).
- `src/main/resources/sensors.json`: lista de `{ip, porta, nome, setor}` com **6 sensores** (Setor 01–06). Fica **dentro do jar** → ao editar, é preciso `./mvnw clean package` e reiniciar.
- Endpoints (`/sensor`, `@CrossOrigin("*")`):
  - `GET /sensor/tags/{ip}/{porta}` → lista de `{tag, setor}`
  - `GET /sensor/tags/{ip}/{porta}/{tagId}` → `{tag, setor}`
  - `GET /sensor/testar-todos` → mapa `"ip:porta"` → `OK`/`ERRO` para cada sensor do `sensors.json`
- Perfil via `APP_PROFILE` (padrão `prod`). `README.md` é a fonte de verdade da documentação.

## Arquitetura e fluxo

- `RequestFilter` (log com UUID) → `SensorController` → `SensorService` (abre um socket novo por requisição; connect 5 s, leitura 10 s) → `driver/WirelessReader` (protocolo binário do FI-801; varredura de ~8 s).
- Setor: `SensorConfigService.buscarSetorPorIpEPorta(ip, porta)` a partir do `sensors.json`.
- `ApiExceptionHandler` (estado atual):
  - **404**: lista de tags vazia / tag não encontrada
  - **503**: timeout / conexão recusada
  - **400**: host desconhecido
  - **500**: demais erros, incluindo setor não encontrado (ip:porta fora do `sensors.json`)
- `/testar-todos` roda **em paralelo** (pool de até 8 threads).
- CLI interativa em `Application` só inicia com TTY (`System.console() != null`).

## Objetivo atual

Refatorar para rodar em **servidor fraco** (pouca RAM/CPU). A máquina ainda está sendo definida pelo usuário, que decide com base nas medições.

Suportar **Linux e Windows**. A adaptação para Windows está pronta, mas **não foi validada em execução**: o usuário vai testar no servidor da empresa em 14/10/2026 (com VPN conectada, os sensores da empresa ficam alcançáveis).

## Já feito

- `f921893` — sensores 5 e 6 no `sensors.json` e `CLAUDE.md`.
- `a28e49e` — configuração enxuta para poucos recursos:
  - `application-prod.yml`: `lazy-initialization: true`; Tomcat `threads.max: 8`, `min-spare: 1`, `accept-count: 20`.
  - `executar-servidor.sh`: `JAVA_OPTS` padrão (sobrescrevível) `-Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m`.
  - `deploy/sensor-rfid.service`: unidade systemd (testada com `systemd-run --user`; não instalada; ajustar User/caminhos).
- `8846295` — 404 em vez de 500 para tags vazias/não encontradas; `/testar-todos` em paralelo; CLI só com TTY; README auditado e reescrito.
- Medições (padrão → ajustado):
  - RSS: ~205 MB → **~110 MB**
  - Startup: 2,8 s → **1,9 s** (single-core: 5,7 s → 2,7 s)
  - Piso de ~110 MB; abaixo disso só com GraalVM native.
- Commit da adaptação Windows (mantendo Linux):
  - `executar-servidor.ps1` e `testar-portas-sensor.ps1`: equivalentes aos `.sh` (mesmo `JAVA_OPTS` padrão). Em UTF-8 **com BOM** (necessário para acentos no PowerShell 5.1).
  - `deploy/instalar-servico-windows.ps1`: serviço `sensor-rfid` via **NSSM** (chama `java` direto; reinício em falha após 10 s; log em `logs\servico.log` com rotação na inicialização; exige admin, `nssm` e `java` no PATH). Não lê `JAVA_OPTS` → ajustar com `nssm edit sensor-rfid`.
  - `.gitattributes`: `*.sh`/`mvnw` LF; `*.ps1`/`*.cmd`/`*.bat` CRLF. `.gitignore` ganhou `logs/`.
  - README: seções Windows lado a lado com Linux (Opção 4 = NSSM).
  - Validado: sintaxe dos `.ps1` (parser), `testar-portas-sensor.ps1` (porta aberta/fechada/IP inalcançável), fins de linha, `bash -n` nos `.sh`.
  - **Não validado**: build, execução e endpoints no Windows; instalador NSSM.

## Próximo passo sugerido

1. **Testar no Windows** (servidor da empresa, 14/10/2026, com VPN):
   - `.\mvnw.cmd clean package`
   - `.\executar-servidor.ps1`
   - endpoints com `curl.exe` (ver "Como validar rápido")
   - RAM com `Get-Process`
   - instalar o serviço NSSM (`deploy\instalar-servico-windows.ps1`, como admin) e testar start/stop/restart
2. Depois, **remover código e dependências mortas** (não iniciado):

- `pom.xml`: `spring-boot-starter-data-jpa`, `postgresql`, `forms_rt` (IntelliJ), `jSerialComm` (verificado: usado só por `driver/Reader.java`).
- `docker-compose.yml` (Postgres que o código não usa).
- Excludes de autoconfigure que existem só por causa do JPA (DataSource/Hibernate/DataSourceTransactionManager em `application.yml`, `application-prod.yml`, `application-local.yml`).
- `driver/Reader.java` (@deprecated, estado estático, serial).
- `SensorManagerService` (injetado no construtor do `SensorController` mas nunca usado → remover também do construtor), `SensorDataDTO`, `SensorResponseDTO`.

Esperado: jar ~pela metade; RAM quase igual. **Não pode quebrar**: depois, validar build + start + requisições reais a um sensor (ver "Como validar rápido").

## Problemas conhecidos / pendências

- Não há testes (`src/test` não existe).
- `ipLocal` (query param e campo de `SensorDTO`) é aceito mas ignorado — usuário decidiu manter e só documentar.
- `application-local.yml` tem `spring:` aninhado dentro de `spring:` (rsocket) → essas chaves não têm efeito.
- Timeouts fixos no código (connect 5 s, leitura 10 s, varredura 8 s).
- ip:porta desconhecido → 500 ("Setor não encontrado"), e só depois da leitura de 8 s.
- Leituras simultâneas do mesmo sensor (ex.: duas chamadas a `/testar-todos`) podem dar "Connection reset" → sensor aparece como ERRO (resposta continua 200).
- Segunda instância → só `BindException: Endereço já em uso` cru.
- `servidor.log` (gerado com nohup) não está no `.gitignore`.
- `ENDPOINTS_API.md`, `DIAGNOSTICO_SENSORES.md`, `INSTRUCOES_TESTE.md` podem estar desatualizados; o `README.md` é a fonte de verdade.

## Decisões do usuário (não refazer sem pedido)

- Recusado: lock de leitura por sensor (Connection reset); aviso no README para parar a instância antiga antes de reiniciar; `servidor.log` no `.gitignore`.
- Manter `ipLocal` (apenas documentado como ignorado).
- Aprovado e feito: 404 em vez de 500; `/testar-todos` em paralelo; CLI só com TTY.
- Manter suporte aos dois SOs (Linux e Windows); serviço Windows via **NSSM**.
- Não instalar JDK na máquina de desenvolvimento Windows.

## Fluxo de trabalho

- Commits direto na `main` (sem PR), mensagem no estilo `tipo: descrição` em português.
- O Claude não consegue dar push (sem credenciais do GitHub no shell): faça o commit e peça ao usuário para rodar `git push origin main`.
- Sempre validar com build e execução reais.
- O ambiente de dev pode ser **Windows sem JDK** (sem build/execução local; só validação de sintaxe). Nesse caso, a validação real fica para o servidor.
- Sensor real alcançável durante o desenvolvimento: `192.168.5.1:8005` (pode ser diferente em outra máquina).
- Linha de comando da JVM usada nas verificações = `JAVA_OPTS` padrão do `executar-servidor.sh`:
  `java -Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -jar target/SensorSerialYan-1.0-SNAPSHOT.jar`

## Como validar rápido

```bash
./mvnw clean package
nohup ./executar-servidor.sh < /dev/null > servidor.log 2>&1 &   # pare antes qualquer instância antiga
curl http://localhost:8080/sensor/testar-todos
curl http://localhost:8080/sensor/tags/192.168.5.1/8005
curl http://localhost:8080/sensor/tags/192.168.5.1/8005/<tagId>
ps -o pid,rss,nlwp,cmd -p $(pgrep -f 'java .*SensorSerialYan')   # RSS em KB (~110 MB esperado)
```

Windows (PowerShell):

```powershell
.\mvnw.cmd clean package
.\executar-servidor.ps1                                  # em outro terminal para os testes abaixo
curl.exe http://localhost:8080/sensor/testar-todos
curl.exe http://localhost:8080/sensor/tags/192.168.5.1/8005
Get-Process java | Select-Object Id, @{n='RAM_MB';e={[int]($_.WorkingSet64/1MB)}}
```

Última atualização: 13 de outubro de 2026 (adaptação Windows)
