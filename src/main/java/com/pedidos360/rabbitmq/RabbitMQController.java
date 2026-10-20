package com.pedidos360.rabbitmq;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/public/rabbitmq")
public class RabbitMQController {

    private final OrderProducer orderProducer;
    private final DeadLetterConsumer deadLetterConsumer;

    public RabbitMQController(OrderProducer orderProducer, DeadLetterConsumer deadLetterConsumer) {
        this.orderProducer = orderProducer;
        this.deadLetterConsumer = deadLetterConsumer;
    }

    // =========================================================================
    // CASO 1: ORDENES (DIRECT EXCHANGE)
    // =========================================================================
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody(required = false) OrderEvent request) {
        String id = (request != null && request.getOrderId() != null && !request.getOrderId().isBlank())
                ? request.getOrderId()
                : "ORD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String cliente = (request != null && request.getCliente() != null) ? request.getCliente() : "Cliente Pedidos360";
        String producto = (request != null && request.getProducto() != null) ? request.getProducto() : "Item General";
        double total = (request != null && request.getTotal() > 0) ? request.getTotal() : 150000.0;

        OrderEvent event = new OrderEvent(id, cliente, producto, total, false, null);
        orderProducer.publishOrder(event);

        return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED",
                "message", "Orden encolada exitosamente en 'orders.queue'",
                "order", event
        ));
    }

    @PostMapping("/orders/fail")
    public ResponseEntity<Map<String, Object>> createFailingOrder(@RequestBody(required = false) OrderEvent request) {
        String id = (request != null && request.getOrderId() != null && !request.getOrderId().isBlank())
                ? request.getOrderId()
                : "FAIL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String cliente = (request != null && request.getCliente() != null) ? request.getCliente() : "Cliente con Error";
        String producto = (request != null && request.getProducto() != null) ? request.getProducto() : "Transaccion Fallida";
        double total = (request != null && request.getTotal() > 0) ? request.getTotal() : 50000.0;
        String motivo = (request != null && request.getMotivoFallo() != null)
                ? request.getMotivoFallo()
                : "Fondos insuficientes o pasarela no disponible (Simulacion DLQ)";

        OrderEvent event = new OrderEvent(id, cliente, producto, total, true, motivo);
        orderProducer.publishOrder(event);

        return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED_FAILING",
                "message", "Orden con error encolada. Reintentara 3 veces antes de pasar a 'orders.dlq'.",
                "order", event
        ));
    }

    // =========================================================================
    // CASO 2: NOTIFICACIONES (DIRECT EXCHANGE)
    // =========================================================================
    @PostMapping("/notifications")
    public ResponseEntity<Map<String, Object>> createNotification(@RequestBody(required = false) NotificationEvent request) {
        String id = (request != null && request.getId() != null && !request.getId().isBlank())
                ? request.getId()
                : "NOTIF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String dest = (request != null && request.getDestinatario() != null) ? request.getDestinatario() : "cliente@empresa.cl";
        String msg = (request != null && request.getMensaje() != null) ? request.getMensaje() : "Su pedido ha sido despachado exitosamente.";
        String canal = (request != null && request.getTipoCanal() != null) ? request.getTipoCanal() : "EMAIL";

        NotificationEvent event = new NotificationEvent(id, dest, msg, canal, false);
        orderProducer.publishNotification(event);

        return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED",
                "message", "Notificacion encolada en 'notifications.queue'",
                "notification", event
        ));
    }

    @PostMapping("/notifications/fail")
    public ResponseEntity<Map<String, Object>> createFailingNotification(@RequestBody(required = false) NotificationEvent request) {
        String id = (request != null && request.getId() != null && !request.getId().isBlank())
                ? request.getId()
                : "NOTIF-FAIL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String dest = (request != null && request.getDestinatario() != null) ? request.getDestinatario() : "+56911223344";
        String msg = (request != null && request.getMensaje() != null) ? request.getMensaje() : "Alerta de pago fallido.";
        String canal = (request != null && request.getTipoCanal() != null) ? request.getTipoCanal() : "SMS";

        NotificationEvent event = new NotificationEvent(id, dest, msg, canal, true);
        orderProducer.publishNotification(event);

        return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED_FAILING",
                "message", "Notificacion con fallo encolada. Reintentara y pasara a 'notifications.dlq'.",
                "notification", event
        ));
    }

    // =========================================================================
    // CASO 3: EVENTOS TOPIC (TOPIC EXCHANGE)
    // =========================================================================
    @PostMapping("/topic-events")
    public ResponseEntity<Map<String, Object>> createTopicEvent(
            @RequestParam(name = "routingKey", defaultValue = "pedidos.creado") String routingKey,
            @RequestBody(required = false) BusinessTopicEvent request) {

        String id = (request != null && request.getEventId() != null && !request.getEventId().isBlank())
                ? request.getEventId()
                : "EVT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String entidad = (request != null && request.getEntidad() != null) ? request.getEntidad() : "PEDIDO";
        String accion = (request != null && request.getAccion() != null) ? request.getAccion() : "CREADO";
        String payload = (request != null && request.getPayload() != null) ? request.getPayload() : "{\"total\": 120000, \"items\": 3}";

        BusinessTopicEvent event = new BusinessTopicEvent(id, entidad, accion, payload, false);
        orderProducer.publishTopicEvent(routingKey, event);

        return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED",
                "exchange", RabbitMQConfig.TOPIC_EXCHANGE,
                "routingKey", routingKey,
                "event", event
        ));
    }

    @PostMapping("/topic-events/fail")
    public ResponseEntity<Map<String, Object>> createFailingTopicEvent(
            @RequestParam(name = "routingKey", defaultValue = "pedidos.critico") String routingKey,
            @RequestBody(required = false) BusinessTopicEvent request) {

        String id = (request != null && request.getEventId() != null && !request.getEventId().isBlank())
                ? request.getEventId()
                : "EVT-FAIL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String entidad = (request != null && request.getEntidad() != null) ? request.getEntidad() : "PEDIDO";
        String accion = (request != null && request.getAccion() != null) ? request.getAccion() : "ERROR_CRITICO";
        String payload = (request != null && request.getPayload() != null) ? request.getPayload() : "{\"error\": \"Inconsistencia contable\"}";

        BusinessTopicEvent event = new BusinessTopicEvent(id, entidad, accion, payload, true);
        orderProducer.publishTopicEvent(routingKey, event);

        return ResponseEntity.ok(Map.of(
                "status", "ENQUEUED_FAILING",
                "exchange", RabbitMQConfig.TOPIC_EXCHANGE,
                "routingKey", routingKey,
                "event", event
        ));
    }

    // =========================================================================
    // AUDITORIA Y CONTROL DE DEAD LETTER QUEUE (DLQ)
    // =========================================================================
    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterConsumer.DeadLetterEntry>> getDlqMessages() {
        return ResponseEntity.ok(deadLetterConsumer.getRecords());
    }

    @DeleteMapping("/dlq")
    public ResponseEntity<Map<String, String>> clearDlq() {
        deadLetterConsumer.clear();
        return ResponseEntity.ok(Map.of("message", "Historial de DLQ vaciado"));
    }

    // =========================================================================
    // ESTADO DE LA TOPOLOGIA COMPLETA (3 COLAS, 3 DLQS, DIRECT Y TOPIC)
    // =========================================================================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTopologyStatus() {
        return ResponseEntity.ok(Map.of(
                "system", "Pedidos360 - Resilient AMQP Module (EP3 / EP4)",
                "status", "UP",
                "exchanges", List.of(
                        Map.of("name", RabbitMQConfig.ORDERS_EXCHANGE, "type", "Direct"),
                        Map.of("name", RabbitMQConfig.NOTIFICATIONS_EXCHANGE, "type", "Direct"),
                        Map.of("name", RabbitMQConfig.TOPIC_EXCHANGE, "type", "Topic (pedidos.*)"),
                        Map.of("name", RabbitMQConfig.DLX_EXCHANGE, "type", "Fanout (DLX)"),
                        Map.of("name", RabbitMQConfig.NOTIFICATIONS_DLX, "type", "Fanout (DLX)"),
                        Map.of("name", RabbitMQConfig.TOPIC_DLX, "type", "Fanout (DLX)")
                ),
                "queues", List.of(
                        Map.of("name", RabbitMQConfig.ORDERS_QUEUE, "ttl", "30s", "dlq", RabbitMQConfig.DLQ_QUEUE),
                        Map.of("name", RabbitMQConfig.NOTIFICATIONS_QUEUE, "ttl", "45s", "dlq", RabbitMQConfig.NOTIFICATIONS_DLQ),
                        Map.of("name", RabbitMQConfig.TOPIC_QUEUE, "ttl", "60s", "dlq", RabbitMQConfig.TOPIC_DLQ)
                )
        ));
    }
}
