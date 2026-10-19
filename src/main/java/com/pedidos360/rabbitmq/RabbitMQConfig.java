package com.pedidos360.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia de mensajeria asincrona resiliente de RabbitMQ (Duoc UC EP3 y EP4):
 * - 3 Colas de mensajes principales con sus 3 respectivas Dead Letter Queues (DLQ).
 * - Exchanges de tipo Direct, Fanout y Topic (enrutamiento avanzado por patron).
 * - Beans administradores dinamicos (RabbitAdmin).
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // CASO 1: PEDIDOS (DIRECT EXCHANGE)
    // =========================================================================
    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String ORDERS_QUEUE = "orders.queue";
    public static final String ORDERS_ROUTING_KEY = "order.created";

    public static final String DLX_EXCHANGE = "orders.dlx";
    public static final String DLQ_QUEUE = "orders.dlq";
    public static final String DLX_ROUTING_KEY = "order.dead";

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(ORDERS_EXCHANGE, true, false);
    }

    @Bean
    public Queue ordersQueue() {
        return QueueBuilder.durable(ORDERS_QUEUE)
                .withArgument("x-message-ttl", 30000)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
                .withArgument("x-max-length", 1000)
                .build();
    }

    @Bean
    public Binding ordersBinding(Queue ordersQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(ordersQueue)
                .to(ordersExchange)
                .with(ORDERS_ROUTING_KEY);
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, FanoutExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange);
    }

    // =========================================================================
    // CASO 2: NOTIFICACIONES (DIRECT EXCHANGE)
    // =========================================================================
    public static final String NOTIFICATIONS_EXCHANGE = "notifications.exchange";
    public static final String NOTIFICATIONS_QUEUE = "notifications.queue";
    public static final String NOTIFICATIONS_ROUTING_KEY = "notification.send";

    public static final String NOTIFICATIONS_DLX = "notifications.dlx";
    public static final String NOTIFICATIONS_DLQ = "notifications.dlq";
    public static final String NOTIFICATIONS_DEAD_KEY = "notification.dead";

    @Bean
    public DirectExchange notificationsExchange() {
        return new DirectExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .withArgument("x-message-ttl", 45000)
                .withArgument("x-dead-letter-exchange", NOTIFICATIONS_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATIONS_DEAD_KEY)
                .withArgument("x-max-length", 1000)
                .build();
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, DirectExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(notificationsExchange)
                .with(NOTIFICATIONS_ROUTING_KEY);
    }

    @Bean
    public FanoutExchange notificationsDlx() {
        return new FanoutExchange(NOTIFICATIONS_DLX, true, false);
    }

    @Bean
    public Queue notificationsDlq() {
        return QueueBuilder.durable(NOTIFICATIONS_DLQ)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding notificationsDlqBinding(Queue notificationsDlq, FanoutExchange notificationsDlx) {
        return BindingBuilder.bind(notificationsDlq)
                .to(notificationsDlx);
    }

    // =========================================================================
    // CASO 3: EVENTOS DE NEGOCIO Y AUDITORIA (TOPIC EXCHANGE)
    // =========================================================================
    public static final String TOPIC_EXCHANGE = "events.topic.exchange";
    public static final String TOPIC_QUEUE = "events.topic.queue";
    public static final String TOPIC_ROUTING_PATTERN = "pedidos.*";

    public static final String TOPIC_DLX = "events.dlx";
    public static final String TOPIC_DLQ = "events.dlq";
    public static final String TOPIC_DEAD_KEY = "events.dead";

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue topicQueue() {
        return QueueBuilder.durable(TOPIC_QUEUE)
                .withArgument("x-message-ttl", 60000)
                .withArgument("x-dead-letter-exchange", TOPIC_DLX)
                .withArgument("x-dead-letter-routing-key", TOPIC_DEAD_KEY)
                .withArgument("x-max-length", 1000)
                .build();
    }

    @Bean
    public Binding topicBinding(Queue topicQueue, TopicExchange topicExchange) {
        return BindingBuilder.bind(topicQueue)
                .to(topicExchange)
                .with(TOPIC_ROUTING_PATTERN);
    }

    @Bean
    public FanoutExchange topicDlx() {
        return new FanoutExchange(TOPIC_DLX, true, false);
    }

    @Bean
    public Queue topicDlq() {
        return QueueBuilder.durable(TOPIC_DLQ)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Binding topicDlqBinding(Queue topicDlq, FanoutExchange topicDlx) {
        return BindingBuilder.bind(topicDlq)
                .to(topicDlx);
    }

    // =========================================================================
    // COMPONENTES CORE: RABBIT ADMIN, SERIALIZADOR Y TEMPLATE
    // =========================================================================
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
