# SensorSerialYan — Notas rápidas

Resumo das alterações recentes:
- `sensors.json` em `src/main/resources` agora contém a lista fixa de sensores (até 4).
- `SensorConfigService` carrega `sensors.json` na inicialização e é usado por `GET /sensor/testar-todos`.

Arquivo de configuração
- Local: `src/main/resources/sensors.json`
- Formato: array JSON com objetos `{ "ip": "<IP>", "porta": <PORT>, "nome": "<NOME>" }`.
- Exemplo:

```
[
  { "ip": "192.168.2.1", "porta": 8000, "nome": "sensor-1" },
  { "ip": "192.168.20.1", "porta": 9000, "nome": "sensor-2" }
]
```

Endpoints principais
- `GET /sensor/tags/{ip}/{porta}` — retorna todas as tags do sensor especificado.
- `GET /sensor/tags/{ip}/{porta}/{tagId}` — retorna a tag específica.
- `GET /sensor/testar-todos` — contacta todos os sensores listados em `sensors.json` e retorna um resumo por sensor.

Exemplos de uso (assumindo `localhost:8080`):

```bash
curl http://localhost:8080/sensor/testar-todos

curl http://localhost:8080/sensor/tags/192.168.2.1/8000

curl http://localhost:8080/sensor/tags/192.168.2.1/8000/000ABC123
```

Como editar sensores
- Edite `src/main/resources/sensors.json` com os IPs/portas corretos e reinicie a aplicação.

Build e execução
- Build: `./mvnw -DskipTests package`
- Executar: `java -jar target/SensorSerialYan-1.0-SNAPSHOT.jar` ou `./mvnw spring-boot:run`

Notas
- O campo `ipLocal` foi removido do `sensors.json` — as chamadas do serviço usam apenas IP e porta.
- Não altere endpoints existentes — a intenção foi enxugar apenas o que não é utilizado.
