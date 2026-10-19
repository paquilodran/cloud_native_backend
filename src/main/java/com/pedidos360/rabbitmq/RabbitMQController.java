package com.pedidos360.rabbitmq;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * Enviar orden nominal a orders.exchange -> orders.queue
     */
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

    /**
     * Enviar orden con fallo simulado (3 reintentos antes de terminar en orders.dlq)
     */
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

    /**
     * Consultar mensajes muertos auditados en la DLQ
     */
    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterConsumer.DeadLetterEntry>> getDlqMessages() {
        return ResponseEntity.ok(deadLetterConsumer.getRecords());
    }

    /**
     * Limpiar historial de mensajes muertos
     */
    @DeleteMapping("/dlq")
    public ResponseEntity<Map<String, String>> clearDlq() {
        deadLetterConsumer.clear();
        return ResponseEntity.ok(Map.of("message", "Historial de DLQ vaciado"));
    }

    /**
     * Estado de la topologia
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTopologyStatus() {
        return ResponseEntity.ok(Map.of(
                "system", "Pedidos360 - Resilient AMQP Module",
                "status", "UP",
                "exchange", RabbitMQConfig.ORDERS_EXCHANGE + " (Direct)",
                "queue", RabbitMQConfig.ORDERS_QUEUE + " (TTL 30s)",
                "dlx", RabbitMQConfig.DLX_EXCHANGE + " (Fanout)",
                "dlq", RabbitMQConfig.DLQ_QUEUE + " (Retencion 24h)"
        ));
    }
}
