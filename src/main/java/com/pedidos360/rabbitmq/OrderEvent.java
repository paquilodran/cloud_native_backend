package com.pedidos360.rabbitmq;

import java.io.Serializable;
import java.time.Instant;

public class OrderEvent implements Serializable {

    private String orderId;
    private String cliente;
    private String producto;
    private double total;
    private boolean simularFallo;
    private String motivoFallo;
    private String timestamp;

    public OrderEvent() {
        this.timestamp = Instant.now().toString();
    }

    public OrderEvent(String orderId, String cliente, String producto, double total, boolean simularFallo, String motivoFallo) {
        this.orderId = orderId;
        this.cliente = cliente;
        this.producto = producto;
        this.total = total;
        this.simularFallo = simularFallo;
        this.motivoFallo = motivoFallo;
        this.timestamp = Instant.now().toString();
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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public boolean isSimularFallo() {
        return simularFallo;
    }

    public void setSimularFallo(boolean simularFallo) {
        this.simularFallo = simularFallo;
    }

    public String getMotivoFallo() {
        return motivoFallo;
    }

    public void setMotivoFallo(String motivoFallo) {
        this.motivoFallo = motivoFallo;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
