package com.pedidos360.rabbitmq.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateQueueDto {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede contener letras, numeros, puntos, guiones y guiones bajos")
    private String name;

    private boolean durable = true;
    private boolean exclusive = false;
    private boolean autoDelete = false;

    @Min(value = 1000, message = "El TTL minimo permitido es 1000 ms (1 segundo)")
    @Max(value = 604800000, message = "El TTL maximo permitido es 7 dias")
    private Integer messageTtl;

    private String deadLetterExchange;
    private String deadLetterRoutingKey;

    @Min(value = 1, message = "La longitud maxima de cola debe ser de al menos 1")
    private Integer maxLength;

    public CreateQueueDto() {}

    public CreateQueueDto(String name, boolean durable, Integer messageTtl, String deadLetterExchange, String deadLetterRoutingKey) {
        this.name = name;
        this.durable = durable;
        this.messageTtl = messageTtl;
        this.deadLetterExchange = deadLetterExchange;
        this.deadLetterRoutingKey = deadLetterRoutingKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public Integer getMessageTtl() {
        return messageTtl;
    }

    public void setMessageTtl(Integer messageTtl) {
        this.messageTtl = messageTtl;
    }

    public String getDeadLetterExchange() {
        return deadLetterExchange;
    }

    public void setDeadLetterExchange(String deadLetterExchange) {
        this.deadLetterExchange = deadLetterExchange;
    }

    public String getDeadLetterRoutingKey() {
        return deadLetterRoutingKey;
    }

    public void setDeadLetterRoutingKey(String deadLetterRoutingKey) {
        this.deadLetterRoutingKey = deadLetterRoutingKey;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
}
