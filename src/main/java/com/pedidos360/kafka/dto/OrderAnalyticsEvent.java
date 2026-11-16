package com.pedidos360.kafka.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO tipado para eventos analiticos de streaming en Apache Kafka.
 * Topic principal: pedidos.analytics.stream
 */
public class OrderAnalyticsEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String orderId;
    private String cliente;
    private String producto;
    private Double monto;
    private String categoria;
    private String canalVenta;
    private String estado;
    private String timestamp;
    private String metricaTipo;

    public OrderAnalyticsEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
        this.metricaTipo = "ORDER_ANALYTICS";
    }

    public OrderAnalyticsEvent(String orderId, String cliente, String producto, Double monto,
                               String categoria, String canalVenta, String estado) {
        this.eventId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.cliente = cliente;
        this.producto = producto;
        this.monto = monto;
        this.categoria = categoria != null ? categoria : "GENERAL";
        this.canalVenta = canalVenta != null ? canalVenta : "WEB";
        this.estado = estado != null ? estado : "PROCESADO";
        this.timestamp = Instant.now().toString();
        this.metricaTipo = "ORDER_ANALYTICS";
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getCanalVenta() {
        return canalVenta;
    }

    public void setCanalVenta(String canalVenta) {
        this.canalVenta = canalVenta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetricaTipo() {
        return metricaTipo;
    }

    public void setMetricaTipo(String metricaTipo) {
        this.metricaTipo = metricaTipo;
    }

    @Override
    public String toString() {
        return "OrderAnalyticsEvent{" +
                "eventId='" + eventId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", cliente='" + cliente + '\'' +
                ", producto='" + producto + '\'' +
                ", monto=" + monto +
                ", categoria='" + categoria + '\'' +
                ", canalVenta='" + canalVenta + '\'' +
                ", estado='" + estado + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
