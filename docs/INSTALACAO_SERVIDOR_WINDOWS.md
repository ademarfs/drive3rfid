# Instalação manual do servidor Windows (10/11)

Este roteiro prepara um Windows 10 ou 11 como servidor da API **SensorSerialYan**, passo a passo e à mão. Começa na instalação do VS Code e termina no firewall.

- Cada passo traz o **objetivo**, o **como fazer** (cliques na interface e/ou comandos do PowerShell para copiar e colar) e o **como validar** (comando e saída esperada).
- A documentação da API (endpoints, respostas e códigos de erro) está no [README.md](../README.md), que é a fonte de verdade. Aqui só aparecem as chamadas necessárias para validar a instalação.

---

## Antes de começar

### Requisitos

| Item | Detalhe |
|---|---|
| Sistema | Windows 10 ou 11, 64 bits. Basta o PowerShell 5.1 que já vem com o Windows. |
| Conta | **Administrador** da máquina. É necessária para os instaladores, o serviço, o firewall, o `PATH` da máquina e os drivers dos adaptadores USB. |
| Internet | Necessária para os downloads e para a primeira compilação (o Maven baixa as dependências). Use cabo ou uma rede Wi-Fi que **não** seja de sensor. |
| Hardware | **Um adaptador Wi-Fi USB por sensor**. Hoje há 6 sensores cadastrados (veja o passo 6). |
| Dados dos sensores | Lista com o **SSID e a senha** do Wi-Fi de cada sensor. O IP e a porta de cada um estão no `sensors.json`. |
| Tempo | De 45 a 60 minutos. A maior parte vai nos downloads e na conexão do Wi-Fi de cada sensor. |

### Abrir o PowerShell como administrador

- **Windows 10:** *Iniciar* > digite `PowerShell` > clique com o botão direito em *Windows PowerShell* > **Executar como administrador**. Outra opção é `Win+X` > *Windows PowerShell (Admin)*.
- **Windows 11:** `Win+X` > **Terminal (Administrador)**. Outra opção é *Iniciar* > digite `PowerShell` > **Executar como administrador**.
- Confirme o pedido do UAC ("Deseja permitir que este aplicativo faça alterações...?") clicando em **Sim**.

**Como validar:**

```powershell
([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
```

Esperado: `True`.

### Política de execução de scripts

Por padrão, o Windows bloqueia scripts `.ps1` com a mensagem:

> `... não pode ser carregado porque a execução de scripts foi desabilitada neste sistema.`

Neste roteiro, os scripts do projeto são executados assim:

```powershell
powershell -ExecutionPolicy Bypass -File .\nome-do-script.ps1
```

Outra opção é liberar só para a janela atual do PowerShell (a liberação vale até você fechar a janela):

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

> Isso ignora **apenas** a política de execução do PowerShell. **Não** ignora o UAC nem a exigência de administrador, que continuam valendo. Não mude a política da máquina inteira.

### Downloads pelo PowerShell no Windows 10 antigo

