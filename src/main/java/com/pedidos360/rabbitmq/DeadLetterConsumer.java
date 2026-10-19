package com.pedidos360.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);
    private final List<DeadLetterEntry> records = new CopyOnWriteArrayList<>();

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void onDeadLetter(
            OrderEvent event,
            @Header(required = false, name = "x-death") List<Map<String, Object>> xDeath) {

        String razon = "rejected";
        Long reintentos = 1L;
        String colaOrigen = RabbitMQConfig.ORDERS_QUEUE;

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

        log.error("======================================================================");
        log.error("[AUDITORIA DLQ Pedidos360] Mensaje muerto capturado en '{}'", RabbitMQConfig.DLQ_QUEUE);
        log.error("                           Orden ID: {} | Cliente: {}", event.getOrderId(), event.getCliente());
        log.error("                           Causa x-death: '{}' | Reintentos: {} | Cola origen: '{}'",
                razon, reintentos, colaOrigen);
        log.error("======================================================================");

        DeadLetterEntry entry = new DeadLetterEntry(
                UUID.randomUUID().toString(),
                event,
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
            OrderEvent event,
            String razon,
            Long reintentos,
            String colaOrigen,
            String timestamp
    ) {}
}
