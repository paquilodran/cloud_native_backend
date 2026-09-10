package com.pedidos360.controller;

import com.pedidos360.entity.Pedido;
import com.pedidos360.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosController {

    private final PedidoRepository pedidoRepository;

    public PedidosController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping
    @Secured("ROLE_AUTHENTICATED")
    public List<Pedido> getPedidos() {
        return pedidoRepository.findAll();
    }

    @PostMapping
    @Secured("ROLE_AUTHENTICATED")
    @ResponseStatus(HttpStatus.CREATED)
    public Pedido createPedido(@Valid @RequestBody Pedido pedido) {
        pedido.setId(null);
        if (pedido.getEstado() == null || pedido.getEstado().isBlank()) {
            pedido.setEstado("CREADO");
        }
        if (pedido.getFecha() == null) {
            pedido.setFecha(java.time.Instant.now());
        }
        return pedidoRepository.save(pedido);
    }
}