Em alguns Windows 10, o PowerShell 5.1 não usa TLS 1.2 por padrão. Sem ele, o `Invoke-WebRequest` falha com "Não foi possível criar um canal seguro SSL/TLS". Antes de baixar qualquer arquivo pelo PowerShell, rode na mesma janela:

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
```

Pelo navegador, isso não é necessário.

### winget

Os passos usam os **links de download direto** como caminho principal. O `winget` aparece só como alternativa, porque pode não existir no Windows 10: ele vem com o "Instalador de Aplicativo" da Microsoft Store. Para saber se ele existe, rode `winget --version`.

> **Depois de instalar algo que mexe no `PATH`** (passos 1 a 4), feche e reabra o PowerShell e o VS Code. Janelas que já estavam abertas continuam com o `PATH` antigo.

---

## 1. VS Code

**Objetivo:** ter um editor no servidor para alterar o `sensors.json` (e o código, se precisar) e recompilar.

**Como fazer:**

1. Acesse <https://code.visualstudio.com/download> e baixe o **System Installer** > **x64**. O link direto é <https://update.code.visualstudio.com/latest/win32-x64/stable>.
   - Use o instalador **de sistema** (para todos os usuários), e não o *User Installer*.
2. Execute o instalador como administrador e aceite a licença.
3. Na tela *Selecionar Tarefas Adicionais*, deixe marcado **"Adicionar ao PATH"**. As opções "Abrir com Code" no menu de contexto são opcionais.
4. Conclua a instalação.

Alternativa com winget:

```powershell
winget install --id Microsoft.VisualStudioCode -e --scope machine --accept-package-agreements --accept-source-agreements
```

**Opcional:** instale a extensão **Extension Pack for Java** (`vscjava.vscode-java-pack`). Pode ser pelo painel *Extensões* do VS Code ou por um PowerShell novo:

```powershell
code --install-extension vscjava.vscode-java-pack
```

**Como validar** (em um PowerShell novo):

```powershell
Test-Path 'C:\Program Files\Microsoft VS Code\Code.exe'
code --version
```

Esperado: `True` e, em seguida, a versão do VS Code (ex.: `1.xx.x`) na primeira linha.

---

## 2. Git

**Objetivo:** clonar o repositório e, depois, atualizar o código com `git pull`.

**Como fazer:**

1. Acesse <https://git-scm.com/download/win> e baixe o instalador **64-bit** do Git for Windows.
2. Execute o instalador como administrador e mantenha **as opções padrão** em todas as telas (*Next* até *Install*).

Alternativa com winget:

```powershell
winget install --id Git.Git -e --scope machine --accept-package-agreements --accept-source-agreements
```

**Como validar** (em um PowerShell novo):

```powershell
git --version
```

Esperado: `git version 2.x.y.windows.z`.

---

## 3. JDK 17 (Eclipse Temurin)

**Objetivo:** instalar o Java 17 exigido pela API. O `mvnw.cmd` usa o `JAVA_HOME`, e o serviço usa o `java` que estiver no `PATH` no momento da instalação (passo 10).

**Como fazer:**

1. Acesse <https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64&package=jdk> e baixe o **.msi** (JDK 17, Windows, x64).
2. Execute o MSI. Na tela *Custom Setup*, deixe estas opções como **"Will be installed on local hard drive"**:
   - **Add to PATH** (já vem ativada);
   - **Set JAVA_HOME variable** (vem desativada: clique no ícone e ative).
3. Conclua a instalação.

Download pelo PowerShell, se preferir (instala já com `PATH` e `JAVA_HOME`):

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$msi = "$env:TEMP\temurin17.msi"
Invoke-WebRequest -UseBasicParsing 'https://api.adoptium.net/v3/installer/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $msi
Start-Process msiexec.exe -ArgumentList "/i `"$msi`" ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome /qn /norestart" -Wait
```

Alternativa com winget:

```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e --scope machine --accept-package-agreements --accept-source-agreements
```

**Como validar** (em um PowerShell **novo**):

```powershell
java -version
where.exe java
$env:JAVA_HOME
```

Esperado:

- `java -version` mostra `openjdk version "17.0.x"` e uma linha com `Temurin`.
- A **primeira** linha do `where.exe java` está em `C:\Program Files\Eclipse Adoptium\jdk-17...\bin\java.exe`.
- O `JAVA_HOME` aponta para `C:\Program Files\Eclipse Adoptium\jdk-17...`.

### Se aparecer `java version "1.8..."`

Nesse caso, um Java 8 da Oracle está antes do JDK 17 no `PATH`. Ele costuma ficar em `C:\Program Files (x86)\Common Files\Oracle\Java\java8path` ou em `...\javapath`. Para corrigir:

1. `Win+R` > digite `sysdm.cpl` > Enter > aba **Avançado** > **Variáveis de Ambiente...**.
2. Em **Variáveis do sistema**, selecione `Path` > **Editar...**.
3. Selecione a linha `C:\Program Files\Eclipse Adoptium\jdk-17...\bin` e clique em **Mover para Cima** até ela ficar acima das linhas da Oracle. O ideal é deixá-la no topo.
4. Clique em **OK** em todas as janelas.
5. Feche e reabra o PowerShell e o VS Code, e repita a validação.

Se o `JAVA_HOME` estiver vazio ou errado, ajuste-o na mesma tela (*Variáveis do sistema* > `JAVA_HOME`) ou pelo comando abaixo, trocando o nome da pasta pelo nome real:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\jdk-17.0.X.Y-hotspot', 'Machine')
```

---

## 4. NSSM

**Objetivo:** instalar o NSSM, que transforma o `java -jar` em um serviço do Windows. Use o build **2.24-101**, porque a versão 2.24 "estável" tem um bug a partir do Windows 10 Creators Update. O NSSM fica em `C:\Program Files\nssm`, uma pasta da máquina, porque o serviço roda como LocalSystem.

> **Não** use o `winget install NSSM.NSSM`: ele instala o NSSM na pasta do usuário.

**Como fazer (pela interface):**

1. Baixe <https://nssm.cc/ci/nssm-2.24-101-g897c7ad.zip> pelo navegador.
2. Clique com o botão direito no ZIP > **Propriedades** > marque **Desbloquear** (se a opção aparecer) > **OK**.
3. Extraia o ZIP e copie o arquivo `win64\nssm.exe` para a pasta `C:\Program Files\nssm\` (crie a pasta).
4. Coloque essa pasta no `PATH` do sistema: `Win+R` > `sysdm.cpl` > **Avançado** > **Variáveis de Ambiente...** > em *Variáveis do sistema*, `Path` > **Editar...** > **Novo** > `C:\Program Files\nssm` > **OK** em todas as janelas.

**Como fazer (pelo PowerShell, como administrador):**

```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$zip = "$env:TEMP\nssm.zip"
Invoke-WebRequest -UseBasicParsing 'https://nssm.cc/ci/nssm-2.24-101-g897c7ad.zip' -OutFile $zip
Unblock-File $zip
Expand-Archive $zip "$env:TEMP\nssm" -Force
New-Item -ItemType Directory -Force 'C:\Program Files\nssm' | Out-Null
$exe = Get-ChildItem "$env:TEMP\nssm" -Recurse -Filter nssm.exe | Where-Object { $_.FullName -match '\\win64\\' } | Select-Object -First 1
Copy-Item $exe.FullName 'C:\Program Files\nssm\nssm.exe' -Force

