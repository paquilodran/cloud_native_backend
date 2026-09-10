package com.pedidos360.config;

import com.pedidos360.entity.Pedido;
import com.pedidos360.repository.PedidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PedidoRepository pedidoRepository;

    public DataSeeder(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public void run(String... args) {
        if (pedidoRepository.count() > 0) {
            return;
        }
        pedidoRepository.save(new Pedido("Pedido oficina", "Cliente Norte", "CREADO", new BigDecimal("15000.00")));
        pedidoRepository.save(new Pedido("Pedido retail", "Cliente Sur", "EN_PREPARACION", new BigDecimal("8200.50")));
        pedidoRepository.save(new Pedido("Pedido mayorista", "Cliente Este", "ENTREGADO", new BigDecimal("43100.00")));
    }
}
