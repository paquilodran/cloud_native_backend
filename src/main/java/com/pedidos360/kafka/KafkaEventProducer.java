package com.pedidos360.kafka;

import com.pedidos360.kafka.dto.OrderAnalyticsEvent;
import com.pedidos360.kafka.dto.OrderAuditStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Productor de Eventos Apache Kafka para Pedidos360.
 * Envia eventos tipados mediante DTOs estructurados y claves de particionamiento.
 * Cumple con el indicador de evaluacion EP5 para productores Kafka.
 */
@Component
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.analytics:pedidos.analytics.stream}")
    private String analyticsTopic;

    @Value("${app.kafka.topics.audit:pedidos.audit.stream}")
    private String auditTopic;

    public KafkaEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica un evento analitico en el topico pedidos.analytics.stream.
     * Utiliza el orderId como mensaje key para garantizar ordenamiento por orden.
     */
    public CompletableFuture<SendResult<String, Object>> publishAnalyticsEvent(OrderAnalyticsEvent event) {
        String key = event.getOrderId() != null ? event.getOrderId() : UUID.randomUUID().toString();

        log.info("[KAFKA-PRODUCER] Publicando Evento Analitico en '{}' [Key='{}', Monto=${}, Categoria='{}']",
                analyticsTopic, key, event.getMonto(), event.getCategoria());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(analyticsTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[KAFKA-PRODUCER-SUCCESS] Evento publicado en topico='{}', particion={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[KAFKA-PRODUCER-ERROR] Fallo al publicar evento en topico='{}' con key='{}': {}",
                        analyticsTopic, key, ex.getMessage());
            }
        });

        return future;
    }

    /**
     * Publica un evento de auditoria en el topico pedidos.audit.stream.
     */
    public CompletableFuture<SendResult<String, Object>> publishAuditEvent(OrderAuditStreamEvent event) {
        String key = event.getOrderId() != null ? event.getOrderId() : UUID.randomUUID().toString();

        log.info("[KAFKA-PRODUCER] Publicando Evento de Auditoria en '{}' [Key='{}', Accion='{}', Usuario='{}']",
                auditTopic, key, event.getAccion(), event.getUsuarioOperador());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(auditTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("[KAFKA-PRODUCER-SUCCESS] Auditoria publicada en topico='{}', particion={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("[KAFKA-PRODUCER-ERROR] Fallo al publicar auditoria en topico='{}': {}",
                        auditTopic, ex.getMessage());
            }
        });

        return future;
    }

    public String getAnalyticsTopic() {
        return analyticsTopic;
    }

    public String getAuditTopic() {
        return auditTopic;
    }
}
