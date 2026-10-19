package com.pedidos360.rabbitmq;

import com.pedidos360.rabbitmq.dto.CreateBindingDto;
import com.pedidos360.rabbitmq.dto.CreateExchangeDto;
import com.pedidos360.rabbitmq.dto.CreateQueueDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Servicio de alto nivel encargado de la administracion de recursos AMQP en RabbitMQ.
 * Cumple con el indicador de evaluacion EP3 de encapsulamiento del RabbitAdmin.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);
    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    /**
     * Declara una nueva cola con parametros resilientes opcionales (TTL, DLX, max length).
     */
    public boolean declareQueue(CreateQueueDto dto) {
        log.info("[RABBIT-ADMIN] Creando cola '{}' (Durable: {}, TTL: {} ms, DLX: '{}')",
                dto.getName(), dto.isDurable(), dto.getMessageTtl(), dto.getDeadLetterExchange());

        QueueBuilder builder = dto.isDurable()
                ? QueueBuilder.durable(dto.getName())
                : QueueBuilder.nonDurable(dto.getName());

        if (dto.isAutoDelete()) {
            builder.autoDelete();
        }
        if (dto.isExclusive()) {
            builder.exclusive();
        }
        if (dto.getMessageTtl() != null && dto.getMessageTtl() > 0) {
            builder.withArgument("x-message-ttl", dto.getMessageTtl());
        }
        if (dto.getDeadLetterExchange() != null && !dto.getDeadLetterExchange().isBlank()) {
            builder.withArgument("x-dead-letter-exchange", dto.getDeadLetterExchange().trim());
        }
        if (dto.getDeadLetterRoutingKey() != null && !dto.getDeadLetterRoutingKey().isBlank()) {
            builder.withArgument("x-dead-letter-routing-key", dto.getDeadLetterRoutingKey().trim());
        }
        if (dto.getMaxLength() != null && dto.getMaxLength() > 0) {
            builder.withArgument("x-max-length", dto.getMaxLength());
        }

        Queue queue = builder.build();
        String declaredName = rabbitAdmin.declareQueue(queue);
        return declaredName != null;
    }

    /**
     * Elimina una cola existente.
     */
    public boolean deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) {
        log.warn("[RABBIT-ADMIN] Solicitando eliminacion de cola '{}' (Unused: {}, Empty: {})",
                queueName, ifUnused, ifEmpty);
        try {
            rabbitAdmin.deleteQueue(queueName, ifUnused, ifEmpty);
            return true;
        } catch (Exception e) {
            log.error("[RABBIT-ADMIN] Error eliminando cola '{}': {}", queueName, e.getMessage());
            return false;
        }
    }

    /**
     * Purga todos los mensajes pendientes de una cola sin eliminarla.
     */
    public boolean purgeQueue(String queueName) {
        log.info("[RABBIT-ADMIN] Purgando cola '{}'", queueName);
        try {
            rabbitAdmin.purgeQueue(queueName, false);
            return true;
        } catch (Exception e) {
            log.error("[RABBIT-ADMIN] Error purgando cola '{}': {}", queueName, e.getMessage());
            return false;
        }
    }

    /**
     * Declara un nuevo Exchange de tipo Direct, Fanout, Topic o Headers.
     */
    public void declareExchange(CreateExchangeDto dto) {
        String type = dto.getType().toLowerCase().trim();
        log.info("[RABBIT-ADMIN] Creando Exchange '{}' de tipo '{}'", dto.getName(), type);

        Exchange exchange = switch (type) {
            case "topic" -> new TopicExchange(dto.getName(), dto.isDurable(), dto.isAutoDelete());
            case "fanout" -> new FanoutExchange(dto.getName(), dto.isDurable(), dto.isAutoDelete());
            case "headers" -> new HeadersExchange(dto.getName(), dto.isDurable(), dto.isAutoDelete());
            default -> new DirectExchange(dto.getName(), dto.isDurable(), dto.isAutoDelete());
        };

        rabbitAdmin.declareExchange(exchange);
    }

    /**
     * Elimina un Exchange existente.
     */
    public boolean deleteExchange(String exchangeName) {
        log.warn("[RABBIT-ADMIN] Eliminando Exchange '{}'", exchangeName);
        return rabbitAdmin.deleteExchange(exchangeName);
    }

    /**
     * Vincula una cola a un Exchange con una Routing Key.
     */
    public void declareBinding(CreateBindingDto dto) {
        log.info("[RABBIT-ADMIN] Creando Binding: cola '{}' -> exchange '{}' con key '{}'",
                dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());

        Binding binding = new Binding(
                dto.getQueueName(),
                Binding.DestinationType.QUEUE,
                dto.getExchangeName(),
                dto.getRoutingKey(),
                null
        );
        rabbitAdmin.declareBinding(binding);
    }

    /**
     * Remueve un Binding.
     */
    public void removeBinding(CreateBindingDto dto) {
        log.info("[RABBIT-ADMIN] Removiendo Binding: cola '{}' -> exchange '{}' con key '{}'",
                dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());

        Binding binding = new Binding(
                dto.getQueueName(),
                Binding.DestinationType.QUEUE,
                dto.getExchangeName(),
                dto.getRoutingKey(),
                null
        );
        rabbitAdmin.removeBinding(binding);
    }

    /**
     * Obtiene el estado y estadisticas de una cola.
     */
    public Map<String, Object> getQueueInfo(String queueName) {
        QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
        Map<String, Object> result = new HashMap<>();
        result.put("queueName", queueName);

        if (info != null) {
            result.put("exists", true);
            result.put("messageCount", info.getMessageCount());
            result.put("consumerCount", info.getConsumerCount());
        } else {
            result.put("exists", false);
            result.put("messageCount", 0);
            result.put("consumerCount", 0);
        }

        Properties properties = rabbitAdmin.getQueueProperties(queueName);
        if (properties != null) {
            properties.forEach((k, v) -> result.put(String.valueOf(k), v));
        }

        return result;
    }
}
