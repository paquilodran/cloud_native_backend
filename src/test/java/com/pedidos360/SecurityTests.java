package com.pedidos360;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.entity.Carta;
import com.pedidos360.kafka.KafkaEventProducer;
import com.pedidos360.rabbitmq.OrderProducer;
import com.pedidos360.repository.CartaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.tcg.api-enabled=false")
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CartaRepository cartaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private KafkaEventProducer kafkaEventProducer;

    @MockBean
    private OrderProducer orderProducer;

    @Test
    @DisplayName("GET /api/public/health debe responder 200 sin autenticacion")
    void healthDebeSerPublico() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/public/cartas es publico y retorna el catalogo semilla")
    void catalogoDebeSerPublico() throws Exception {
        mockMvc.perform(get("/api/public/cartas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nombre").exists())
                .andExpect(jsonPath("$[0].imagenUrl").exists());
    }

    @Test
    @DisplayName("GET /api/ordenes sin token debe responder 401 Unauthorized estructurado")
    void ordenesSinTokenDebeRetornar401() throws Exception {
        mockMvc.perform(get("/api/ordenes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/ordenes con Bearer invalido debe responder 401")
    void ordenesConBearerInvalidoDebeRetornar401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Token invalido"));

        mockMvc.perform(get("/api/ordenes").header("Authorization", "Bearer token-mal-formado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/ordenes con token sin scope access_as_user debe responder 403")
    void ordenesSinScopeAdecuadoDebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/ordenes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_otro_scope"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/ordenes con scope access_as_user debe responder 200 y lista")
    void ordenesConScopeValidoDebeRetornar200() throws Exception {
        mockMvc.perform(get("/api/ordenes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/ordenes con scope valido compra una carta y responde 201")
    void comprarCartaDebeRetornar201() throws Exception {
        Carta carta = cartaRepository.findAll().get(0);

        mockMvc.perform(post("/api/ordenes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cartaId", carta.getId(), "cliente", "Ash Ketchum", "cantidad", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.cartaNombre").value(carta.getNombre()))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.estado").value("CREADA"));
    }

    @Test
    @DisplayName("POST /api/ordenes/sobre abre un sobre de 5 cartas")
    void abrirSobreDebeRetornar5Cartas() throws Exception {
        mockMvc.perform(post("/api/ordenes/sobre")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":\"Misty\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartas.length()").value(5))
                .andExpect(jsonPath("$.orden.tipo").value("SOBRE"));
    }
}