# Acrescenta a pasta ao PATH da máquina (rode uma vez só)
$k = 'HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\Environment'
$p = (Get-Item $k).GetValue('Path', '', 'DoNotExpandEnvironmentNames')
Set-ItemProperty $k Path ("C:\Program Files\nssm;" + $p) -Type ExpandString
```

> Depois de mexer no `PATH`, **feche e reabra o PowerShell** (e o VS Code). O instalador do serviço (passo 10) só encontra o `nssm` pelo `PATH`, então ele precisa rodar em um terminal aberto **depois** deste passo.

**Como validar** (em um PowerShell **novo**):

```powershell
nssm version
(Get-Command nssm).Source
```

Esperado: a versão contém `2.24-101`, e o caminho é `C:\Program Files\nssm\nssm.exe`.

---

## 5. Clonar o projeto em `C:\sensor-rfid`

**Objetivo:** baixar o código. A pasta fica fora do perfil do usuário porque o serviço roda como LocalSystem.

**Como fazer** (PowerShell como administrador):

```powershell
git clone --branch main https://github.com/ademarfs/drive3rfid.git C:\sensor-rfid
git config --system --add safe.directory C:/sensor-rfid
```

- O repositório é público: o clone não pede login.
- O `safe.directory` evita o erro `detected dubious ownership in repository at 'C:/sensor-rfid'`. Esse erro aparece quando um usuário comum (por exemplo, no VS Code aberto sem administrador) usa uma pasta criada pelo administrador. O `--system` vale para todas as contas da máquina.

Abra o projeto no VS Code:

```powershell
code C:\sensor-rfid
```

Se o VS Code perguntar se você confia nos autores da pasta, responda **"Sim, confio nos autores"**.

**Como validar:**

```powershell
git -C C:\sensor-rfid log -1 --oneline
git config --system --get-all safe.directory
```

Esperado: o hash e a mensagem do último commit, e `C:/sensor-rfid` na lista do `safe.directory`.

---

## 6. Conferir o `sensors.json`

**Objetivo:** confirmar que o IP, a porta e o setor de cada sensor estão corretos **antes** de compilar.

**Como fazer:** no VS Code, abra `src\main\resources\sensors.json`.

Conteúdo atual (6 sensores):

| Nome | IP | Porta | Setor |
|---|---|---|---|
| sensor-1 | 192.168.2.1 | 8000 | Setor 01 |
| sensor-2 | 192.168.20.1 | 9000 | Setor 02 |
| sensor-3 | 192.168.3.1 | 8003 | Setor 03 |
| sensor-4 | 192.168.4.1 | 8004 | Setor 04 |
| sensor-5 | 192.168.5.1 | 8005 | Setor 05 |
| sensor-6 | 192.168.6.1 | 8006 | Setor 06 |

- Se algo mudar, edite e salve. O formato e as regras estão no [README, "Cadastro de Sensores"](../README.md#cadastro-de-sensores).
- O arquivo é **empacotado dentro do jar**. Toda alteração exige recompilar (passo 7) e reiniciar o serviço.
- Cada sensor precisa de uma **sub-rede distinta** (ex.: `192.168.5.x`, `192.168.6.x`), porque é o Windows que escolhe o adaptador pela tabela de rotas.

**Como validar** (o JSON é válido e lista os sensores):

```powershell
Get-Content C:\sensor-rfid\src\main\resources\sensors.json -Raw -Encoding UTF8 | ConvertFrom-Json | Format-Table nome, ip, porta, setor
```

Esperado: uma tabela com os sensores. Se o JSON tiver erro (como uma vírgula faltando), aparece um erro do `ConvertFrom-Json`.

---

## 7. Compilar

**Objetivo:** gerar o jar `target\SensorSerialYan-1.0-SNAPSHOT.jar`.

**Como fazer:**

```powershell
cd C:\sensor-rfid
.\mvnw.cmd clean package
```

A primeira compilação precisa de internet, porque o wrapper baixa o Maven e as dependências. Ela pode levar alguns minutos.

**Como validar:**

```powershell
Get-Item C:\sensor-rfid\target\SensorSerialYan-1.0-SNAPSHOT.jar | Select-Object Name, @{n='MB';e={[math]::Round($_.Length/1MB,1)}}
```

Esperado: a compilação termina com `BUILD SUCCESS`, e o jar tem cerca de **20 MB**.

> Se o serviço já estiver instalado e rodando, pare-o antes de compilar (`nssm stop sensor-rfid`). No Windows, o jar em uso fica bloqueado e o `clean` falha.

---

## 8. Conectar o Wi-Fi dos sensores

**Objetivo:** conectar cada adaptador Wi-Fi USB ao ponto de acesso de **um** sensor, com reconexão automática. Cada sensor é o seu próprio ponto de acesso, e cada adaptador só alcança o sensor ao qual está conectado.

**Como fazer:**

1. **Conecte os adaptadores USB.** Se o Windows não reconhecer um deles sozinho, instale o driver do fabricante (pode exigir administrador). Cada adaptador aparece como `Wi-Fi`, `Wi-Fi 2`, `Wi-Fi 3`...
   ```powershell
   Get-NetAdapter -Physical | Where-Object { $_.PhysicalMediaType -match '802.11' } | Format-Table Name, Status, InterfaceDescription
   ```
2. **Conecte cada adaptador a um sensor, manualmente:**
   - Clique no ícone de rede da barra de tarefas.
     - **Windows 11:** clique na seta **>** ao lado do bloco *Wi-Fi*.
     - **Windows 10:** o painel já mostra a lista de redes.
   - Com vários adaptadores, o Windows mostra um **seletor de adaptador** no topo da lista (ex.: *Wi-Fi 2*). Escolha o adaptador.
   - Escolha a rede (SSID) do sensor, marque **"Conectar automaticamente"**, clique em *Conectar* e digite a senha.
   - Repita para cada sensor, sempre com um adaptador diferente.
3. **Confira** o SSID e o IP de cada adaptador:
   ```powershell
   netsh wlan show interfaces
   Get-NetIPAddress -AddressFamily IPv4 | Where-Object InterfaceAlias -like 'Wi-Fi*' | Format-Table InterfaceAlias, IPAddress
   ```
   - Cada adaptador deve estar com `Estado: conectado`, o SSID do seu sensor e um IP na sub-rede desse sensor (ex.: `192.168.5.x` para o sensor `192.168.5.1`).
   - **Windows 11 24H2 ou mais novo:** o `netsh wlan show interfaces` só mostra o SSID se a **Localização** estiver permitida. Ative em *Configurações > Privacidade e segurança > Localização*, incluindo "Permitir que aplicativos da área de trabalho acessem sua localização".

**Como validar:** teste a porta TCP de cada sensor (um por vez):

```powershell
Test-NetConnection 192.168.5.1 -Port 8005 -InformationLevel Quiet
```

Esperado: `True`. Também dá para testar todos os sensores do `sensors.json` de uma vez:

```powershell
Get-Content C:\sensor-rfid\src\main\resources\sensors.json -Raw -Encoding UTF8 | ConvertFrom-Json |
  ForEach-Object { [pscustomobject]@{ Sensor = $_.nome; Endereco = "$($_.ip):$($_.porta)"; OK = (Test-NetConnection $_.ip -Port $_.porta -InformationLevel Quiet -WarningAction SilentlyContinue) } }
