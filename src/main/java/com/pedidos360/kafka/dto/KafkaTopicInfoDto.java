package com.pedidos360.kafka.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO con informacion detallada de un topico en el cluster Kafka.
 */
public class KafkaTopicInfoDto {

    private String name;
    private int partitionsCount;
    private int replicationFactor;
    private Long retentionMs;
    private String cleanupPolicy;
    private boolean internal;
    private List<PartitionDetailDto> partitions;
    private Map<String, String> configs;

    public KafkaTopicInfoDto() {
    }

    public KafkaTopicInfoDto(String name, int partitionsCount, int replicationFactor,
                             Long retentionMs, String cleanupPolicy, boolean internal,
                             List<PartitionDetailDto> partitions, Map<String, String> configs) {
        this.name = name;
        this.partitionsCount = partitionsCount;
        this.replicationFactor = replicationFactor;
        this.retentionMs = retentionMs;
        this.cleanupPolicy = cleanupPolicy;
        this.internal = internal;
        this.partitions = partitions;
        this.configs = configs;
    }

    public static class PartitionDetailDto {
        private int partition;
        private int leaderId;
        private List<Integer> replicas;
        private List<Integer> isr; // In-Sync Replicas

        public PartitionDetailDto() {
        }

        public PartitionDetailDto(int partition, int leaderId, List<Integer> replicas, List<Integer> isr) {
            this.partition = partition;
            this.leaderId = leaderId;
            this.replicas = replicas;
            this.isr = isr;
        }

        public int getPartition() {
            return partition;
        }

        public void setPartition(int partition) {
            this.partition = partition;
        }

        public int getLeaderId() {
            return leaderId;
        }

        public void setLeaderId(int leaderId) {
            this.leaderId = leaderId;
        }

        public List<Integer> getReplicas() {
            return replicas;
        }

        public void setReplicas(List<Integer> replicas) {
            this.replicas = replicas;
        }

        public List<Integer> getIsr() {
            return isr;
        }

        public void setIsr(List<Integer> isr) {
            this.isr = isr;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPartitionsCount() {
        return partitionsCount;
    }

    public void setPartitionsCount(int partitionsCount) {
        this.partitionsCount = partitionsCount;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
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

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public List<PartitionDetailDto> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<PartitionDetailDto> partitions) {
        this.partitions = partitions;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }
}
