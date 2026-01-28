package org.Yan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.Yan.infra.DTO.SensorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
public class SensorConfigService {
    private static final Logger log = LoggerFactory.getLogger(SensorConfigService.class);
    private List<SensorDTO> sensores = Collections.emptyList();

    public SensorConfigService() {
        String external = System.getenv("SENSORS_FILE");
        if (external != null && !external.isBlank()) {
            loadSensorsFromFileSystem(external);
            if (!sensores.isEmpty()) return;
        }
        loadSensorsFromClasspath();
    }

    private void loadSensorsFromClasspath() {
        try (InputStream is = new ClassPathResource("sensors.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            sensores = mapper.readValue(is, new TypeReference<List<SensorDTO>>(){});
            log.info("Loaded {} sensors from sensors.json", sensores.size());
        } catch (Exception e) {
            log.warn("Could not load sensors.json from classpath, continuing with empty list", e);
            sensores = Collections.emptyList();
        }
    }

    private void loadSensorsFromFileSystem(String path) {
        try (InputStream is = new java.io.FileInputStream(path)) {
            ObjectMapper mapper = new ObjectMapper();
            sensores = mapper.readValue(is, new TypeReference<List<SensorDTO>>(){});
            log.info("Loaded {} sensors from external file {}", sensores.size(), path);
        } catch (Exception e) {
            log.warn("Could not load sensors from file {}", path, e);
            sensores = Collections.emptyList();
        }
    }

    public List<SensorDTO> listarSensores() {
        return sensores;
    }
}
