package com.pedidos360.kafka;

import com.pedidos360.kafka.dto.ConsumerGroupInfoDto;
import com.pedidos360.kafka.dto.CreateKafkaTopicDto;
import com.pedidos360.kafka.dto.KafkaTopicInfoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Microservicio Administrador de Apache Kafka.
 * Expone endpoints REST para administracion dinamica de topicos, configuraciones y grupos de consumidores.
 * Cumple con los indicadores de evaluacion EP5 (Microservicio Administrador de Kafka).
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/public/kafka/admin")
public class KafkaAdminController {

    private final KafkaAdminService adminService;

    public KafkaAdminController(KafkaAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> getClusterOverview() {
        return ResponseEntity.ok(adminService.getClusterOverview());
    }

    @GetMapping("/topics")
    public ResponseEntity<List<KafkaTopicInfoDto>> listTopics() {
        return ResponseEntity.ok(adminService.listTopics());
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<?> describeTopic(@PathVariable String name) {
        Optional<KafkaTopicInfoDto> topic = adminService.describeTopic(name);
        return topic.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body((KafkaTopicInfoDto) null));
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> createTopic(@Valid @RequestBody CreateKafkaTopicDto dto) {
        boolean created = adminService.createTopic(dto);
        if (created) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "CREATED",
                    "topic", dto.getName(),
                    "partitions", dto.getPartitions(),
                    "replicationFactor", dto.getReplicationFactor(),
                    "retentionMs", dto.getRetentionMs(),
                    "cleanupPolicy", dto.getCleanupPolicy(),
                    "message", "Topico creado satisfactoriamente en el cluster Kafka"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "No se pudo crear el topico. Verifique la conexion al cluster o si ya existe."
            ));
        }
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Map<String, Object>> deleteTopic(@PathVariable String name) {
        boolean deleted = adminService.deleteTopic(name);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "status", "DELETED",
                    "topic", name,
                    "message", "Topico eliminado satisfactoriamente"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "No se pudo eliminar el topico '" + name + "'"
            ));
        }
    }

    @PutMapping("/topics/{name}/config")
    public ResponseEntity<Map<String, Object>> updateTopicConfig(@PathVariable String name,
                                                                 @RequestBody Map<String, String> configs) {
        boolean updated = adminService.updateTopicConfig(name, configs);
        if (updated) {
            return ResponseEntity.ok(Map.of(
                    "status", "UPDATED",
                    "topic", name,
                    "configs", configs,
                    "message", "Configuraciones del topico actualizadas exitosamente"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "ERROR",
                    "message", "Error al actualizar las configuraciones del topico '" + name + "'"
            ));
        }
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<ConsumerGroupInfoDto>> listConsumerGroups() {
        return ResponseEntity.ok(adminService.listConsumerGroups());
    }
}
