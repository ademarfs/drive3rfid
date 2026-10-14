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
  - `GET /sensor/testar-todos` → mapa `"ip:porta"` → objeto para cada sensor do `sensors.json`: `{status: "OK", tagsEncontradas, tags}` ou `{status: "ERRO", mensagem}` (não é string simples)
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

Suportar **Linux e Windows**. A adaptação para Windows está pronta e **validada com sensor real** (14/10/2026) numa **máquina de teste Windows 11** (não é o servidor final). Falta preparar a **máquina Windows que será o servidor** (outra máquina) e testar lá com os 6 sensores.

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
- `a46582b` — adaptação Windows (mantendo Linux; push feito):
  - `executar-servidor.ps1` e `testar-portas-sensor.ps1`: equivalentes aos `.sh` (mesmo `JAVA_OPTS` padrão). Em UTF-8 **com BOM** (necessário para acentos no PowerShell 5.1).
  - `deploy/instalar-servico-windows.ps1`: serviço `sensor-rfid` via **NSSM** (chama `java` direto; reinício em falha após 10 s; log em `logs\servico.log` com rotação na inicialização; exige admin, `nssm` e `java` no PATH). Não lê `JAVA_OPTS` → ajustar com `nssm edit sensor-rfid`.
  - `.gitattributes`: `*.sh`/`mvnw` LF; `*.ps1`/`*.cmd`/`*.bat` CRLF. `.gitignore` ganhou `logs/`.
  - README: seções Windows lado a lado com Linux (Opção 4 = NSSM).
  - Validado: sintaxe dos `.ps1` (parser), `testar-portas-sensor.ps1` (porta aberta/fechada/IP inalcançável), fins de linha, `bash -n` nos `.sh`.
  - **Não validado**: endpoints com sensor real no Windows; instalador NSSM. (Build e start no Windows validados depois, na limpeza abaixo; endpoints com sensor real e instalador NSSM validados na sessão de 14/10/2026.)
- Commit da limpeza de código morto:
  - `pom.xml`: removidos `spring-boot-starter-data-jpa`, `postgresql`, `forms_rt`, `jSerialComm` (só `spring-boot-starter-web` restou).
  - Removidos `docker-compose.yml`, `driver/Reader.java`, `SensorManagerService` (e do construtor do `SensorController`), `SensorDataDTO`, `SensorResponseDTO`.
  - Removidos os excludes de JPA de `application-prod.yml` e `application-local.yml` (excludes de mail mantidos). README ajustado.
  - Medições: jar 51,8 MB → **20,0 MB** (−61%); RAM ~110 MB (inalterada); startup 1,37 s no Windows (perfil prod, sem WARN/ERROR).
  - Validado (Windows): build, start, `/sensor/testar-todos` HTTP 200 (todos ERRO por timeout — rede dos sensores inalcançável).
  - Leitura de tags e códigos de erro validados depois com sensor real (ver sessão de 14/10/2026 abaixo).
