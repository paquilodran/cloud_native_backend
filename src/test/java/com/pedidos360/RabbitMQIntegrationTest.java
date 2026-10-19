package com.pedidos360;

import com.pedidos360.rabbitmq.DeadLetterConsumer;
import com.pedidos360.rabbitmq.OrderEvent;
import com.pedidos360.rabbitmq.OrderProducer;
import com.pedidos360.rabbitmq.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RabbitMQIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private DeadLetterConsumer deadLetterConsumer;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/public/rabbitmq/status debe devolver la topologia activa")
    void testTopologyStatus() throws Exception {
        mockMvc.perform(get("/api/public/rabbitmq/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.system").value("Pedidos360 - Resilient AMQP Module"))
                .andExpect(jsonPath("$.exchange").value(RabbitMQConfig.ORDERS_EXCHANGE + " (Direct)"))
                .andExpect(jsonPath("$.dlx").value(RabbitMQConfig.DLX_EXCHANGE + " (Fanout)"));
    }

    @Test
    @DisplayName("POST /api/public/rabbitmq/orders debe encolar exitosamente un pedido normal")
    void testCreateOrder() throws Exception {
        String json = """
            {
                "orderId": "ORD-TEST-001",
                "cliente": "Empresa Demo SA",
                "producto": "Licencia Cloud Pro",
                "total": 299990.0,
                "simularFallo": false
            }
        """;

        mockMvc.perform(post("/api/public/rabbitmq/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENQUEUED"))
                .andExpect(jsonPath("$.order.orderId").value("ORD-TEST-001"));
    }

    @Test
    @DisplayName("Flujo de fallo resiliente: 3 reintentos y enrutamiento hacia la DLQ")
    void testFailingOrderFlowToDLQ() throws Exception {
        deadLetterConsumer.clear();

        String failJson = """
            {
                "orderId": "FAIL-TEST-999",
                "cliente": "Cliente Fallo Test",
                "producto": "Transaccion Rechazada",
                "total": 85000.0,
                "simularFallo": true,
                "motivoFallo": "Fondo insuficiente simulado"
            }
        """;

        mockMvc.perform(post("/api/public/rabbitmq/orders/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENQUEUED_FAILING"));

        // Esperar a que el consumidor ejecute los 3 reintentos con backoff (1s, 2s) y transfiera a DLQ
        Thread.sleep(6000);

        // Verificar que el consumidor de DLQ capturo el mensaje muerto
        assertThat(deadLetterConsumer.getRecords()).isNotEmpty();
        DeadLetterConsumer.DeadLetterEntry dlqEntry = deadLetterConsumer.getRecords().stream()
                .filter(e -> "FAIL-TEST-999".equals(e.event().getOrderId()))
                .findFirst()
                .orElse(null);

        assertThat(dlqEntry).isNotNull();
        assertThat(dlqEntry.colaOrigen()).isEqualTo(RabbitMQConfig.ORDERS_QUEUE);
        assertThat(dlqEntry.razon()).isEqualTo("rejected");

        // Verificar endpoint GET /api/public/rabbitmq/dlq
        mockMvc.perform(get("/api/public/rabbitmq/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verificar endpoint DELETE /api/public/rabbitmq/dlq
        mockMvc.perform(delete("/api/public/rabbitmq/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Historial de DLQ vaciado"));

        assertThat(deadLetterConsumer.getRecords()).isEmpty();
    }
}