```

Esperado: `OK = True` em todas as linhas.

> O `testar-portas-sensor.ps1` testa só uma lista **fixa** de portas (8000, 6000, 8080, 80, 23, 5000, 4001). Ele não cobre as portas 8003–8006 nem a 9000, por isso aqui se usa o `Test-NetConnection <ip> -Port <porta>`.

---

## 9. Teste rápido da API

**Objetivo:** confirmar que o jar sobe e lê os sensores **antes** de transformá-lo em serviço.

**Como fazer:**

No **primeiro** PowerShell:

```powershell
cd C:\sensor-rfid
powershell -ExecutionPolicy Bypass -File .\executar-servidor.ps1
```

- Espere a linha `Started Application in X seconds`.
- O menu `CLI DO SENSOR ATIVO` que aparece no terminal é legado e pode ser ignorado.
- Se o Windows mostrar o aviso do Firewall perguntando se deseja permitir o acesso do Java, clique em **Permitir acesso**. **Não** clique em *Cancelar*, porque isso cria uma regra que **bloqueia** o `java.exe` (veja o passo 12).

Em um **segundo** PowerShell:

```powershell
curl.exe http://localhost:8080/sensor/testar-todos
curl.exe http://localhost:8080/sensor/tags/192.168.5.1/8005
Get-Process java | Select-Object Id, @{n='RAM_MB';e={[int]($_.WorkingSet64/1MB)}}
```

> No PowerShell, `curl` é um apelido de `Invoke-WebRequest`. Use sempre `curl.exe`.

**Como validar:**

- `/sensor/testar-todos` responde **HTTP 200** depois de cerca de **8 a 13 s**. O corpo tem uma chave `"ip:porta"` por sensor:
  - `"status": "OK"`, com `tagsEncontradas` e `tags`, para cada sensor lido;
  - `"status": "ERRO"`, com `mensagem`, para cada sensor inacessível (volte ao passo 8).
- `/sensor/tags/<ip>/<porta>` devolve a lista de `{ "tag": ..., "setor": ... }` em pelo menos 8 s. Se não houver nenhuma tag ao alcance, a resposta é 404 `Lista de tags não encontrada`.
- A RAM do `java` fica em cerca de **105 a 112 MB**.
- O formato completo das respostas e dos erros está no [README, "API Endpoints"](../README.md#-api-endpoints).

Para encerrar, volte ao primeiro terminal e pressione **Ctrl+C**. Isso é obrigatório antes do passo 10, porque o serviço não sobe com a porta 8080 ocupada.

---

## 10. Instalar o serviço (NSSM) e testar

**Objetivo:** fazer a API iniciar no boot, sem ninguém logado, e reiniciar sozinha em caso de falha.

**Como fazer:** abra um PowerShell **novo como administrador**, depois dos passos 3 e 4 (o instalador usa o `java` do `PATH` atual) e com o teste do passo 9 encerrado:

```powershell
cd C:\sensor-rfid
powershell -ExecutionPolicy Bypass -File .\deploy\instalar-servico-windows.ps1
nssm start sensor-rfid
```

O `deploy\instalar-servico-windows.ps1` faz o seguinte:

- Exige administrador. Usa o `nssm` e o `java` encontrados no `PATH` do terminal atual. Por isso, rode-o em um PowerShell aberto **depois** dos passos 3 e 4.
  - Se o `nssm` não estiver no `PATH`, ele para com a mensagem `nssm não encontrado no PATH` e sugere o `winget install NSSM.NSSM`. **Ignore essa sugestão**: siga o passo 4 e abra um PowerShell novo.
- **Recusa** a instalação se o serviço `sensor-rfid` já existir. Nesse caso, nada é alterado (veja "Remover o serviço").
- Configura o serviço `sensor-rfid` (conta LocalSystem) assim:
  - **Aplicativo:** o `java` encontrado no `PATH`, com os parâmetros `-Xms16m -Xmx64m -Xss256k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -jar "C:\sensor-rfid\target\SensorSerialYan-1.0-SNAPSHOT.jar"`.
  - **Diretório de trabalho:** `C:\sensor-rfid`. **Entrada:** `NUL`.
  - **Log:** saída e erros em `C:\sensor-rfid\logs\servico.log`, com rotação a cada inicialização do serviço (veja "Logs").
  - **Falha:** reinicia o `java` depois de **10 s**.
  - **Início:** automático (`SERVICE_AUTO_START`).
- **Não** confere o resultado de cada comando do `nssm`: ele segue até o fim mesmo se um deles falhar. Leia a saída; se aparecer algum erro do `nssm`, remova o serviço (`nssm remove sensor-rfid confirm`), corrija a causa e rode o instalador de novo.

**Como validar:**

```powershell
Get-Service sensor-rfid | Format-Table Name, Status, StartType
nssm status sensor-rfid
curl.exe http://localhost:8080/sensor/testar-todos
Get-Content C:\sensor-rfid\logs\servico.log -Tail 20
```

Esperado:

- O serviço está `Running`, com `StartType` `Automatic`, e o `nssm status` mostra `SERVICE_RUNNING`.
- O `/testar-todos` dá o mesmo resultado do passo 9.
- O log contém `Started Application` e `CLI desativado (sem terminal interativo)`, o que é normal.
- A **primeira** inicialização leva cerca de 7 s. As seguintes levam de 1,3 a 1,9 s.

**Teste os comandos de parar, iniciar e reiniciar:**

```powershell
nssm stop sensor-rfid;    Get-Service sensor-rfid   # Stopped
nssm start sensor-rfid;   Get-Service sensor-rfid   # Running
nssm restart sensor-rfid; Get-Service sensor-rfid   # Running
```

**Teste o reinício automático** (encerre o `java` à força e veja-o voltar):

```powershell
$pid1 = (Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object CommandLine -like '*SensorSerialYan*').ProcessId
Stop-Process -Id $pid1 -Force
Start-Sleep -Seconds 15
(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object CommandLine -like '*SensorSerialYan*').ProcessId
Get-Service sensor-rfid
```

Esperado: depois de cerca de 10 s, há um `java` novo (com PID diferente de `$pid1`), e o serviço continua `Running`. O `/testar-todos` volta a responder.

**Recomendado:** reinicie o Windows e confirme que o serviço sobe sozinho e que os adaptadores reconectam (repita o `/testar-todos`).

---

## 11. Energia

**Objetivo:** impedir que o servidor suspenda ou que o Windows desligue os adaptadores USB para economizar energia. Se isso acontecer, o sensor daquele adaptador passa a aparecer como `ERRO`.

**Como fazer:**

1. **Nunca suspender na tomada:**
   - **Windows 11:** *Configurações > Sistema > Energia* (ou *Energia e bateria*) > *Tela e suspensão* > "Colocar o dispositivo em suspensão após" = **Nunca** (na tomada).
   - **Windows 10:** *Configurações > Sistema > Energia e suspensão* > "Suspender: Quando conectado, o computador entra em suspensão após" = **Nunca**.

   Também dá para fazer pelo PowerShell como administrador:
   ```powershell
   powercfg /change standby-timeout-ac 0
   powercfg /change hibernate-timeout-ac 0
   ```
2. **Desativar a suspensão seletiva USB (na tomada):**
   ```powershell
   powercfg /setacvalueindex SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bebba308a3 48e6b7a6-50f5-4782-a5d4-53bb8f07e226 0
   powercfg /setactive SCHEME_CURRENT
   ```
3. **Impedir que o Windows desligue os adaptadores Wi-Fi:** `Win+X` > **Gerenciador de Dispositivos** > *Adaptadores de rede* > clique duas vezes em cada adaptador Wi-Fi > aba **Gerenciamento de Energia** > **desmarque** "O computador pode desligar o dispositivo para economizar energia" > **OK**. Repita para todos os adaptadores.
   Alternativa pelo PowerShell (alguns drivers não aceitam; nesse caso, use a interface):
   ```powershell
   Get-NetAdapter -Physical | Where-Object { $_.PhysicalMediaType -match '802.11' } |
     ForEach-Object { Set-NetAdapterPowerManagement -Name $_.Name -AllowComputerToTurnOffDevice Disabled }
   ```

**Como validar:**

```powershell
powercfg /query SCHEME_CURRENT SUB_SLEEP STANDBYIDLE
powercfg /query SCHEME_CURRENT 2a737441-1930-4402-8d77-b2bebba308a3 48e6b7a6-50f5-4782-a5d4-53bb8f07e226
Get-NetAdapter -Physical | Where-Object { $_.PhysicalMediaType -match '802.11' } | Get-NetAdapterPowerManagement | Format-Table Name, AllowComputerToTurnOffDevice
```

Esperado:

- No primeiro comando, o índice de **CA** (corrente alternada) está em `0x00000000`. O mesmo vale para o segundo.
- No terceiro, `AllowComputerToTurnOffDevice` aparece como `Disabled` em todos os adaptadores. Se o driver não informar esse valor, confira a caixa na aba *Gerenciamento de Energia*.

---

## 12. Firewall (porta 8080)

**Objetivo:** permitir que outras máquinas da rede chamem a API.

**Como fazer** (PowerShell como administrador; é o mesmo comando do README):

```powershell
New-NetFirewallRule -DisplayName "API RFID 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

