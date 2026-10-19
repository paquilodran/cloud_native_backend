package com.pedidos360.rabbitmq;

import com.pedidos360.rabbitmq.dto.CreateBindingDto;
import com.pedidos360.rabbitmq.dto.CreateExchangeDto;
import com.pedidos360.rabbitmq.dto.CreateQueueDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST del Microservicio Administrador de RabbitMQ.
 * Expone endpoints REST para administracion dinamica de colas, exchanges y bindings.
 * Cumple con el criterio 6 y 8 de la evaluacion EP3.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/public/rabbitmq/admin")
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    public RabbitAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    /**
     * POST /queues: Crear cola con validacion DTO de parametros de entrada
     */
    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> createQueue(@Valid @RequestBody CreateQueueDto request) {
        boolean created = rabbitAdminService.declareQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "CREATED",
                "message", "Cola '" + request.getName() + "' creada exitosamente",
                "queue", request,
                "success", created
        ));
    }

    /**
     * DELETE /queues/{name}: Eliminar una cola por su nombre
     */
    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> deleteQueue(
            @PathVariable("name") String name,
            @RequestParam(name = "unused", defaultValue = "false") boolean unused,
            @RequestParam(name = "empty", defaultValue = "false") boolean empty) {

        boolean deleted = rabbitAdminService.deleteQueue(name, unused, empty);
        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "message", "Cola '" + name + "' procesada para eliminacion",
                "success", deleted
        ));
    }

    /**
     * POST /queues/{name}/purge: Vaciar mensajes de una cola
     */
    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<Map<String, Object>> purgeQueue(@PathVariable("name") String name) {
        boolean purged = rabbitAdminService.purgeQueue(name);
        return ResponseEntity.ok(Map.of(
                "status", "PURGED",
                "queue", name,
                "success", purged
        ));
    }

    /**
     * GET /queues/{name}: Consultar estadisticas e informacion de una cola
     */
    @GetMapping("/queues/{name}")
    public ResponseEntity<Map<String, Object>> getQueueInfo(@PathVariable("name") String name) {
        Map<String, Object> info = rabbitAdminService.getQueueInfo(name);
        return ResponseEntity.ok(info);
    }

    /**
     * POST /exchanges: Crear Exchange (direct, fanout, topic)
     */
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> createExchange(@Valid @RequestBody CreateExchangeDto request) {
        rabbitAdminService.declareExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "CREATED",
                "message", "Exchange '" + request.getName() + "' de tipo " + request.getType() + " creado exitosamente",
                "exchange", request
        ));
    }

    /**
     * DELETE /exchanges/{name}: Eliminar un Exchange
     */
    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Map<String, Object>> deleteExchange(@PathVariable("name") String name) {
        boolean deleted = rabbitAdminService.deleteExchange(name);
        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "exchange", name,
                "success", deleted
        ));
    }

    /**
     * POST /bindings: Crear enlace entre cola y exchange
     */
    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> createBinding(@Valid @RequestBody CreateBindingDto request) {
        rabbitAdminService.declareBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "CREATED",
                "message", "Binding creado: cola '" + request.getQueueName() + "' vinculada a exchange '" + request.getExchangeName() + "'",
                "binding", request
        ));
    }

    /**
     * DELETE /bindings: Eliminar enlace
     */
    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, Object>> deleteBinding(@Valid @RequestBody CreateBindingDto request) {
        rabbitAdminService.removeBinding(request);
        return ResponseEntity.ok(Map.of(
                "status", "DELETED",
                "message", "Binding eliminado"
        ));
    }
}
