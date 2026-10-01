package com.pedidos360.controller;

import com.pedidos360.entity.Pedido;
import com.pedidos360.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    public List<Pedido> getPedidos() {
        return pedidoRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    public Pedido updatePedido(@org.springframework.web.bind.annotation.PathVariable Long id,
            @Valid @RequestBody Pedido datos) {
        return pedidoRepository.findById(id).map(existente -> {
            existente.setNombre(datos.getNombre());
            existente.setCliente(datos.getCliente());
            if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
                existente.setEstado(datos.getEstado());
            }
            existente.setTotal(datos.getTotal());
            return pedidoRepository.save(existente);
        }).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND,
                "Pedido no encontrado"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePedido(@org.springframework.web.bind.annotation.PathVariable Long id) {
        if (pedidoRepository.existsById(id)) {
            pedidoRepository.deleteById(id);
        }
    }
}
