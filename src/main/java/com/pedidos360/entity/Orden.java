package com.pedidos360.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orden de compra de cartas (o de un sobre). Es el hecho de negocio que dispara
 * mensajes en RabbitMQ (transaccional) y eventos en Kafka (streaming/analitica).
 */
@Entity
@Table(name = "ordenes_cartas")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carta_id")
    private Long cartaId;

    @Column(name = "carta_nombre", nullable = false, length = 600)
    private String cartaNombre;

    @Column(nullable = false)
    private String tipo = "Colorless";

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private String cliente;

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private String estado = "CREADA";

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    public Orden() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCartaId() { return cartaId; }
    public void setCartaId(Long cartaId) { this.cartaId = cartaId; }
    public String getCartaNombre() { return cartaNombre; }
    public void setCartaNombre(String cartaNombre) { this.cartaNombre = cartaNombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
