package com.pedidos360.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    private final ConcurrentHashMap<String, AtomicInteger> intentosPorOrden = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
    public void processOrder(OrderEvent event) {
        AtomicInteger contador = intentosPorOrden.computeIfAbsent(event.getOrderId(), k -> new AtomicInteger(0));
        int intentoActual = contador.incrementAndGet();

        log.info("----------------------------------------------------------------------");
        log.info("[📥 CONSUMER Pedidos360] Recibiendo orden #{} (Intento {} de 3)", event.getOrderId(), intentoActual);
        log.info("                         Cliente: {} | Total: ${}", event.getCliente(), event.getTotal());

        if (event.isSimularFallo()) {
            log.warn("[⚠️ FALLO SIMULADO] Error en orden #{}. Motivo: {}",
                    event.getOrderId(), event.getMotivoFallo());

            if (intentoActual >= 3) {
                log.error("[❌ LÍMITE DE REINTENTOS] Orden #{} rechazada. RabbitMQ la traslada a '{}' -> '{}'",
                        event.getOrderId(), RabbitMQConfig.DLX_EXCHANGE, RabbitMQConfig.DLQ_QUEUE);
                intentosPorOrden.remove(event.getOrderId());
            }

            throw new RuntimeException("Simulación de fallo controlada: " + event.getMotivoFallo());
        }

        log.info("[✅ ÉXITO] Orden #{} procesada correctamente (ACK)", event.getOrderId());
        log.info("----------------------------------------------------------------------");
        intentosPorOrden.remove(event.getOrderId());
    }
}
