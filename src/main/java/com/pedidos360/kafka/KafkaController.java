package com.pedidos360.kafka;

import com.pedidos360.kafka.dto.OrderAnalyticsEvent;
import com.pedidos360.kafka.dto.OrderAuditStreamEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador REST para publicacion y consulta de metricas de streaming en Apache Kafka.
 * Expuesto bajo /api/public/kafka para interactuar con el Frontend Angular.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/public/kafka")
public class KafkaController {

    private final KafkaEventProducer producer;
    private final KafkaStreamingAnalyticsService analyticsService;

    public KafkaController(KafkaEventProducer producer, KafkaStreamingAnalyticsService analyticsService) {
        this.producer = producer;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getKafkaStatus() {
        return ResponseEntity.ok(Map.of(
                "system", "Apache Kafka Streaming Platform",
                "status", "CONNECTED",
                "topics", List.of(
                        Map.of(
                                "name", producer.getAnalyticsTopic(),
                                "partitions", 3,
                                "replicas", 3,
                                "retention", "24h (86400000 ms)",
                                "cleanupPolicy", "delete",
                                "consumerGroup", "pedidos-analytics-group"
                        ),
                        Map.of(
                                "name", producer.getAuditTopic(),
                                "partitions", 3,
                                "replicas", 3,
                                "retention", "7d (604800000 ms)",
                                "cleanupPolicy", "compact,delete",
                                "consumerGroup", "pedidos-audit-group"
                        )
                ),
                "metrics", analyticsService.getStreamingMetrics()
        ));
    }

    @PostMapping("/publish/analytics")
    public ResponseEntity<Map<String, Object>> publishAnalytics(@RequestBody(required = false) OrderAnalyticsEvent request) {
        String orderId = (request != null && request.getOrderId() != null && !request.getOrderId().isBlank())
                ? request.getOrderId()
                : "ORD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String cliente = (request != null && request.getCliente() != null) ? request.getCliente() : "Cliente Streaming";
        String producto = (request != null && request.getProducto() != null) ? request.getProducto() : "Articulo Cloud";
        Double monto = (request != null && request.getMonto() != null && request.getMonto() > 0) ? request.getMonto() : 85000.0;
        String cat = (request != null && request.getCategoria() != null) ? request.getCategoria() : "TECNOLOGIA";
        String canal = (request != null && request.getCanalVenta() != null) ? request.getCanalVenta() : "WEB";
        String estado = (request != null && request.getEstado() != null) ? request.getEstado() : "PROCESADO";

        OrderAnalyticsEvent event = new OrderAnalyticsEvent(orderId, cliente, producto, monto, cat, canal, estado);
        producer.publishAnalyticsEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "PUBLISHED",
                "topic", producer.getAnalyticsTopic(),
                "event", event,
                "message", "Evento analitico emitido a Apache Kafka satisfactoriamente"
        ));
    }

    @PostMapping("/publish/audit")
    public ResponseEntity<Map<String, Object>> publishAudit(@RequestBody(required = false) OrderAuditStreamEvent request) {
        String orderId = (request != null && request.getOrderId() != null && !request.getOrderId().isBlank())
                ? request.getOrderId()
                : "ORD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String accion = (request != null && request.getAccion() != null) ? request.getAccion() : "CAMBIO_ESTADO";
        String anterior = (request != null && request.getEstadoAnterior() != null) ? request.getEstadoAnterior() : "CREADO";
        String nuevo = (request != null && request.getNuevoEstado() != null) ? request.getNuevoEstado() : "PAGADO";
        String usuario = (request != null && request.getUsuarioOperador() != null) ? request.getUsuarioOperador() : "operador.duoc";
        String detalles = (request != null && request.getDetalles() != null) ? request.getDetalles() : "Transicion automatica via workflow";

        OrderAuditStreamEvent event = new OrderAuditStreamEvent(orderId, accion, anterior, nuevo, usuario, detalles);
        producer.publishAuditEvent(event);

        return ResponseEntity.ok(Map.of(
                "status", "PUBLISHED",
                "topic", producer.getAuditTopic(),
                "auditEvent", event,
                "message", "Evento de auditoria emitido a Apache Kafka satisfactoriamente"
        ));
    }

    @PostMapping("/publish/batch-demo")
    public ResponseEntity<Map<String, Object>> publishBatchDemo(@RequestParam(defaultValue = "6") int count) {
        String[] categorias = {"TECNOLOGIA", "ALIMENTOS", "HOGAR", "LOGISTICA", "MODA"};
        String[] canales = {"WEB", "MOBILE", "API", "SUCURSAL"};
        String[] estados = {"PROCESADO", "PAGADO", "DESPACHADO"};
        Random random = new Random();

        List<OrderAnalyticsEvent> emitted = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String orderId = "ORD-" + (1000 + random.nextInt(9000));
            String cliente = "Cliente Batch " + (i + 1);
            String prod = "Item-" + (10 + random.nextInt(90));
            double monto = 20000.0 + (random.nextInt(150) * 1000.0);
            String cat = categorias[random.nextInt(categorias.length)];
            String canal = canales[random.nextInt(canales.length)];
            String estado = estados[random.nextInt(estados.length)];

            OrderAnalyticsEvent event = new OrderAnalyticsEvent(orderId, cliente, prod, monto, cat, canal, estado);
            producer.publishAnalyticsEvent(event);
            emitted.add(event);

            // Emitir tambien un evento de auditoria asociado
            OrderAuditStreamEvent audit = new OrderAuditStreamEvent(
                    orderId, "VENTA_STREAMING", "INICIAL", estado, "demo.load.generator", "Rafaga de prueba en lote"
            );
            producer.publishAuditEvent(audit);
        }

        return ResponseEntity.ok(Map.of(
                "status", "BATCH_PUBLISHED",
                "count", count,
                "topicAnalytics", producer.getAnalyticsTopic(),
                "topicAudit", producer.getAuditTopic(),
                "sampleEmitted", emitted
        ));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getStreamingMetrics() {
        return ResponseEntity.ok(analyticsService.getStreamingMetrics());
    }

    @GetMapping("/events/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentStreamEvents() {
        return ResponseEntity.ok(analyticsService.getRecentStreamEvents());
    }

    @GetMapping("/audit/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentAuditTrail() {
        return ResponseEntity.ok(analyticsService.getRecentAuditTrail());
    }

    @DeleteMapping("/metrics")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        analyticsService.resetMetrics();
        return ResponseEntity.ok(Map.of("message", "Metricas de streaming reiniciadas exitosamente"));
    }
}