- Sessão de 14/10/2026 — máquina de teste Windows 11:
  - Setup:
    - Projeto veio do ZIP do GitHub; pasta ligada ao remoto (`git remote add origin` + `fetch` + `git reset origin/main`, mixed) → `main` rastreando `origin/main`, limpa.
    - JDK 17 Temurin 17.0.20.1 via `winget install EclipseAdoptium.Temurin.17.JDK` em `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`. O instalador definiu `JAVA_HOME` (Machine) e pôs o JDK **na frente** do Oracle Java 8 (`java8path`) no PATH da máquina. Terminais/VS Code já abertos mantêm o PATH antigo (Java 8) até serem reiniciados.
    - NSSM 2.24 via `winget install NSSM.NSSM`, adicionado **só ao PATH do usuário** (não ao da máquina) → funciona no PowerShell elevado do mesmo usuário; outra conta admin precisa do caminho completo ou do PATH da máquina.
  - Validado com sensor real (primeira validação da versão pós-limpeza), JVM com o `JAVA_OPTS` padrão:
    - Build OK (jar 20,0 MB); startup 1,96 s, sem WARN/ERROR; RAM 103 MB após o start, ~111 MB após os testes.
    - `/testar-todos` → 200 (8,7 s): sensor 5 OK, demais ERRO (timeout; esperado).
    - `GET /tags/192.168.5.1/8005` → 200 (8,2 s), 3 tags, Setor 05.
    - `GET /tags/192.168.5.1/8005/{tag}` → 200; `/NAOEXISTE` → 404.
    - Porta errada (8006) → 503 (conexão recusada, 2,1 s); host desconhecido → 400; IP que não responde (`10.255.255.1`) → 503 (connect timeout, 5 s).
  - Validado o serviço NSSM:
    - `deploy\instalar-servico-windows.ps1` rodou como admin (serviço `sensor-rfid` instalado), usando o contorno de execution policy (ver pendências).
    - Roda como `LocalSystem`, início automático, com o `JAVA_OPTS` enxuto fixado pelo instalador (`java` do PATH no momento da instalação = Temurin 17).
    - Endpoints → 200 com o serviço rodando (`/tags/192.168.5.1/8005` em ~9,5 s); RAM ~105 → 112 MB.
    - Log em `logs\servico.log`, sem WARN/ERROR.
    - `nssm stop|start|restart sensor-rfid` OK; reinício automático após matar o `java` (`Stop-Process -Force`) OK (~10 s).
    - Startup pelos logs: primeiro start do serviço 7,3 s; depois 1,86 s (start), 1,39 s (restart) e 1,25 s (reinício automático).
  - **Não validado**: 500 para ip:porta fora do `sensors.json` (exige sensor alcançável cujo ip:porta não esteja no arquivo).
  - Achados na máquina de teste:
    - `nssm remove sensor-rfid confirm` com o serviço rodando só **marca para exclusão** (`DeleteFlag=1`, StartType Disabled) até ele parar → sempre `nssm stop` antes.
    - O NSSM rotacionou `logs\servico.log` a **cada** início do serviço (não só a ~10 MB).
    - Estado atual dessa máquina: o serviço `sensor-rfid` está **marcado para exclusão** (ainda rodando) e aponta para o `nssm` do winget na pasta do usuário.
- `180debd` — `docs/INSTALACAO_SERVIDOR_WINDOWS.md`: tutorial de instalação **manual** do servidor Windows 10/11. Passos: VS Code → Git → JDK 17 → NSSM 2.24-101 em `C:\Program Files\nssm` → clonar em `C:\sensor-rfid` + `safe.directory` → `sensors.json` → compilar → Wi-Fi manual → teste rápido → serviço NSSM → energia → firewall `API RFID 8080`. **Ainda não validado.**
- `f66956e` — README (Opção 4): recomenda NSSM 2.24-101 em `C:\Program Files\nssm` no PATH do sistema (em vez de `winget install NSSM.NSSM`), com link para o passo 4 do tutorial.

## Próximo passo sugerido

1. **Usuário valida a instalação manual** em outra máquina/local, seguindo **só** o `docs/INSTALACAO_SERVIDOR_WINDOWS.md` (checklist final do tutorial), e relata os problemas → corrigir o tutorial.
   - Aproveitar para testar nos **6 sensores**: códigos de erro (404, 503, 400 e **500 com ip:porta fora do `sensors.json`**), `/testar-todos` concorrente e RAM.
2. Depois, corrigir o bug do NPE no `/testar-todos` (ver pendências).
3. Depois, se o usuário quiser, retomar o script de automação da branch `automacao-instalacao` (atualizá-lo para bater com o tutorial validado).
4. Revisar "Problemas conhecidos / pendências" com o usuário.

## Problemas conhecidos / pendências

- **Execution policy do Windows** (pedido do usuário para registrar): rodar `.\deploy\instalar-servico-windows.ps1` no PowerShell admin falhou com "a execução de scripts foi desabilitada neste sistema" (política padrão `Restricted`).
  - Vale para **todos** os `.ps1` (`executar-servidor.ps1`, `testar-portas-sensor.ps1`) e vai acontecer também no servidor.
  - Arquivos vindos de ZIP baixado podem ficar bloqueados mesmo com `RemoteSigned` (Mark of the Web).
  - Contornos sem mudar a configuração da máquina: `powershell -ExecutionPolicy Bypass -File .\deploy\instalar-servico-windows.ps1` (usado com sucesso) ou `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force` naquela janela.
  - O README ainda não menciona isso — proposto adicionar, **não feito**.
