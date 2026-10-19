package com.pedidos360.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);
    private final List<DeadLetterEntry> records = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = {
            RabbitMQConfig.DLQ_QUEUE,
            RabbitMQConfig.NOTIFICATIONS_DLQ,
            RabbitMQConfig.TOPIC_DLQ
    })
    public void onDeadLetter(
            Object rawMessage,
            @Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

        String razon = "rejected";
        Long reintentos = 1L;
        String colaOrigen = "orders.queue";

        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, Object> death = xDeath.get(0);
            razon = String.valueOf(death.getOrDefault("reason", "rejected"));
            Object count = death.get("count");
            if (count instanceof Number n) {
                reintentos = n.longValue();
            }
            Object q = death.get("queue");
            if (q != null) {
                colaOrigen = String.valueOf(q);
            }
        }

        String payloadText;
        if (rawMessage instanceof byte[] bytes) {
            payloadText = new String(bytes, StandardCharsets.UTF_8);
        } else if (rawMessage instanceof Message msg) {
            payloadText = new String(msg.getBody(), StandardCharsets.UTF_8);
        } else if (rawMessage != null) {
            payloadText = rawMessage.toString();
        } else {
            payloadText = "{}";
        }

        log.error("======================================================================");
        log.error("[AUDITORIA DLQ Pedidos360] Mensaje muerto capturado desde cola origen '{}'", colaOrigen);
        log.error("                           Causa x-death: '{}' | Reintentos: {}", razon, reintentos);
        log.error("                           Payload: {}", payloadText);
        log.error("======================================================================");

        DeadLetterEntry entry = new DeadLetterEntry(
                UUID.randomUUID().toString(),
                payloadText,
                razon,
                reintentos,
                colaOrigen,
                Instant.now().toString()
        );

        records.add(0, entry);
    }

    public List<DeadLetterEntry> getRecords() {
        return Collections.unmodifiableList(records);
    }

    public void clear() {
        records.clear();
    }

    public record DeadLetterEntry(
            String id,
            String payload,
            String razon,
            Long reintentos,
            String colaOrigen,
            String timestamp
    ) {}
}
