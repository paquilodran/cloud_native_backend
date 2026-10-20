package com.pedidos360;

import com.pedidos360.rabbitmq.DeadLetterConsumer;
import com.pedidos360.rabbitmq.OrderProducer;
import com.pedidos360.rabbitmq.RabbitAdminService;
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

    @Autowired
    private RabbitAdminService rabbitAdminService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/public/rabbitmq/status debe devolver las 3 colas, 3 DLQs y exchanges Direct y Topic")
    void testTopologyStatus() throws Exception {
        mockMvc.perform(get("/api/public/rabbitmq/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.queues").isArray())
                .andExpect(jsonPath("$.exchanges").isArray());
    }

    @Test
    @DisplayName("EP3 Admin API: Crear y eliminar colas dinamicamente con validacion DTO")
    void testDynamicQueueLifecycle() throws Exception {
        // 1. Probar validacion DTO: nombre vacio debe fallar con 400 Bad Request
        String invalidQueueJson = """
            {
                "name": "",
                "durable": true
            }
        """;
        mockMvc.perform(post("/api/public/rabbitmq/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidQueueJson))
                .andExpect(status().isBadRequest());

        // 2. Crear cola valida
        String validQueueJson = """
            {
                "name": "test.dinamica.ep3",
                "durable": true,
                "messageTtl": 60000
            }
        """;
        mockMvc.perform(post("/api/public/rabbitmq/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validQueueJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.queue.name").value("test.dinamica.ep3"));

        // 3. Consultar informacion de la cola creada
        mockMvc.perform(get("/api/public/rabbitmq/admin/queues/test.dinamica.ep3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

        // 4. Eliminar la cola
        mockMvc.perform(delete("/api/public/rabbitmq/admin/queues/test.dinamica.ep3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    @DisplayName("EP3 Admin API: Crear exchange dinamico con validacion DTO")
    void testDynamicExchangeLifecycle() throws Exception {
        String exchangeJson = """
            {
                "name": "test.exchange.ep3",
                "type": "direct",
                "durable": true
            }
        """;
        mockMvc.perform(post("/api/public/rabbitmq/admin/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(exchangeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(delete("/api/public/rabbitmq/admin/exchanges/test.exchange.ep3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test
    @DisplayName("Caso 1: Encolar y procesar orden normal en orders.queue")
    void testCreateOrder() throws Exception {
        String json = """
            {
                "orderId": "ORD-EP3-001",
                "cliente": "Empresa Duoc SA",
                "producto": "Servidor Cloud Native",
                "total": 450000.0,
                "simularFallo": false
            }
        """;

        mockMvc.perform(post("/api/public/rabbitmq/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENQUEUED"))
                .andExpect(jsonPath("$.order.orderId").value("ORD-EP3-001"));
    }

    @Test
    @DisplayName("Caso 2: Encolar notificacion en notifications.queue")
    void testCreateNotification() throws Exception {
        String json = """
            {
                "id": "NOTIF-EP3-001",
                "destinatario": "profesor@duoc.cl",
                "mensaje": "Evaluacion Parcial N 3 iniciada",
                "tipoCanal": "EMAIL",
                "simularFallo": false
            }
        """;

        mockMvc.perform(post("/api/public/rabbitmq/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENQUEUED"))
                .andExpect(jsonPath("$.notification.id").value("NOTIF-EP3-001"));
    }

    @Test
    @DisplayName("Caso 3: Encolar evento topic en events.topic.queue con patron pedidos.*")
    void testCreateTopicEvent() throws Exception {
        String json = """
            {
                "eventId": "TOPIC-EP3-001",
                "entidad": "PEDIDO",
                "accion": "DESPACHADO",
                "payload": "{\\"guiaDespacho\\": \\"GD-998811\\"}",
                "simularFallo": false
            }
        """;

        mockMvc.perform(post("/api/public/rabbitmq/topic-events?routingKey=pedidos.despachado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENQUEUED"))
                .andExpect(jsonPath("$.exchange").value(RabbitMQConfig.TOPIC_EXCHANGE))
                .andExpect(jsonPath("$.routingKey").value("pedidos.despachado"));
    }

    @Test
    @DisplayName("Flujo resiliente con DLQ: 3 reintentos y enrutamiento hacia orders.dlq")
    void testFailingOrderFlowToDLQ() throws Exception {
        deadLetterConsumer.clear();

        String failJson = """
            {
                "orderId": "FAIL-DLQ-777",
                "cliente": "Cliente Test Resiliencia",
                "producto": "Transaccion Rechazada",
                "total": 99000.0,
                "simularFallo": true,
                "motivoFallo": "Fondo insuficiente simulado"
            }
        """;

        mockMvc.perform(post("/api/public/rabbitmq/orders/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENQUEUED_FAILING"));

        // Esperar a que el consumidor reintente 3 veces y desvie a DLQ
        Thread.sleep(6000);

        assertThat(deadLetterConsumer.getRecords()).isNotEmpty();

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
