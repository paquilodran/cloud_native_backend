package com.pedidos360.kafka.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO tipado para eventos de auditoria y trazabilidad en Apache Kafka.
 * Topic principal: pedidos.audit.stream
 */
public class OrderAuditStreamEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String auditId;
    private String orderId;
    private String accion;
    private String estadoAnterior;
    private String nuevoEstado;
    private String usuarioOperador;
    private String origen;
    private String ipOrigen;
    private String timestamp;
    private String detalles;

    public OrderAuditStreamEvent() {
        this.auditId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
        this.origen = "BACKEND_SERVICE";
    }

    public OrderAuditStreamEvent(String orderId, String accion, String estadoAnterior,
                                 String nuevoEstado, String usuarioOperador, String detalles) {
        this.auditId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.accion = accion;
        this.estadoAnterior = estadoAnterior;
        this.nuevoEstado = nuevoEstado;
        this.usuarioOperador = usuarioOperador != null ? usuarioOperador : "SISTEMA";
        this.origen = "BACKEND_SERVICE";
        this.ipOrigen = "127.0.0.1";
        this.timestamp = Instant.now().toString();
        this.detalles = detalles;
    }

    public String getAuditId() {
        return auditId;
    }

    public void setAuditId(String auditId) {
        this.auditId = auditId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getUsuarioOperador() {
        return usuarioOperador;
    }

    public void setUsuarioOperador(String usuarioOperador) {
        this.usuarioOperador = usuarioOperador;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetalles() {
        return detalles;
    }

    public void setDetalles(String detalles) {
        this.detalles = detalles;
    }

    @Override
    public String toString() {
        return "OrderAuditStreamEvent{" +
                "auditId='" + auditId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", accion='" + accion + '\'' +
                ", estadoAnterior='" + estadoAnterior + '\'' +
                ", nuevoEstado='" + nuevoEstado + '\'' +
                ", usuarioOperador='" + usuarioOperador + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
