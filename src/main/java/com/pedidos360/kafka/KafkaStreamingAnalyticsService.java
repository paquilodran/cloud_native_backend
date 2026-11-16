package com.pedidos360.kafka;

import com.pedidos360.kafka.dto.OrderAnalyticsEvent;
import com.pedidos360.kafka.dto.OrderAuditStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de Negocio para Analitica y Procesamiento en Tiempo Real de Eventos Streaming.
 * Mantiene la logica de procesamiento separada de la infraestructura de mensajeria (EP5).
 */
@Service
public class KafkaStreamingAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(KafkaStreamingAnalyticsService.class);

    // Metricas globales en tiempo real
    private final AtomicLong totalEventsConsumed = new AtomicLong(0);
    private final AtomicLong totalAnalyticsEvents = new AtomicLong(0);
    private final AtomicLong totalAuditEvents = new AtomicLong(0);
    private final AtomicLong totalRevenueStreamed = new AtomicLong(0);

    // Distribucion por particion (demuestra balanceo de carga en 3 particiones)
    private final Map<Integer, AtomicLong> partitionCounter = new ConcurrentHashMap<>();

    // Agregacion de volumen por categoria
    private final Map<String, Double> revenueByCategory = new ConcurrentHashMap<>();
    private final Map<String, Long> countByCategory = new ConcurrentHashMap<>();

    // Historial circular en memoria de eventos recientes para el Frontend
    private final List<Map<String, Object>> recentStreamEvents = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> recentAuditTrail = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 50;

    public KafkaStreamingAnalyticsService() {
        partitionCounter.put(0, new AtomicLong(0));
        partitionCounter.put(1, new AtomicLong(0));
        partitionCounter.put(2, new AtomicLong(0));
    }

    /**
     * Procesa un evento analitico entrante desde el consumidor Kafka.
     */
    public void processAnalyticsEvent(OrderAnalyticsEvent event, int partition, long offset, String key) {
        log.info("[STREAM-PROCESSING] Procesando Analitica Pedido '{}' - Particion: {}, Offset: {}, Monto: ${}",
                event.getOrderId(), partition, offset, event.getMonto());

        totalEventsConsumed.incrementAndGet();
        totalAnalyticsEvents.incrementAndGet();

        if (event.getMonto() != null) {
            totalRevenueStreamed.addAndGet(event.getMonto().longValue());
            String cat = (event.getCategoria() != null && !event.getCategoria().isBlank())
                    ? event.getCategoria().toUpperCase() : "GENERAL";
            revenueByCategory.merge(cat, event.getMonto(), Double::sum);
            countByCategory.merge(cat, 1L, Long::sum);
        }

        partitionCounter.computeIfAbsent(partition, k -> new AtomicLong(0)).incrementAndGet();

        // Almacenar en historial reciente para el panel de Frontend
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("eventId", event.getEventId());
        record.put("orderId", event.getOrderId());
        record.put("cliente", event.getCliente());
        record.put("producto", event.getProducto());
        record.put("monto", event.getMonto());
        record.put("categoria", event.getCategoria());
        record.put("canalVenta", event.getCanalVenta());
        record.put("estado", event.getEstado());
        record.put("timestamp", event.getTimestamp());
        record.put("partition", partition);
        record.put("offset", offset);
        record.put("key", key);
        record.put("processedAt", Instant.now().toString());

        recentStreamEvents.add(0, record);
        while (recentStreamEvents.size() > MAX_HISTORY) {
            recentStreamEvents.remove(recentStreamEvents.size() - 1);
        }
    }

    /**
     * Procesa un evento de auditoria entrante desde el topico de auditoria.
     */
    public void processAuditEvent(OrderAuditStreamEvent event, int partition, long offset, String key) {
        log.info("[AUDIT-PROCESSING] Auditoria Pedido '{}' - Accion: {}, Particion: {}, Offset: {}",
                event.getOrderId(), event.getAccion(), partition, offset);

        totalEventsConsumed.incrementAndGet();
        totalAuditEvents.incrementAndGet();
        partitionCounter.computeIfAbsent(partition, k -> new AtomicLong(0)).incrementAndGet();

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("auditId", event.getAuditId());
        record.put("orderId", event.getOrderId());
        record.put("accion", event.getAccion());
        record.put("estadoAnterior", event.getEstadoAnterior());
        record.put("nuevoEstado", event.getNuevoEstado());
        record.put("usuarioOperador", event.getUsuarioOperador());
        record.put("origen", event.getOrigen());
        record.put("timestamp", event.getTimestamp());
        record.put("detalles", event.getDetalles());
        record.put("partition", partition);
        record.put("offset", offset);
        record.put("key", key);
        record.put("processedAt", Instant.now().toString());

        recentAuditTrail.add(0, record);
        while (recentAuditTrail.size() > MAX_HISTORY) {
            recentAuditTrail.remove(recentAuditTrail.size() - 1);
        }
    }

    /**
     * Retorna metricas consolidadas en tiempo real para el panel de administracion y Frontend.
     */
    public Map<String, Object> getStreamingMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalEventsConsumed", totalEventsConsumed.get());
        metrics.put("totalAnalyticsEvents", totalAnalyticsEvents.get());
        metrics.put("totalAuditEvents", totalAuditEvents.get());
        metrics.put("totalRevenueStreamed", totalRevenueStreamed.get());

        Map<String, Long> partitionMap = new LinkedHashMap<>();
        partitionCounter.forEach((k, v) -> partitionMap.put("partition_" + k, v.get()));
        metrics.put("partitionDistribution", partitionMap);

        metrics.put("revenueByCategory", new HashMap<>(revenueByCategory));
        metrics.put("countByCategory", new HashMap<>(countByCategory));
        metrics.put("recentEventsCount", recentStreamEvents.size());
        metrics.put("recentAuditCount", recentAuditTrail.size());
        metrics.put("systemTime", Instant.now().toString());
        return metrics;
    }

    public List<Map<String, Object>> getRecentStreamEvents() {
        return Collections.unmodifiableList(recentStreamEvents);
    }

    public List<Map<String, Object>> getRecentAuditTrail() {
        return Collections.unmodifiableList(recentAuditTrail);
    }

    public void resetMetrics() {
        totalEventsConsumed.set(0);
        totalAnalyticsEvents.set(0);
        totalAuditEvents.set(0);
        totalRevenueStreamed.set(0);
        partitionCounter.values().forEach(v -> v.set(0));
        revenueByCategory.clear();
        countByCategory.clear();
        recentStreamEvents.clear();
        recentAuditTrail.clear();
    }
}