Também dá para criar a regra pela interface:

1. `Win+R` > `wf.msc` > abre o **Windows Defender Firewall com Segurança Avançada**.
2. **Regras de Entrada** > **Nova Regra...**
3. **Porta** > *Avançar* > **TCP**, *Portas locais específicas*: `8080` > *Avançar*.
4. **Permitir a conexão** > *Avançar* > deixe marcados *Domínio*, *Particular* e *Público* > *Avançar*.
5. Nome: `API RFID 8080` > **Concluir**.

> Use sempre o nome `API RFID 8080`. Não crie regras duplicadas com outros nomes.

**Verifique se há regras que bloqueiam o `java.exe`.** Elas são criadas quando alguém clica em *Cancelar* no aviso "permitir o acesso do Java?" e vencem as regras de permissão:

```powershell
Get-NetFirewallApplicationFilter | Where-Object Program -like '*java.exe' | Get-NetFirewallRule | Where-Object Action -eq Block
```

Se o comando listar alguma regra, remova-a acrescentando `| Remove-NetFirewallRule` ao final. Para só desativá-la, use `| Disable-NetFirewallRule`. Pela interface, em *Regras de Entrada*, procure as regras do Java com o ícone de bloqueio e exclua ou desative cada uma.

**Como validar:**

```powershell
Get-NetFirewallRule -DisplayName 'API RFID 8080' | Format-Table DisplayName, Enabled, Direction, Action
```

