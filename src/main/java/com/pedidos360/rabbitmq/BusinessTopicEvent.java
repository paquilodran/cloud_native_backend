package com.pedidos360.rabbitmq;

import java.io.Serializable;
import java.time.Instant;

public class BusinessTopicEvent implements Serializable {

    private String eventId;
    private String entidad;
    private String accion;
    private String payload;
    private boolean simularFallo;
    private String timestamp;

    public BusinessTopicEvent() {
        this.timestamp = Instant.now().toString();
    }

    public BusinessTopicEvent(String eventId, String entidad, String accion, String payload, boolean simularFallo) {
        this.eventId = eventId;
        this.entidad = entidad;
        this.accion = accion;
        this.payload = payload;
        this.simularFallo = simularFallo;
        this.timestamp = Instant.now().toString();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isSimularFallo() {
        return simularFallo;
    }

    public void setSimularFallo(boolean simularFallo) {
        this.simularFallo = simularFallo;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
