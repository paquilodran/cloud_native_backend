# Pedidos360 — Backend (Spring Boot + RabbitMQ Resiliente + JWT)

**Institución:** Duoc UC  
**Asignatura:** DSY1107 — Desarrollo Cloud Native I  
**Evaluación:** Evaluación Parcial N° 3 (Encargo 12%) & Evaluación Parcial N° 4 (Presentación 18%)  
**Rama Oficial:** `guia-dlx-dlq-retencion-alertas`  
**Repositorio Backend:** [https://github.com/paquilodran/cloud_native_backend](https://github.com/paquilodran/cloud_native_backend)  
**Repositorio Frontend:** [https://github.com/paquilodran/cloud_native_frontend](https://github.com/paquilodran/cloud_native_frontend)  

---

## 1. Descripción del Sistema

API REST desarrollada con **Java 17** y **Spring Boot 3.4**, que integra autenticación basada en tokens JWT con Microsoft Entra ID (IDaaS) y una **arquitectura de mensajería asíncrona desacoplada y resiliente con RabbitMQ**.

El backend incorpora:
1. **3 Colas de Mensajería Principales** con sus respectivas **3 Dead Letter Queues (DLQ)** y políticas de retención TTL.
2. **Exchanges Direct y Topic** para enrutamiento selectivo y por patrones (`pedidos.*`).
3. **Manejo de Resiliencia:** 3 reintentos automáticos con backoff exponencial, confirmaciones ACK/NACK explícitas y auditoría de metadatos `x-death`.
4. **Microservicio Administrador (`RabbitAdminService`):** Creación, eliminación y purga dinámica de colas y exchanges expuestos vía API REST, con validación estricta de parámetros mediante DTOs.
5. **Soporte de Alta Disponibilidad:** Compatible con clúster de 2 nodos de RabbitMQ en Docker.

---

## 2. Diagrama de Arquitectura y Flujo de Mensajería

```
[ Cliente Frontend (Angular) ]
              │
              ▼ (HTTP / REST)
     [ Pedidos360 Backend ]
     ├── SecurityConfig (JWT Azure AD / OAuth2)
     ├── RabbitAdminService (Administración Dinámica)
     └── OrderProducer (Publicación Asíncrona)
              │
    ┌─────────┴────────────────────────────────────────────────┐
    │                                                          │
    ▼ (Direct: order.created)                                  ▼ (Topic: pedidos.*)
[ orders.exchange ]                                    [ events.topic.exchange ]
    │                                                          │
    ▼                                                          ▼
[ orders.queue ] (TTL: 30s)                           [ events.topic.queue ] (TTL: 60s)
    │                                                          │
    ├─► Éxito: OrderConsumer (ACK)                            ├─► Éxito: BusinessTopicConsumer (ACK)
    └─► Error: 3 Reintentos con Backoff                       └─► Error: 3 Reintentos con Backoff
            │                                                          │
            ▼ (Exhausted / NACK)                                       ▼ (Exhausted / NACK)
      [ orders.dlx ] (Fanout)                                    [ events.dlx ] (Fanout)
            │                                                          │
            ▼                                                          ▼
      [ orders.dlq ] (Retención 24h)                            [ events.dlq ] (Retención 24h)
            │                                                          │
            └──────────────────────────┬───────────────────────────────┘
                                       ▼
                       [ DeadLetterConsumer ]
                       (Auditoría x-death: reason, count, queue)
```

---

## 3. Topología RabbitMQ Implementada (EP3 y EP4)

| Tipo Exchange | Exchange | Cola Principal | Routing Key | TTL Principal | Dead Letter Exchange (DLX) | Dead Letter Queue (DLQ) | Retención DLQ |
| :--- | :--- | :--- | :--- | :---: | :--- | :--- | :---: |
| **Direct** | `orders.exchange` | `orders.queue` | `order.created` | 30 seg | `orders.dlx` (Fanout) | `orders.dlq` | 24 horas |
| **Direct** | `notifications.exchange` | `notifications.queue` | `notification.send` | 45 seg | `notifications.dlx` (Fanout) | `notifications.dlq` | 24 horas |
| **Topic** | `events.topic.exchange` | `events.topic.queue` | `pedidos.*` | 60 seg | `events.dlx` (Fanout) | `events.dlq` | 24 horas |

---

## 4. Endpoints de la API REST

### A. Mensajería y Pruebas de Resiliencia (`/api/public/rabbitmq`)

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `GET` | `/api/public/rabbitmq/status` | Devuelve el estado de la topología (3 colas, 3 DLQs y exchanges). |
| `POST` | `/api/public/rabbitmq/orders` | Publica una orden nominal a `orders.queue` (procesada con ACK). |
| `POST` | `/api/public/rabbitmq/orders/fail` | Simula fallo crítico: reintenta 3 veces y se deriva a `orders.dlq`. |
| `POST` | `/api/public/rabbitmq/notifications` | Publica notificación en `notifications.queue`. |
| `POST` | `/api/public/rabbitmq/topic-events?routingKey=...` | Publica evento Topic bajo patrón (ej. `pedidos.despachado`). |
| `GET` | `/api/public/rabbitmq/dlq` | Obtiene la lista de mensajes muertos capturados con causa `x-death`. |
| `DELETE` | `/api/public/rabbitmq/dlq` | Limpia el historial de auditoría de la DLQ. |

### B. Microservicio Administrador de RabbitMQ (`/api/public/rabbitmq/admin`)

| Método | Endpoint | Payload / Parámetros | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/admin/queues` | `CreateQueueDto` (`name`, `messageTtl`, `durable`) | Crea una cola con validación DTO (`@NotBlank`, etc.). |
| `DELETE` | `/admin/queues/{name}` | `name` (path) | Elimina una cola dinámicamente. |
| `POST` | `/admin/queues/{name}/purge` | `name` (path) | Purga todos los mensajes pendientes de la cola. |
| `GET` | `/admin/queues/{name}` | `name` (path) | Retorna información y número de mensajes en la cola. |
| `POST` | `/admin/exchanges` | `CreateExchangeDto` (`name`, `type`) | Crea Exchange (direct, fanout, topic). |
| `DELETE` | `/admin/exchanges/{name}` | `name` (path) | Elimina un Exchange. |
| `POST` | `/admin/bindings` | `CreateBindingDto` (`queueName`, `exchangeName`, `routingKey`) | Crea vinculación entre cola y exchange. |

---

## 5. Instrucciones de Ejecución

### Requisitos Previos
- Java 17 (JDK)
- Docker Desktop en ejecución

### Paso 1: Iniciar RabbitMQ

**Opción A — Broker Standalone (Recomendado para pruebas locales):**
```powershell
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

**Opción B — Clúster de 2 Nodos (EP4):**
```powershell
# Levantar ambos nodos en clúster
docker compose -f docker-compose-cluster.yml up -d

# Sincronizar el clúster (unir nodo 2 a rabbit@rabbit1)
powershell .\scripts\setup-cluster.ps1

# Verificar estado del clúster
docker exec rabbitmq-cluster-node1 rabbitmqctl cluster_status
```

* Dashboard Nodo 1: [http://localhost:15672](http://localhost:15672) (User/Pass: `guest` / `guest`)
* Dashboard Nodo 2: [http://localhost:15673](http://localhost:15673) (User/Pass: `guest` / `guest`)

---

### Paso 2: Ejecutar las Pruebas Automatizadas (13 Tests)
Ejecuta la suite completa de pruebas unitarias y de integración end-to-end:
```powershell
cd pedidos360-backend
.\mvnw.cmd test
```
> Resultado esperado: `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0` -> `BUILD SUCCESS`.

---

### Paso 3: Iniciar el Backend
Inicia la aplicación en el puerto `8080`:
```powershell
cd pedidos360-backend
.\mvnw.cmd spring-boot:run
```
Verifica que el servicio esté operativo:
```powershell
curl http://localhost:8080/api/public/health
# Respuesta: {"status":"UP","app":"pedidos360-backend"}
```

---

## 6. Estructura del Código Fuente

```text
src/main/java/com/pedidos360
├── Pedidos360Application.java
├── config/
│   └── DataSeeder.java
├── controller/
│   ├── HealthController.java
│   └── PedidosController.java
├── entity/
│   └── Pedido.java
├── rabbitmq/
│   ├── BusinessTopicConsumer.java      # Consumidor Topic Exchange (pedidos.*)
│   ├── BusinessTopicEvent.java         # Modelo de evento Topic
│   ├── DeadLetterConsumer.java         # Consumidor de las 3 DLQ con auditoría x-death
│   ├── NotificationConsumer.java       # Consumidor de notifications.queue
│   ├── NotificationEvent.java          # Modelo de notificación
│   ├── OrderConsumer.java              # Consumidor de orders.queue con reintentos
│   ├── OrderEvent.java                 # Modelo de pedido
│   ├── OrderProducer.java              # Productor Direct y Topic
│   ├── RabbitAdminController.java      # API REST de administración (EP3 Criterio 6)
│   ├── RabbitAdminService.java         # Servicio de alto nivel RabbitAdmin (EP3 Criterio 7)
│   ├── RabbitMQConfig.java             # Topología centralizada: 3 colas, 3 DLQs (EP3/EP4)
│   ├── RabbitMQController.java         # Endpoints de pruebas públicas y status
│   └── dto/
│       ├── CreateBindingDto.java       # DTO con validaciones Jakarta
│       ├── CreateExchangeDto.java      # DTO con validaciones Jakarta
│       └── CreateQueueDto.java         # DTO con validaciones Jakarta (EP3 Criterio 8)
├── repository/
│   └── PedidoRepository.java
└── security/
    ├── AudienceValidator.java
    ├── IssuerValidator.java
    └── SecurityConfig.java
```
