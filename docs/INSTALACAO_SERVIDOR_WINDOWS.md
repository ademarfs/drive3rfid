# Instalação do servidor (Windows 10/11)

Use uma conta administrador; o projeto fica em `C:\sensor-rfid` e a API responde na porta 8080.

1. Instale o VS Code: baixe o **System Installer x64** em <https://code.visualstudio.com/download> e deixe marcado **"Adicionar ao PATH"**.

2. Instale o Java 17 (Eclipse Temurin, `.msi`): <https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64&package=jdk>. No instalador, ative **"Set JAVA_HOME variable"**.
   Garanta o `JAVA_HOME` (sem ele, a compilação falha), no PowerShell como administrador:
   ```powershell
   $jdk = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' | Select-Object -First 1).FullName
   [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdk, 'Machine')
   ```
   > Confira em um PowerShell novo com `java -version` (deve mostrar 17) e `$env:JAVA_HOME`; se `java -version` mostrar 1.8, mova o JDK 17 para o topo do `Path` do sistema.

3. Instale o Git: <https://git-scm.com/download/win> (opções padrão).

4. Instale o NSSM: baixe <https://nssm.cc/ci/nssm-2.24-101-g897c7ad.zip>, crie a pasta `C:\Program Files\nssm` e copie para ela **só o arquivo** `nssm.exe` da pasta `win64` do zip. Resultado: `C:\Program Files\nssm\nssm.exe` (não `...\nssm\win64\nssm.exe`). Depois adicione `C:\Program Files\nssm` ao `Path` **do sistema** (em "Variáveis do sistema", não nas do usuário).
   > Não use o winget para o NSSM.

5. Feche e abra o PowerShell **como administrador** (para carregar o novo `Path`). Confira (deve mostrar `C:\Program Files\nssm\nssm.exe`):
   ```powershell
   (Get-Command nssm).Source
   ```

6. Clone o repositório:
   ```powershell
   git clone https://github.com/ademarfs/drive3rfid.git C:\sensor-rfid
   git config --system --add safe.directory C:/sensor-rfid
   ```
   > A segunda linha evita o erro "dubious ownership" no VS Code.

7. Analise o `sensors.json`: abra o projeto com `code C:\sensor-rfid` e confira em `src\main\resources\sensors.json` o ip, a porta, o nome e o setor de cada sensor ([formato](../README.md#cadastro-de-sensores)).

8. Compile (gera `target\SensorSerialYan-1.0-SNAPSHOT.jar`):
   ```powershell
   cd C:\sensor-rfid
   .\mvnw.cmd clean package
   ```

9. Conecte cada sensor em um adaptador Wi-Fi USB (um adaptador por sensor): pela bandeja do Windows, escolha o adaptador, conecte na rede do sensor e marque **"Conectar automaticamente"**.

10. Teste a API:
    ```powershell
    powershell -ExecutionPolicy Bypass -File .\executar-servidor.ps1
    ```
    Em outro PowerShell (cada sensor deve aparecer com `"status": "OK"`):
    ```powershell
    curl.exe http://localhost:8080/sensor/testar-todos
    ```
    Depois, pare a API com **Ctrl+C** no primeiro PowerShell.
    > Se o Windows perguntar sobre o Java no firewall, clique em **Permitir**.

11. Instale e inicie o serviço; confira com `nssm status sensor-rfid` (deve mostrar `SERVICE_RUNNING`) ou `Get-Service sensor-rfid` (deve mostrar `Running`):
    ```powershell
    powershell -ExecutionPolicy Bypass -File .\deploy\instalar-servico-windows.ps1
    nssm start sensor-rfid
    nssm status sensor-rfid
    ```

12. Libere a porta 8080 no firewall:
    ```powershell
    New-NetFirewallRule -DisplayName "API RFID 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
    ```

13. (Recomendado) Desative a suspensão do Windows:
    ```powershell
    powercfg /change standby-timeout-ac 0
    ```

## Comandos úteis

- Serviço: `nssm status sensor-rfid`, `nssm start sensor-rfid`, `nssm stop sensor-rfid`, `nssm restart sensor-rfid`.
- Log: `C:\sensor-rfid\logs\servico.log`.
- Alterar sensores: edite o `sensors.json` → `nssm stop sensor-rfid` → `.\mvnw.cmd clean package` → `nssm start sensor-rfid`.
- Atualizar o código: rode `git pull` antes de compilar (mesma sequência acima).
- Remover o serviço: `nssm stop sensor-rfid` e depois `nssm remove sensor-rfid confirm`.

Detalhes da API (endpoints, respostas e erros): [README.md](../README.md).
