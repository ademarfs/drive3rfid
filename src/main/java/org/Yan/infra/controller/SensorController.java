package org.Yan.infra.controller;

import org.Yan.infra.DTO.SensorDTO;
import org.Yan.infra.DTO.TagDto;
import org.Yan.service.ISensorService;
import org.Yan.service.SensorManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sensor")
@CrossOrigin("*")
public class SensorController {
    private static final Logger log = LoggerFactory.getLogger(SensorController.class);
    private final ISensorService service;
    private final SensorManagerService sensorManager;

    public SensorController(ISensorService service, SensorManagerService sensorManager) {
        this.service = service;
        this.sensorManager = sensorManager;
    }

    // ========== Endpoints de Gerenciamento de Sensores ==========
    
    @PostMapping("/cadastrar")
    public ResponseEntity<SensorDTO> cadastrarSensor(@RequestBody SensorDTO sensor) {
        log.info("Cadastrando sensor: {}:{}", sensor.getIp(), sensor.getPorta());
        SensorDTO sensorCadastrado = sensorManager.adicionarSensor(sensor);
        return ResponseEntity.status(HttpStatus.CREATED).body(sensorCadastrado);
    }

    @GetMapping("/listar")
    public ResponseEntity<List<SensorDTO>> listarSensores() {
        log.info("Listando sensores cadastrados");
        return ResponseEntity.ok(sensorManager.listarSensores());
    }

    @DeleteMapping("/{ip}/{porta}")
    public ResponseEntity<Void> removerSensor(@PathVariable String ip, @PathVariable int porta) {
        log.info("Removendo sensor: {}:{}", ip, porta);
        if (sensorManager.removerSensor(ip, porta)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ========== Endpoints de Leitura de Tags ==========
    // ipLocal = IP desta máquina na interface que alcança o sensor (use quando tiver vários adaptadores)

    @GetMapping("/tags")
    public ResponseEntity<List<TagDto>> getAllTags(@RequestParam(required = false) String ip,
                                                   @RequestParam(required = false) Integer porta,
                                                   @RequestParam(required = false) String ipLocal) {
        if (ip != null && porta != null) {
            log.info("Buscando tags do sensor {}:{} ipLocal={}", ip, porta, ipLocal);
            var tags = service.GetAll(ip, porta, ipLocal);
            return ResponseEntity.ok(tags);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/tags/{ip}/{porta}")
    public ResponseEntity<List<TagDto>> getAllTagsBySensor(@PathVariable String ip, @PathVariable int porta,
                                                          @RequestParam(required = false) String ipLocal) {
        log.info("Buscando tags do sensor {}:{} ipLocal={}", ip, porta, ipLocal);
        var tags = service.GetAll(ip, porta, ipLocal);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/tags/{ip}/{porta}/{tagId}")
    public ResponseEntity<TagDto> findTagById(@PathVariable String ip, @PathVariable int porta,
                                             @PathVariable String tagId,
                                             @RequestParam(required = false) String ipLocal) {
        log.info("Buscando tag {} no sensor {}:{} ipLocal={}", tagId, ip, porta, ipLocal);
        var tag = service.GetById(tagId, ip, porta, ipLocal);
        return ResponseEntity.ok(tag);
    }

    @GetMapping("/testar-todos")
    public ResponseEntity<Object> testarTodosSensores() {
        log.info("Testando todos os sensores cadastrados simultaneamente");
        var sensores = sensorManager.listarSensores();
        if (sensores.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhum sensor cadastrado. Use POST /sensor/cadastrar primeiro.");
        }
        var resultados = new java.util.HashMap<String, Object>();
        for (var sensor : sensores) {
            try {
                var tags = service.GetAll(sensor.getIp(), sensor.getPorta(), sensor.getIpLocal());
                resultados.put(sensor.getIp() + ":" + sensor.getPorta(),
                    java.util.Map.of(
                        "status", "OK",
                        "tagsEncontradas", tags.size(),
                        "tags", tags
                    ));
            } catch (Exception e) {
                resultados.put(sensor.getIp() + ":" + sensor.getPorta(),
                    java.util.Map.of(
                        "status", "ERRO",
                        "mensagem", e.getMessage()
                    ));
            }
        }
        return ResponseEntity.ok(resultados);
    }
}
