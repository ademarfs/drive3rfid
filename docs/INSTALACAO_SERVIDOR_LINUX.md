# Instalação do servidor (Linux Ubuntu/Debian)

Rode os comandos no terminal com o seu usuário normal (não como root), usando `sudo` só onde aparece; a API responde na porta 8080.

> **Pasta provisória:** este tutorial usa `~/Downloads/rfid/drive3rfid`. Se o projeto estiver em outra pasta, troque o caminho em todos os comandos.

1. Instale o VS Code (pule se já estiver instalado): baixe o `.deb` em <https://code.visualstudio.com/download> e, na pasta do download:
   ```bash
   sudo apt install ./code_*.deb
   ```

2. Instale o Java 17 e o Git:
   ```bash
   sudo apt update && sudo apt install -y openjdk-17-jdk git
   ```
   > Confira com `java -version` (deve mostrar 17).

3. Baixe o projeto. Se a pasta ainda não existir:
   ```bash
   mkdir -p ~/Downloads/rfid && git clone https://github.com/ademarfs/drive3rfid.git ~/Downloads/rfid/drive3rfid
   ```
   Se já existir, atualize:
   ```bash
   cd ~/Downloads/rfid/drive3rfid && git pull
   ```

4. Analise o `sensors.json`: abra o projeto com `code ~/Downloads/rfid/drive3rfid` e confira em `src/main/resources/sensors.json` o ip, a porta, o nome e o setor de cada sensor ([formato](../README.md#cadastro-de-sensores)).

5. Compile (gera `target/SensorSerialYan-1.0-SNAPSHOT.jar`):
   ```bash
   cd ~/Downloads/rfid/drive3rfid
   ./mvnw clean package
   ```

6. Conecte cada sensor em um adaptador Wi-Fi USB (um adaptador por sensor): pelo ícone de rede, escolha o adaptador e conecte na rede do sensor. Nas configurações dessa rede, marque **"Conectar automaticamente"** e **"Disponível para todos os usuários"**.
   > Sem "Disponível para todos os usuários", o Wi-Fi só conecta depois do login; com ela, conecta já no boot e o serviço alcança o sensor sem ninguém logado.

7. Teste a API:
   ```bash
   cd ~/Downloads/rfid/drive3rfid
   ./executar-servidor.sh
   ```
   Em outro terminal (cada sensor deve aparecer com `"status": "OK"`):
   ```bash
   curl http://localhost:8080/sensor/testar-todos
   ```
   Depois, pare a API com **Ctrl+C** no primeiro terminal.
   > Se a API já estiver rodando pelo VS Code ou por outro terminal, pare-a antes (senão a porta 8080 fica ocupada).

8. Instale e inicie o serviço (o `sed` ajusta o usuário e a pasta da unidade de exemplo):
   ```bash
   cd ~/Downloads/rfid/drive3rfid
   sudo cp deploy/sensor-rfid.service /etc/systemd/system/
   sudo sed -i -e "s|^User=.*|User=$USER|" -e "s|/home/user/|$HOME/|g" /etc/systemd/system/sensor-rfid.service
   sudo systemctl daemon-reload
   sudo systemctl enable --now sensor-rfid
   systemctl status sensor-rfid
   ```
   > O status deve mostrar `active (running)` (saia com `q`).

9. Libere a porta 8080 no firewall (só se o `ufw` estiver ativo):
   ```bash
   sudo ufw allow 8080/tcp
   ```

10. (Recomendado) Desative a suspensão:
    ```bash
    sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target
    ```

11. Reinicie a máquina e, sem abrir o VS Code, confira:
    ```bash
    curl http://localhost:8080/sensor/testar-todos
    ```

## Comandos úteis

- Serviço: `systemctl status sensor-rfid`, `sudo systemctl start sensor-rfid`, `sudo systemctl stop sensor-rfid`, `sudo systemctl restart sensor-rfid`.
- Log (ao vivo): `journalctl -u sensor-rfid -f`.
- Alterar sensores: edite o `sensors.json` → `sudo systemctl stop sensor-rfid` → `./mvnw clean package` → `sudo systemctl start sensor-rfid`.
- Atualizar o código: rode `git pull` antes de compilar (mesma sequência acima).
- Remover o serviço: `sudo systemctl disable --now sensor-rfid`, depois `sudo rm /etc/systemd/system/sensor-rfid.service` e `sudo systemctl daemon-reload`.

Detalhes da API (endpoints, respostas e erros): [README.md](../README.md).
