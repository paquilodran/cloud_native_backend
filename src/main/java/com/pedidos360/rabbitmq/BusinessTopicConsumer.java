package com.pedidos360.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidor de la cola 'events.topic.queue' (Caso de uso 3 - Topic Exchange).
 * Escucha eventos bajo el patron de enrutamiento 'pedidos.*'.
 */
@Component
public class BusinessTopicConsumer {

    private static final Logger log = LoggerFactory.getLogger(BusinessTopicConsumer.class);
    private final ConcurrentHashMap<String, AtomicInteger> intentosPorEvento = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.TOPIC_QUEUE)
    public void processTopicEvent(BusinessTopicEvent event) {
        AtomicInteger contador = intentosPorEvento.computeIfAbsent(event.getEventId(), k -> new AtomicInteger(0));
        int intentoActual = contador.incrementAndGet();

        log.info("----------------------------------------------------------------------");
        log.info("[TOPIC-CONSUMER] Recibiendo evento #{} | Entidad: '{}' | Accion: '{}' (Intento {} de 3)",
                event.getEventId(), event.getEntidad(), event.getAccion(), intentoActual);
        log.info("                 Detalle: {}", event.getPayload());

        if (event.isSimularFallo()) {
            log.warn("[FALLO TOPIC EVENT] Error simulado en procesamiento del evento #{}", event.getEventId());

            if (intentoActual >= 3) {
                log.error("[DLQ TRANSFER] Evento Topic #{} transferido a '{}' -> '{}'",
                        event.getEventId(), RabbitMQConfig.TOPIC_DLX, RabbitMQConfig.TOPIC_DLQ);
                intentosPorEvento.remove(event.getEventId());
            }

            throw new RuntimeException("Fallo en procesamiento de evento topic: " + event.getEventId());
        }

        log.info("[EXITO] Evento Topic #{} procesado y confirmado (ACK)", event.getEventId());
        log.info("----------------------------------------------------------------------");
        intentosPorEvento.remove(event.getEventId());
    }
}
