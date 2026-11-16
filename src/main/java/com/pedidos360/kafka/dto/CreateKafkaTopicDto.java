package com.pedidos360.kafka.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la creacion dinamica de topicos Kafka desde la API REST.
 * Microservicio Administrador de Kafka.
 */
public class CreateKafkaTopicDto {

    @NotBlank(message = "El nombre del topico no puede estar vacio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del topico solo puede contener letras, numeros, puntos, guiones y guiones bajos")
    private String name;

    @Min(value = 1, message = "El numero de particiones debe ser al menos 1")
    private int partitions = 3;

    @Min(value = 1, message = "El factor de replicacion debe ser al menos 1")
    private short replicationFactor = 3;

    private Long retentionMs = 86400000L; // 24 horas por defecto

    private String cleanupPolicy = "delete"; // delete o compact

    public CreateKafkaTopicDto() {
    }

    public CreateKafkaTopicDto(String name, int partitions, short replicationFactor, Long retentionMs, String cleanupPolicy) {
        this.name = name;
        this.partitions = partitions > 0 ? partitions : 3;
        this.replicationFactor = replicationFactor > 0 ? replicationFactor : 3;
        this.retentionMs = retentionMs != null ? retentionMs : 86400000L;
        this.cleanupPolicy = cleanupPolicy != null ? cleanupPolicy : "delete";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(int partitions) {
        this.partitions = partitions;
    }

    public short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Long getRetentionMs() {
        return retentionMs;
    }

    public void setRetentionMs(Long retentionMs) {
        this.retentionMs = retentionMs;
    }

    public String getCleanupPolicy() {
        return cleanupPolicy;
    }

    public void setCleanupPolicy(String cleanupPolicy) {
        this.cleanupPolicy = cleanupPolicy;
    }
}
