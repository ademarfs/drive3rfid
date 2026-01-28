# 🔍 Diagnóstico de Sensores

## ✅ Verificações Realizadas

1. **Não há código de cadastro automático** - Verificado todo o código fonte
2. **Todos os endpoints são dinâmicos** - Nenhum IP fixo nos controllers
3. **SensorManagerService está limpo** - Não há inicialização automática

## 🧪 Como Verificar o Problema

### 1. Verificar Sensores Cadastrados
```bash
curl http://localhost:8080/sensor/listar
```

**Resultado esperado:** Lista vazia `[]` se nenhum sensor foi cadastrado manualmente.

### 2. Verificar Logs do Servidor
Ao iniciar o servidor, verifique os logs. Não deve aparecer nenhuma mensagem de cadastro automático.

### 3. Cadastrar Sensores Manualmente
```bash
# Sensor 1 - Porta 8000
curl -X POST http://localhost:8080/sensor/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"ip":"192.168.2.1","porta":8000,"nome":"Sensor 1"}'

# Sensor 2 - Porta 9000 (ou outro IP)
curl -X POST http://localhost:8080/sensor/cadastrar \
  -H "Content-Type: application/json" \
  -d '{"ip":"192.168.2.1","porta":9000,"nome":"Sensor 2"}'
```

### 4. Verificar Novamente
```bash
curl http://localhost:8080/sensor/listar
```

Agora deve mostrar os 2 sensores cadastrados.

### 5. Testar Cada Sensor Individualmente
```bash
# Sensor porta 8000
curl http://localhost:8080/sensor/tags/192.168.2.1/8000

# Sensor porta 9000
curl http://localhost:8080/sensor/tags/192.168.2.1/9000
```

## ⚠️ Possíveis Causas do Problema

1. **Cache do navegador/cliente HTTP** - Limpe o cache ou use curl
2. **Endpoint antigo sendo usado** - Certifique-se de usar os endpoints corretos:
   - ✅ `/sensor/cadastrar` (POST)
   - ✅ `/sensor/listar` (GET)
   - ✅ `/sensor/tags/{ip}/{porta}` (GET)
3. **Sensores em memória** - Os sensores são armazenados em memória, então ao reiniciar o servidor, todos os sensores são perdidos

## 🔧 Solução

Se o problema persistir:

1. **Reinicie o servidor completamente**
2. **Verifique os logs** ao iniciar - não deve haver cadastro automático
3. **Cadastre os sensores manualmente** usando os endpoints
4. **Use os endpoints corretos** para ler tags de cada sensor

## 📝 Nota Importante

- **Não há cadastro automático** - Todos os sensores devem ser cadastrados manualmente via API
- **Sensores são em memória** - Ao reiniciar o servidor, todos os sensores são perdidos
- **Cada requisição é independente** - Não há estado compartilhado entre requisições
