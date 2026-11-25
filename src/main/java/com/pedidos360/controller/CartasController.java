package com.pedidos360.controller;

import com.pedidos360.entity.Carta;
import com.pedidos360.entity.Orden;
import com.pedidos360.kafka.KafkaEventProducer;
import com.pedidos360.kafka.dto.OrderAnalyticsEvent;
import com.pedidos360.kafka.dto.OrderAuditStreamEvent;
import com.pedidos360.rabbitmq.OrderEvent;
import com.pedidos360.rabbitmq.OrderProducer;
import com.pedidos360.repository.CartaRepository;
import com.pedidos360.repository.OrdenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catalogo publico de cartas Pokemon TCG y ordenes de compra (protegidas con JWT de Entra ID).
 * Cada compra se persiste en PostgreSQL y se propaga a RabbitMQ (orden) y Kafka (analitica + auditoria).
 */
@RestController
public class CartasController {

    private static final Logger log = LoggerFactory.getLogger(CartasController.class);
    private static final BigDecimal PRECIO_SOBRE = BigDecimal.valueOf(5000);
    private static final int CARTAS_POR_SOBRE = 5;

    private final CartaRepository cartaRepository;
    private final OrdenRepository ordenRepository;
    private final OrderProducer orderProducer;
    private final KafkaEventProducer kafkaProducer;

    public CartasController(CartaRepository cartaRepository, OrdenRepository ordenRepository,
                            OrderProducer orderProducer, KafkaEventProducer kafkaProducer) {
        this.cartaRepository = cartaRepository;
        this.ordenRepository = ordenRepository;
        this.orderProducer = orderProducer;
        this.kafkaProducer = kafkaProducer;
    }

    public record CompraRequest(Long cartaId, String cliente, Integer cantidad) {
    }

    public record SobreRequest(String cliente) {
    }

    // ---------------------------------------------------------------- catalogo (publico)

    @GetMapping("/api/public/cartas")
    public List<Carta> listarCartas() {
        return cartaRepository.findAll();
    }

    @GetMapping("/api/public/cartas/{id}")
    public ResponseEntity<Carta> obtenerCarta(@PathVariable Long id) {
        return cartaRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------- ordenes (JWT)

    @GetMapping("/api/ordenes")
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    public List<Orden> listarOrdenes() {
        return ordenRepository.findAllByOrderByFechaDesc();
    }

    @PostMapping("/api/ordenes")
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    public ResponseEntity<?> comprar(@RequestBody CompraRequest request) {
        if (request == null || request.cartaId() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "cartaId es obligatorio"));
        }
        Carta carta = cartaRepository.findById(request.cartaId()).orElse(null);
        if (carta == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Carta no encontrada"));
        }
        int cantidad = request.cantidad() == null || request.cantidad() < 1 ? 1 : Math.min(request.cantidad(), 20);
        String cliente = request.cliente() == null || request.cliente().isBlank() ? "Entrenador" : request.cliente();

        Orden orden = new Orden();
        orden.setCartaId(carta.getId());
        orden.setCartaNombre(carta.getNombre());
        orden.setTipo(carta.getTipo());
        orden.setImagenUrl(carta.getImagenUrl());
        orden.setCliente(cliente);
        orden.setCantidad(cantidad);
        orden.setTotal(carta.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        Orden guardada = ordenRepository.save(orden);

        publicar(guardada, List.of(carta), "COMPRA_CARTA");
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PostMapping("/api/ordenes/sobre")
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    public ResponseEntity<?> abrirSobre(@RequestBody(required = false) SobreRequest request) {
        List<Carta> todas = new ArrayList<>(cartaRepository.findAll());
        if (todas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Catalogo vacio"));
        }
        Collections.shuffle(todas);
        List<Carta> sobre = new ArrayList<>(todas.subList(0, Math.min(CARTAS_POR_SOBRE, todas.size())));
        String cliente = request == null || request.cliente() == null || request.cliente().isBlank()
                ? "Entrenador" : request.cliente();

        Orden orden = new Orden();
        orden.setCartaNombre("Sobre: " + String.join(", ", sobre.stream().map(Carta::getNombre).toList()));
        orden.setTipo("SOBRE");
        orden.setImagenUrl(sobre.get(0).getImagenUrl());
        orden.setCliente(cliente);
        orden.setCantidad(sobre.size());
        orden.setTotal(PRECIO_SOBRE);
        Orden guardada = ordenRepository.save(orden);

        publicar(guardada, sobre, "APERTURA_SOBRE");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orden", guardada);
        body.put("cartas", sobre);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ---------------------------------------------------------------- mensajeria

    /** Publica sin romper la compra si un broker esta caido (la orden ya esta persistida). */
    private void publicar(Orden orden, List<Carta> cartas, String accion) {
        String orderId = "TCG-" + orden.getId() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        try {
            orderProducer.publishOrder(new OrderEvent(orderId, orden.getCliente(), orden.getCartaNombre(),
                    orden.getTotal().doubleValue(), false, null));
        } catch (Exception e) {
            log.warn("[TCG] RabbitMQ no disponible para orden {}: {}", orderId, e.getMessage());
        }
        try {
            double montoPorCarta = orden.getTotal().doubleValue() / Math.max(cartas.size(), 1);
            for (Carta carta : cartas) {
                kafkaProducer.publishAnalyticsEvent(new OrderAnalyticsEvent(
                        orderId, orden.getCliente(), carta.getNombre(), montoPorCarta,
                        carta.getTipo().toUpperCase(), "WEB", "PROCESADO"));
            }
            kafkaProducer.publishAuditEvent(new OrderAuditStreamEvent(
                    orderId, accion, "CREADA", "PROCESADA", orden.getCliente(),
                    orden.getCantidad() + " carta(s) - " + orden.getCartaNombre()));
        } catch (Exception e) {
            log.warn("[TCG] Kafka no disponible para orden {}: {}", orderId, e.getMessage());
        }
    }
}
