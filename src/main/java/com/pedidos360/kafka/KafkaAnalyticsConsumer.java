package com.pedidos360.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.kafka.dto.OrderAnalyticsEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor Kafka para el Topico de Analitica en Tiempo Real (tcg.analytics.stream).
 * Consumer Group: tcg-analytics-group (concurrencia 3 hilos para las 3 particiones).
 * Mantiene la logica de procesamiento separada de la infraestructura (EP5).
 */
@Component
public class KafkaAnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaAnalyticsConsumer.class);

    private final KafkaStreamingAnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    public KafkaAnalyticsConsumer(KafkaStreamingAnalyticsService analyticsService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.analytics:tcg.analytics.stream}",
            groupId = "tcg-analytics-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAnalyticsRecord(
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

            OrderAnalyticsEvent event;
            if (actual instanceof OrderAnalyticsEvent) {
                event = (OrderAnalyticsEvent) actual;
            } else if (actual instanceof String str) {
                event = objectMapper.readValue(str, OrderAnalyticsEvent.class);
            } else if (actual instanceof byte[] bytes) {
                event = objectMapper.readValue(bytes, OrderAnalyticsEvent.class);
            } else {
                event = objectMapper.convertValue(actual, OrderAnalyticsEvent.class);
            }

            // Validacion basica de payload
            if (event.getOrderId() == null || event.getOrderId().isBlank()) {
                log.warn("[KAFKA-CONSUMER-ANALYTICS] Payload con orderId invalido en part {}, off {}. Descartando.", partition, offset);
                return;
            }

            log.info("[KAFKA-CONSUMER-ANALYTICS] Evento recibido: OrderId='{}', Monto={}, Particion={}, Offset={}",
                    event.getOrderId(), event.getMonto(), partition, offset);

            // Delegacion de logica de negocio al servicio desacoplado
            analyticsService.processAnalyticsEvent(event, partition, offset, key);

        } catch (Exception e) {
            log.error("[KAFKA-CONSUMER-ANALYTICS-ERROR] Error procesando registro en particion {}, offset {}: {}",
                    partition, offset, e.getMessage(), e);
            throw new RuntimeException("Error en consumidor de analitica", e);
        }
    }
}
