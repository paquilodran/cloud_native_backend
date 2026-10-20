package com.pedidos360.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Productor de mensajes para los diferentes casos de uso de la solucion RabbitMQ:
 * - Direct: Ordenes y Notificaciones.
 * - Topic: Eventos de negocio (patron de routing key).
 */
@Service
public class OrderProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);
    private final RabbitTemplate rabbitTemplate;

    public OrderProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishOrder(OrderEvent event) {
        log.info("[PRODUCER] Publicando orden #{} a '{}' con routingKey '{}'",
                event.getOrderId(), RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ORDERS_ROUTING_KEY);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY,
                event
        );
    }

    public void publishNotification(NotificationEvent event) {
        log.info("[PRODUCER] Publicando notificacion #{} a '{}' con routingKey '{}'",
                event.getId(), RabbitMQConfig.NOTIFICATIONS_EXCHANGE, RabbitMQConfig.NOTIFICATIONS_ROUTING_KEY);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATIONS_EXCHANGE,
                RabbitMQConfig.NOTIFICATIONS_ROUTING_KEY,
                event
        );
    }

    public void publishTopicEvent(String routingKey, BusinessTopicEvent event) {
        log.info("[PRODUCER] Publicando evento Topic #{} a '{}' con routingKey '{}'",
                event.getEventId(), RabbitMQConfig.TOPIC_EXCHANGE, routingKey);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TOPIC_EXCHANGE,
                routingKey,
                event
        );
    }
}