- **Bug no `/testar-todos`** (`SensorController`, ~linhas 74–76): `Map.of("mensagem", e.getMessage())` lança NPE quando a exceção não tem mensagem → o `/testar-todos` inteiro responde 500. Correção: `String.valueOf(e.getMessage())`. Usuário decidiu corrigir **depois** de validar a instalação.
- `testar-portas-sensor.ps1` só testa uma lista fixa de portas (8000, 6000, 8080, 80, 23, 5000, 4001); não cobre 8003–8006 nem 9000.
- Não há testes (`src/test` não existe).
- `ipLocal` (query param e campo de `SensorDTO`) é aceito mas ignorado — usuário decidiu manter e só documentar.
- `application-local.yml` tem `spring:` aninhado dentro de `spring:` (rsocket) → essas chaves não têm efeito.
- Timeouts fixos no código (connect 5 s, leitura 10 s, varredura 8 s).
- ip:porta desconhecido → 500 ("Setor não encontrado"), e só depois da leitura de 8 s.
- Leituras simultâneas do mesmo sensor (ex.: duas chamadas a `/testar-todos`) podem dar "Connection reset" → sensor aparece como ERRO (resposta continua 200).
- Segunda instância → só `BindException: Endereço já em uso` cru.
- `servidor.log` (gerado com nohup) não está no `.gitignore`.
- `ENDPOINTS_API.md`, `DIAGNOSTICO_SENSORES.md`, `INSTRUCOES_TESTE.md` podem estar desatualizados; o `README.md` é a fonte de verdade.
- `.idea/dataSources.xml` ainda cita Postgres (config do IntelliJ, fora do escopo da limpeza).

## Decisões do usuário (não refazer sem pedido)

- Recusado: lock de leitura por sensor (Connection reset); aviso no README para parar a instância antiga antes de reiniciar; `servidor.log` no `.gitignore`.
- Manter `ipLocal` (apenas documentado como ignorado).
- Aprovado e feito: 404 em vez de 500; `/testar-todos` em paralelo; CLI só com TTY.
- Manter suporte aos dois SOs (Linux e Windows); serviço Windows via **NSSM**.
- Commitar a limpeza de código morto antes de validá-la em sensor real (validação fica para o teste na empresa).
- A instalação do servidor será feita e validada **manualmente** primeiro, usando só o `docs/INSTALACAO_SERVIDOR_WINDOWS.md`, em outra máquina/local.
- Script de automação (preparação do servidor) existe **isolado** na branch local `automacao-instalacao` (commit `6babd57`, não validado, sem push): manter isolado do projeto — **nunca** referenciá-lo em README/docs/commits da `main`; retomar só a pedido do usuário, depois de validada a instalação manual.
- Servidor: NSSM em `C:\Program Files\nssm` (não winget); projeto em `C:\sensor-rfid`; regra de firewall `API RFID 8080`; VS Code em cada servidor; `sensors.json` continua dentro do jar (editar e recompilar manualmente).

## Fluxo de trabalho

- Commits direto na `main` (sem PR), mensagem no estilo `tipo: descrição` em português.
- O Claude não consegue dar push (sem credenciais do GitHub no shell): faça o commit e peça ao usuário para rodar `git push origin main`. Em 14/10/2026 a `main` local está **à frente** do `origin/main` (commits `4da3fea`, `180debd`, `f66956e` e o desta atualização) → o usuário precisa rodar `git push origin main`.
- Sempre validar com build e execução reais.
- Máquina de dev Windows anterior (sessões até 13/10/2026): JDK 17 (Temurin 17.0.20.1, via winget) em `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` → build e execução locais funcionavam. Ela **não alcançava** a rede Wi-Fi dos sensores; a VPN alcançava só servidores `10.120.x`, que rodam a versão implantada (não usar para validar builds novos).
- **Máquina de teste Windows 11** (atual, desde 14/10/2026; **não é o servidor final**): plano é validar tudo aqui e depois adaptar o projeto em **outra máquina Windows** que será o servidor. Tem **um adaptador Wi-Fi** ("Wi-Fi 4", IP `192.168.5.2`) ligado ao AP do sensor 5 → alcança só `192.168.5.1:8005`; um sensor por vez (os demais aparecem como ERRO no `/testar-todos`, esperado). Setup em "Já feito" (sessão de 14/10/2026).
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

Windows (PowerShell; `-ExecutionPolicy Bypass` contorna a execution policy, ver pendências):

```powershell
.\mvnw.cmd clean package
powershell -ExecutionPolicy Bypass -File .\executar-servidor.ps1   # em outro terminal para os testes abaixo
curl.exe http://localhost:8080/sensor/testar-todos
curl.exe http://localhost:8080/sensor/tags/192.168.5.1/8005
Get-Process java | Select-Object Id, @{n='RAM_MB';e={[int]($_.WorkingSet64/1MB)}}
```

Última atualização: 14 de outubro de 2026 (tutorial de instalação manual; automação isolada em branch)
