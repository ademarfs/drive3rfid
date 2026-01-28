package org.Yan.infra.controller;

import org.Yan.infra.DTO.TagDto;
import org.Yan.infra.DTO.SensorDTO;
import org.Yan.service.ISensorService;
import org.Yan.service.SensorManagerService;
import org.Yan.service.SensorConfigService;
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
    private final SensorConfigService configService;

    public SensorController(ISensorService service, SensorManagerService sensorManager, SensorConfigService configService) {
        this.service = service;
        this.sensorManager = sensorManager;
        this.configService = configService;
    }

    // ========== Endpoints de Leitura de Tags ==========
    // Mantém apenas os endpoints usados: buscar todas as tags de um sensor e buscar uma tag específica

    @GetMapping("/tags/{ip}/{porta}")
    public ResponseEntity<List<TagDto>> getAllTagsBySensor(@PathVariable String ip, @PathVariable int porta,
                                                          @RequestParam(required = false) String ipLocal) {
        log.info("Buscando tags do sensor {}:{}", ip, porta);
        var tags = service.GetAll(ip, porta);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/tags/{ip}/{porta}/{tagId}")
    public ResponseEntity<TagDto> findTagById(@PathVariable String ip, @PathVariable int porta,
                                             @PathVariable String tagId,
                                             @RequestParam(required = false) String ipLocal) {
        log.info("Buscando tag {} no sensor {}:{}", tagId, ip, porta);
        var tag = service.GetById(tagId, ip, porta);
        return ResponseEntity.ok(tag);
    }

    @GetMapping("/testar-todos")
    public ResponseEntity<Object> testarTodosSensores() {
        log.info("Testando todos os sensores cadastrados simultaneamente");
        var sensores = configService.listarSensores();
        if (sensores.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhum sensor cadastrado. Use POST /sensor/cadastrar primeiro.");
        }
        var resultados = new java.util.HashMap<String, Object>();
        for (var sensor : sensores) {
            try {
                var tags = service.GetAll(sensor.getIp(), sensor.getPorta());
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
