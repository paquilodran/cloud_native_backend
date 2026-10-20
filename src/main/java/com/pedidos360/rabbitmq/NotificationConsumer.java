package com.pedidos360.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumidor de la cola 'notifications.queue' (Caso de uso 2).
 * Gestiona confirmacion explicita, reintentos y envio hacia notifications.dlq.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final ConcurrentHashMap<String, AtomicInteger> intentosPorNotificacion = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void processNotification(NotificationEvent event) {
        AtomicInteger contador = intentosPorNotificacion.computeIfAbsent(event.getId(), k -> new AtomicInteger(0));
        int intentoActual = contador.incrementAndGet();

        log.info("----------------------------------------------------------------------");
        log.info("[NOTIFICATION-CONSUMER] Procesando notificacion #{} via [{}] (Intento {} de 3)",
                event.getId(), event.getTipoCanal(), intentoActual);
        log.info("                        Destinatario: {} | Mensaje: '{}'",
                event.getDestinatario(), event.getMensaje());

        if (event.isSimularFallo()) {
            log.warn("[FALLO NOTIFICACION] Simulando caida del proveedor SMS/Email para #{}", event.getId());

            if (intentoActual >= 3) {
                log.error("[DLQ TRANSFER] Agotados 3 reintentos para notificacion #{}. Derivando a '{}' -> '{}'",
                        event.getId(), RabbitMQConfig.NOTIFICATIONS_DLX, RabbitMQConfig.NOTIFICATIONS_DLQ);
                intentosPorNotificacion.remove(event.getId());
            }

            throw new RuntimeException("Fallo en proveedor de notificaciones para id: " + event.getId());
        }

        log.info("[EXITO] Notificacion #{} enviada correctamente y confirmada (ACK)", event.getId());
        log.info("----------------------------------------------------------------------");
        intentosPorNotificacion.remove(event.getId());
    }
}
