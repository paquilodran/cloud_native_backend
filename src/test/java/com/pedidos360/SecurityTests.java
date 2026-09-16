package com.pedidos360;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.entity.Pedido;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET /api/public/health debe responder 200 sin autenticacion")
    void healthDebeSerPublico() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("pedidos360-backend"));
    }

    @Test
    @DisplayName("GET /api/pedidos sin token debe responder 401 Unauthorized estructurado")
    void pedidosSinTokenDebeRetornar401() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/pedidos con Bearer invalido debe responder 401 Unauthorized estructurado")
    void pedidosConBearerInvalidoDebeRetornar401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Token invalido o firma no verificable"));

        mockMvc.perform(get("/api/pedidos")
                        .header("Authorization", "Bearer token-mal-formado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/pedidos con token valido pero sin scope access_as_user debe responder 403 Forbidden estructurado")
    void pedidosSinScopeAdecuadoDebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_otro_scope"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/pedidos con scope access_as_user debe responder 200 OK y retornar lista")
    void pedidosConScopeValidoDebeRetornar200() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/pedidos con scope access_as_user debe responder 201 Created y persistir")
    void crearPedidoConScopeValidoDebeRetornar201() throws Exception {
        Pedido nuevo = new Pedido();
        nuevo.setNombre("Pedido Prueba Test");
        nuevo.setCliente("Cliente Test");
        nuevo.setTotal(BigDecimal.valueOf(15990.00));

        mockMvc.perform(post("/api/pedidos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_access_as_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Pedido Prueba Test"))
                .andExpect(jsonPath("$.estado").value("CREADO"));
    }
}
