package com.pedidos360.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.kafka.dto.OrderAuditStreamEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka para el Topico de Auditoria y Trazabilidad (tcg.audit.stream).
 * Consumer Group: tcg-audit-group (concurrencia 3 hilos para las 3 particiones).
 */
@Component
public class KafkaAuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAuditConsumer.class);

    private final KafkaStreamingAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public KafkaAuditConsumer(KafkaStreamingAnalyticsService analyticsService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.audit:tcg.audit.stream}",
            groupId = "tcg-audit-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditRecord(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        try {
            Object actual = payload;
            if (actual instanceof ConsumerRecord<?, ?> rec) {
                actual = rec.value();
            }

            OrderAuditStreamEvent event;
            if (actual instanceof OrderAuditStreamEvent) {
                event = (OrderAuditStreamEvent) actual;
            } else if (actual instanceof String str) {
                event = objectMapper.readValue(str, OrderAuditStreamEvent.class);
            } else if (actual instanceof byte[] bytes) {
                event = objectMapper.readValue(bytes, OrderAuditStreamEvent.class);
            } else {
                event = objectMapper.convertValue(actual, OrderAuditStreamEvent.class);
            }

            if (event.getOrderId() == null || event.getOrderId().isBlank()) {
                log.warn("[KAFKA-CONSUMER-AUDIT] Payload con orderId invalido en part {}, off {}. Descartando.", partition, offset);
                return;
            }

            log.info("[KAFKA-CONSUMER-AUDIT] Auditoria recibida: OrderId='{}', Accion='{}', Particion={}, Offset={}",
                    event.getOrderId(), event.getAccion(), partition, offset);

            analyticsService.processAuditEvent(event, partition, offset, key);

        } catch (Exception e) {
            log.error("[KAFKA-CONSUMER-AUDIT-ERROR] Error procesando auditoria en part {}, off {}: {}",
                    partition, offset, e.getMessage(), e);
            throw new RuntimeException("Error en consumidor de auditoria", e);
        }
    }
}
