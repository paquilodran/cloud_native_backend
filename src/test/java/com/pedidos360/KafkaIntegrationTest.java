package com.pedidos360;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.kafka.*;
import com.pedidos360.kafka.dto.CreateKafkaTopicDto;
import com.pedidos360.kafka.dto.KafkaTopicInfoDto;
import com.pedidos360.kafka.dto.OrderAnalyticsEvent;
import com.pedidos360.kafka.dto.OrderAuditStreamEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class KafkaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaStreamingAnalyticsService analyticsService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private AdminClient adminClient;

    @MockBean
    private KafkaAdminService kafkaAdminService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("DTOs Tipados Kafka: Validacion de estructura y campos requeridos")
    void testKafkaDtos() {
        OrderAnalyticsEvent analytics = new OrderAnalyticsEvent(
                "ORD-999", "Juan Perez", "Laptop Pro", 899990.0, "TECNOLOGIA", "WEB", "PROCESADO"
        );
        assertThat(analytics.getOrderId()).isEqualTo("ORD-999");
        assertThat(analytics.getMonto()).isEqualTo(899990.0);
        assertThat(analytics.getCategoria()).isEqualTo("TECNOLOGIA");
        assertThat(analytics.getEventId()).isNotBlank();
        assertThat(analytics.getTimestamp()).isNotBlank();

        OrderAuditStreamEvent audit = new OrderAuditStreamEvent(
                "ORD-999", "PAGO_CONFIRMADO", "CREADO", "PAGADO", "admin@duoc.cl", "Pago con WebPay"
        );
        assertThat(audit.getOrderId()).isEqualTo("ORD-999");
        assertThat(audit.getAccion()).isEqualTo("PAGO_CONFIRMADO");
        assertThat(audit.getEstadoAnterior()).isEqualTo("CREADO");
        assertThat(audit.getNuevoEstado()).isEqualTo("PAGADO");
        assertThat(audit.getAuditId()).isNotBlank();
    }

    @Test
    @DisplayName("KafkaStreamingAnalyticsService: Procesamiento desacoplado y agregacion en tiempo real")
    void testStreamingAnalyticsService() {
        analyticsService.resetMetrics();

        OrderAnalyticsEvent event = new OrderAnalyticsEvent(
                "ORD-TEST-1", "Cliente Test", "Mouse Gamer", 35000.0, "TECNOLOGIA", "WEB", "PROCESADO"
        );
        analyticsService.processAnalyticsEvent(event, 1, 105L, "ORD-TEST-1");

        Map<String, Object> metrics = analyticsService.getStreamingMetrics();
        assertThat(metrics.get("totalEventsConsumed")).isEqualTo(1L);
        assertThat(metrics.get("totalAnalyticsEvents")).isEqualTo(1L);
        assertThat(metrics.get("totalRevenueStreamed")).isEqualTo(35000L);

        List<Map<String, Object>> recent = analyticsService.getRecentStreamEvents();
        assertThat(recent).isNotEmpty();
        assertThat(recent.get(0).get("orderId")).isEqualTo("ORD-TEST-1");
        assertThat(recent.get(0).get("partition")).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/public/kafka/status retorna los 2 topicos configurados con 3 particiones y 3 replicas")
    void testKafkaStatusEndpoint() throws Exception {
        mockMvc.perform(get("/api/public/kafka/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONNECTED"))
                .andExpect(jsonPath("$.topics").isArray())
                .andExpect(jsonPath("$.topics[0].partitions").value(3))
                .andExpect(jsonPath("$.topics[0].replicas").value(3));
    }

    @Test
    @DisplayName("POST /api/public/kafka/publish/analytics publica exitosamente evento analitico")
    void testPublishAnalyticsEndpoint() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        OrderAnalyticsEvent request = new OrderAnalyticsEvent(
                "ORD-777", "Carlos Gomez", "Teclado Mecanico", 60000.0, "TECNOLOGIA", "WEB", "PROCESADO"
        );

        mockMvc.perform(post("/api/public/kafka/publish/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.event.orderId").value("ORD-777"));
    }

    @Test
    @DisplayName("POST /api/public/kafka/publish/audit publica exitosamente evento de auditoria")
    void testPublishAuditEndpoint() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        OrderAuditStreamEvent request = new OrderAuditStreamEvent(
                "ORD-777", "DESPACHO", "PAGADO", "DESPACHADO", "logistica@duoc.cl", "Despacho a domicilio"
        );

        mockMvc.perform(post("/api/public/kafka/publish/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.auditEvent.accion").value("DESPACHO"));
    }

    @Test
    @DisplayName("EP5 Microservicio Administrador: Listar y crear topicos")
    void testKafkaAdminEndpoints() throws Exception {
        when(kafkaAdminService.getClusterOverview()).thenReturn(Map.of(
                "clusterId", "test-cluster",
                "brokersCount", 3,
                "status", "HEALTHY"
        ));

        mockMvc.perform(get("/api/public/kafka/admin/cluster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brokersCount").value(3))
                .andExpect(jsonPath("$.status").value("HEALTHY"));

        // Validacion de creacion de topico
        when(kafkaAdminService.createTopic(any())).thenReturn(true);

        CreateKafkaTopicDto topicDto = new CreateKafkaTopicDto("tcg.nuevo.topico", 3, (short) 3, 86400000L, "delete");

        mockMvc.perform(post("/api/public/kafka/admin/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topicDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.partitions").value(3))
                .andExpect(jsonPath("$.replicationFactor").value(3));
    }
}
