package com.pedidos360.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Carta del catalogo Pokemon TCG (origen: pokemontcg.io, con catalogo semilla de respaldo).
 */
@Entity
@Table(name = "cartas")
public class Carta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", unique = true)
    private String apiId;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String tipo = "Colorless";

    @Column(nullable = false)
    private String rareza = "Common";

    private Integer hp;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio = BigDecimal.valueOf(1500);

    public Carta() {
    }

    public Carta(String apiId, String nombre, String tipo, String rareza, Integer hp, String imagenUrl, BigDecimal precio) {
        this.apiId = apiId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.rareza = rareza;
        this.hp = hp;
        this.imagenUrl = imagenUrl;
        this.precio = precio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }
    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