Esperado: `True`, `Inbound`, `Allow`, e o comando de verificação de bloqueio não retorna nada.

Descubra o IP do servidor na rede da empresa (ignore os IPs `192.168.x` dos adaptadores dos sensores):

```powershell
Get-NetIPAddress -AddressFamily IPv4 | Format-Table InterfaceAlias, IPAddress
```

Em **outra máquina** da rede, chame a API:

```powershell
curl.exe http://<ip-do-servidor>:8080/sensor/testar-todos
```

Esperado: HTTP 200 com o mesmo resultado do passo 10.

---

## Operação do dia a dia

Rode tudo em **PowerShell como administrador**.

### Serviço

| Ação | Comando |
|---|---|
| Status | `nssm status sensor-rfid` ou `Get-Service sensor-rfid` |
| Iniciar | `nssm start sensor-rfid` |
| Parar | `nssm stop sensor-rfid` |
| Reiniciar | `nssm restart sensor-rfid` |
| Ver ou editar a configuração | `nssm edit sensor-rfid` (opções da JVM: aba *Application*, campo *Arguments*) |

O serviço **não** usa a variável `JAVA_OPTS`. Para mudar a memória, use `nssm edit sensor-rfid` e reinicie o serviço. Veja o [README, "Ajustar memória"](../README.md#ajustar-memória).

### Logs

```powershell
Get-Content C:\sensor-rfid\logs\servico.log -Tail 50 -Wait
```

A execução atual grava em `logs\servico.log`. No teste de 14/10/2026, o NSSM rotacionou o log a **cada** inicialização do serviço, mesmo com arquivos pequenos: o anterior é renomeado com data e hora (ex.: `logs\servico-20261014T200012.586.log`, `logs\servico-20261014T200017.533.log`, de inícios com segundos de diferença). Os arquivos antigos se acumulam e podem ser apagados manualmente.

### Alterar sensores e recompilar

O `sensors.json` fica dentro do jar, então salvar o arquivo não basta:

```powershell
code C:\sensor-rfid\src\main\resources\sensors.json   # edite e salve
nssm stop sensor-rfid
cd C:\sensor-rfid
.\mvnw.cmd clean package
nssm start sensor-rfid
curl.exe http://localhost:8080/sensor/testar-todos
```

Um sensor novo também precisa do **seu próprio adaptador Wi-Fi** (passo 8).

### Atualizar o código

```powershell
nssm stop sensor-rfid
cd C:\sensor-rfid
git pull --ff-only
.\mvnw.cmd clean package
nssm start sensor-rfid
```

> Se o `sensors.json` foi editado no servidor, o `git pull` pode reclamar de alterações locais. Nesse caso, faça commit delas ou guarde-as com `git stash` antes do pull (e `git stash pop` depois).

### Remover o serviço

Pare o serviço **antes** de removê-lo:

```powershell
nssm stop sensor-rfid
nssm remove sensor-rfid confirm
```

Se o `remove` for feito com o serviço rodando, o Windows só o **marca para exclusão**: ele aparece como *Desativado* e só some quando parar. Depois que ele sumir de `Get-Service sensor-rfid`, reinstale pelo passo 10.

---

## Solução de problemas

| Sintoma | Causa | Solução |
|---|---|---|
| `... não pode ser carregado porque a execução de scripts foi desabilitada neste sistema` | Política de execução do PowerShell. | Use `powershell -ExecutionPolicy Bypass -File <script>.ps1` ou `Set-ExecutionPolicy -Scope Process Bypass`. Se o arquivo veio de ZIP ou download, rode também `Unblock-File <arquivo>`. |
| `Invoke-WebRequest`: "Não foi possível criar um canal seguro SSL/TLS" | PowerShell 5.1 em um Windows 10 antigo, sem TLS 1.2. | Rode `[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12` na mesma janela, ou baixe pelo navegador. |
| `winget` "não é reconhecido" | Windows 10 sem o "Instalador de Aplicativo". | Use os links de download direto de cada passo. |
| Instalador MSI falha com código **1618** | Outra instalação está em andamento (ex.: Windows Update). | Espere a outra instalação terminar e tente de novo. |
| `java -version` mostra **1.8** | Um Java 8 da Oracle (`...\Oracle\Java\java8path` ou `javapath`) está antes no `PATH`. | Mova o `bin` do JDK 17 para o topo do `Path` do sistema (passo 3) e reabra o terminal. Se o serviço já foi instalado com o java errado, remova-o e reinstale. |
| `java`, `git`, `nssm` ou `code` "não é reconhecido" logo após instalar | O terminal aberto mantém o `PATH` antigo. | Feche e reabra o PowerShell ou o VS Code. |
| `nssm` aponta para `...\AppData\Local\Microsoft\WinGet\...` | Foi instalado com `winget install NSSM.NSSM`. | Siga o passo 4 (`C:\Program Files\nssm`). |
| `.\mvnw.cmd` reclama do `JAVA_HOME` | O `JAVA_HOME` está ausente ou aponta para outro Java. | Ajuste o `JAVA_HOME` da máquina para a pasta `jdk-17...` (passo 3) e reabra o terminal. |
| A compilação falha ao baixar dependências | Sem internet ou com proxy na primeira compilação. | Confira o acesso a `repo.maven.apache.org` (`Test-NetConnection repo.maven.apache.org -Port 443`). |
| `clean` falha com "arquivo em uso" | O serviço está rodando e bloqueia o jar. | `nssm stop sensor-rfid`, compile e depois `nssm start sensor-rfid`. |
| Git: `detected dubious ownership in repository at 'C:/sensor-rfid'` | A pasta foi criada pelo administrador e o git roda com outro usuário. | Como administrador: `git config --system --add safe.directory C:/sensor-rfid`. |
| `instalar-servico-windows.ps1`: "o serviço 'sensor-rfid' já existe" | O serviço já foi instalado antes. | `nssm stop sensor-rfid; nssm remove sensor-rfid confirm` e rode o instalador de novo. |
| `instalar-servico-windows.ps1`: "nssm não encontrado no PATH" | O NSSM não está no `PATH`, ou o terminal foi aberto antes de o `PATH` ser alterado. | Siga o passo 4 e rode o instalador em um PowerShell novo. Ignore a sugestão de `winget install NSSM.NSSM` da mensagem. |
| A API não sobe: `BindException` / porta **8080 em uso** | Um `executar-servidor.ps1` ainda aberto ou outro programa usando a 8080. | `Get-NetTCPConnection -LocalPort 8080 -State Listen \| ForEach-Object { Get-Process -Id $_.OwningProcess }`. Encerre o processo (ou dê Ctrl+C no terminal) e reinicie o serviço. |
| Serviço em **Paused** / `SERVICE_PAUSED` ou não inicia | O java saiu com erro (porta em uso, jar ausente, java errado). O NSSM tenta de novo a cada 10 s. | Leia o `C:\sensor-rfid\logs\servico.log` e confira o java, os argumentos e o diretório com `nssm edit sensor-rfid`. Corrija e rode `nssm restart sensor-rfid`. |
| Serviço como **Desativado** / "marcado para exclusão" | O `nssm remove` foi feito com o serviço rodando. | Rode `nssm stop sensor-rfid` (e feche o *services.msc*, se estiver aberto) e espere ele sumir de `Get-Service`. Depois reinstale. |
| Sensor com `"status": "ERRO"` / timeout | O adaptador não está no SSID do sensor, perdeu a conexão, foi desligado pela economia de energia, ou o sensor está desligado. | Rode `netsh wlan show interfaces`, reconecte com "Conectar automaticamente" (passo 8), aplique o passo 11 e teste com `Test-NetConnection <ip> -Port <porta>`. |
| Todos os sensores com `ERRO` | Nenhum adaptador conectado ou menos adaptadores que sensores. | Passo 8: é preciso um adaptador por sensor. |
| `netsh wlan show interfaces` não mostra o SSID | Windows 11 24H2 ou mais novo sem permissão de Localização. | *Configurações > Privacidade e segurança > Localização*: ative, inclusive para aplicativos da área de trabalho. |
| `Connection reset` / `ERRO` esporádico | Duas leituras simultâneas do **mesmo** sensor (ex.: duas chamadas a `/testar-todos` ao mesmo tempo). | Comportamento conhecido. Evite chamadas concorrentes e repita a chamada. |
| `/testar-todos` demora | É normal: cada leitura leva ~8 s (em paralelo), e um sensor inacessível espera o timeout de 5 s. | Espere de 8 a 13 s. No cliente, use um timeout de pelo menos 30 s. |
| Outras máquinas não acessam a API (no `localhost` funciona) | A regra de firewall está ausente ou há uma regra **bloqueando** o `java.exe`. | Passo 12: crie a `API RFID 8080` e remova as regras de bloqueio do `java.exe`. De outra máquina, teste com `Test-NetConnection <servidor> -Port 8080`. |

---

## Checklist final

- [ ] PowerShell como administrador (`IsInRole` → `True`) e internet funcionando
- [ ] VS Code instalado (`C:\Program Files\Microsoft VS Code\Code.exe`); `code --version` OK
- [ ] `git --version` OK
- [ ] `java -version` → 17 (Temurin); `where.exe java` com o JDK 17 na primeira linha; `JAVA_HOME` apontando para o `jdk-17...`
- [ ] `(Get-Command nssm).Source` → `C:\Program Files\nssm\nssm.exe`; `nssm version` com `2.24-101`
- [ ] Repositório em `C:\sensor-rfid` (`git log -1` OK; `safe.directory` configurado)
- [ ] `sensors.json` conferido (IP, porta e setor de cada sensor)
- [ ] `.\mvnw.cmd clean package` → `BUILD SUCCESS`; jar de ~20 MB em `target\`
- [ ] Um adaptador Wi-Fi USB por sensor, cada um no SSID do seu sensor, com "Conectar automaticamente"
- [ ] `Test-NetConnection <ip> -Port <porta> -InformationLevel Quiet` → `True` para todos os sensores
- [ ] Teste rápido: `/sensor/testar-todos` → HTTP 200, todos com `"status": "OK"`; RAM de ~105 a 112 MB
- [ ] Serviço `sensor-rfid`: `Running` e `Automatic`; parar, iniciar, reiniciar e o reinício automático (~10 s) testados
- [ ] `logs\servico.log` com `Started Application`
- [ ] Suspensão e hibernação desativadas na tomada, suspensão seletiva USB desativada e adaptadores sem economia de energia
- [ ] Regra `API RFID 8080` ativa e nenhuma regra bloqueando o `java.exe`
- [ ] `/sensor/testar-todos` funcionando a partir de outra máquina da rede
- [ ] Servidor reiniciado: o serviço subiu sozinho e os adaptadores reconectaram
