package org.Yan.service;

import org.Yan.infra.DTO.SensorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SensorManagerService {
    private static final Logger log = LoggerFactory.getLogger(SensorManagerService.class);
    private final Map<String, SensorDTO> sensores = new ConcurrentHashMap<>();

    public SensorDTO adicionarSensor(SensorDTO sensor) {
        if (sensor.getIp() == null || sensor.getIp().trim().isEmpty()) {
            throw new IllegalArgumentException("IP do sensor não pode ser vazio");
        }
        if (sensor.getPorta() <= 0 || sensor.getPorta() > 65535) {
            throw new IllegalArgumentException("Porta do sensor deve estar entre 1 e 65535");
        }
        String key = gerarKey(sensor.getIp(), sensor.getPorta());
        log.info("Adicionando sensor: {} (total de sensores: {})", key, sensores.size() + 1);
        sensores.put(key, sensor);
        return sensor;
    }

    public List<SensorDTO> listarSensores() {
        return new ArrayList<>(sensores.values());
    }

    public SensorDTO obterSensor(String ip, int porta) {
        String key = gerarKey(ip, porta);
        return sensores.get(key);
    }

    public boolean removerSensor(String ip, int porta) {
        String key = gerarKey(ip, porta);
        return sensores.remove(key) != null;
    }

    public boolean existeSensor(String ip, int porta) {
        String key = gerarKey(ip, porta);
        return sensores.containsKey(key);
    }

    private String gerarKey(String ip, int porta) {
        return ip + ":" + porta;
    }
}
