package com.pedidos360.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion del Cliente Apache Kafka (Productores, Consumidores y Administracion).
 * Cumple con los requerimientos EP5:
 * - 3 Nodos Brokers en cluster
 * - 2 Topicos con 3 particiones y 3 replicas
 * - Parametros avanzados de retencion y cleanup policy
 * - Consumer Groups con balanceo y concurrencia
 * - Manejo robusto de errores con reintentos y deserializacion segura
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092,localhost:9093,localhost:9094}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.analytics:pedidos.analytics.stream}")
    private String analyticsTopic;

    @Value("${app.kafka.topics.audit:pedidos.audit.stream}")
    private String auditTopic;

    @Value("${app.kafka.partitions:3}")
    private int partitions;

    @Value("${app.kafka.replication-factor:3}")
    private short replicationFactor;

    // =========================================================================
    // 1. TOPICOS PRINCIPALES (EP5: 2 Topicos con 3 particiones y 3 replicas)
    // =========================================================================

    /**
     * Topico 1: Streaming Analitico de Pedidos (pedidos.analytics.stream)
     * - 3 Particiones (balanceo horizontal)
     * - Factor de Replicacion: 3 (tolerancia a caida de 2 brokers con min.insync.replicas=2)
     * - Retencion: 24 horas (86.400.000 ms)
     * - Cleanup Policy: delete
     */
    @Bean
    public NewTopic analyticsTopic() {
        log.info("[KAFKA-CONFIG] Declarando Topico Analitico '{}' (Particiones: {}, Replicas: {})",
                analyticsTopic, partitions, replicationFactor);
        return TopicBuilder.name(analyticsTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, "86400000") // 24 horas
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }

    /**
     * Topico 2: Streaming de Auditoria y Trazabilidad (pedidos.audit.stream)
     * - 3 Particiones
     * - Factor de Replicacion: 3
     * - Retencion: 7 dias (604.800.000 ms)
     * - Cleanup Policy: compact,delete (conserva el ultimo estado por clave + expiracion)
     */
    @Bean
    public NewTopic auditTopic() {
        log.info("[KAFKA-CONFIG] Declarando Topico de Auditoria '{}' (Particiones: {}, Replicas: {})",
                auditTopic, partitions, replicationFactor);
        return TopicBuilder.name(auditTopic)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 dias
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT + "," + TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }

    // =========================================================================
    // 2. KAFKA ADMIN CLIENT (Microservicio Administrador)
    // =========================================================================

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        return new KafkaAdmin(configs);
    }

    @Bean
    public AdminClient adminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        return AdminClient.create(configs);
    }

    // =========================================================================
    // 3. PRODUCTOR KAFKA CON SERIALIZACION DTO Y ACKS=ALL
    // =========================================================================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Resiliencia y Garantias de Entrega:
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // =========================================================================
    // 4. CONSUMIDOR KAFKA CON ERROR HANDLING Y DESERIALIZACION ROBUSTA
    // =========================================================================

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Balanceo: 3 hilos concurrentes para mapear directamente con las 3 particiones
        factory.setConcurrency(partitions);

        // Manejo de errores: 2 reintentos con intervalo de 1 seg y logeo contextual
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            log.error("[KAFKA-ERROR-HANDLER] Error fatal procesando mensaje en topico='{}', particion={}, offset={}. Causa: {}",
                    record.topic(), record.partition(), record.offset(), exception.getMessage());
        }, new FixedBackOff(1000L, 2L));

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
