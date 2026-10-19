package com.pedidos360.rabbitmq;

import java.io.Serializable;
import java.time.Instant;

public class NotificationEvent implements Serializable {

    private String id;
    private String destinatario;
    private String mensaje;
    private String tipoCanal; // EMAIL, SMS, PUSH
    private boolean simularFallo;
    private String timestamp;

    public NotificationEvent() {
        this.timestamp = Instant.now().toString();
    }

    public NotificationEvent(String id, String destinatario, String mensaje, String tipoCanal, boolean simularFallo) {
        this.id = id;
        this.destinatario = destinatario;
        this.mensaje = mensaje;
        this.tipoCanal = tipoCanal;
        this.simularFallo = simularFallo;
        this.timestamp = Instant.now().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTipoCanal() {
        return tipoCanal;
    }

    public void setTipoCanal(String tipoCanal) {
        this.tipoCanal = tipoCanal;
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
