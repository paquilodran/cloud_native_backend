# Pedidos360 — Backend

API REST Spring Boot: listar y crear pedidos, con validación JWT de Azure AD.

## Stack

- Spring Boot 3.4 + Java 17
- Spring Security OAuth2 Resource Server (Nimbus JOSE)
- JPA + H2 (local) / PostgreSQL (Azure o Docker)
- 2 endpoints: `GET /api/pedidos` y `POST /api/pedidos`

## Requisitos

- JDK 17+
- Maven 3.9+
- Tenant Azure AD (apps `Pedidos360-API` y `Pedidos360-Web`)
- Opcional: Docker Desktop (PostgreSQL)

## Setup local (copy-paste)

```bash
# 1) Variables de Azure AD (PowerShell)
$env:AZURE_TENANT_ID="TU-TENANT-ID"
$env:AZURE_API_CLIENT_ID="CLIENT-ID-DE-PEDIDOS360-API"
$env:AZURE_API_APP_ID_URI="api://CLIENT-ID-DE-PEDIDOS360-API"

# 2) Arrancar (H2 en memoria, datos demo incluidos)
mvn spring-boot:run
```

Probar salud (sin token):

```bash
curl http://localhost:8080/api/public/health
```

## Evidencias JWT (Postman / Thunder Client)

Importar `postman/Pedidos360.postman_collection.json` o usar `http/pedidos.http`.

| Request | Token | Resultado esperado |
|---|---|---|
| `GET /api/public/health` | no | **200** `{"status":"UP"}` |
| `GET /api/pedidos` | no | **401 Unauthorized** |
| `GET /api/pedidos` | `Bearer token-invalido` | **401 Unauthorized** |
| `GET /api/pedidos` | JWT válido de Azure AD | **200** + JSON de pedidos |
| `POST /api/pedidos` | JWT válido | **201** + pedido creado |

El access token se obtiene después del login en Angular (consola del browser: `Authorization: Bearer ...`) o desde https://jwt.ms para inspeccionarlo.

> Azure AD emite **401** si el JWT falta o es inválido (no se autenticó). **403** aparece si el token es válido pero no tiene autoridad; este proyecto asigna `ROLE_AUTHENTICATED` a todo JWT válido.

## PostgreSQL local

```bash
docker compose up -d
$env:SPRING_PROFILES_ACTIVE="postgres"
mvn spring-boot:run
```

## Deploy en Azure (App Service o VM)

```bash
mvn -DskipTests package
# JAR: target/pedidos360-backend-1.0.0.jar
```

Variables de entorno en Azure:

```
AZURE_TENANT_ID=...
AZURE_API_CLIENT_ID=...
AZURE_API_APP_ID_URI=api://...
SPRING_PROFILES_ACTIVE=postgres
DB_HOST=tu-servidor.postgres.database.azure.com
DB_PORT=5432
DB_NAME=pedidos360_db
DB_USERNAME=...
DB_PASSWORD=...
CORS_ALLOWED_ORIGINS=https://TU-FRONTEND
```

VM Ubuntu:

```bash
sudo apt update && sudo apt install -y openjdk-17-jre
java -jar pedidos360-backend-1.0.0.jar
```

App Service: configurar las mismas variables y desplegar el JAR.

## Azure AD (backend app)

1. App registration **Pedidos360-API**
2. Expose an API → Application ID URI `api://<client-id>`
3. Add scope `access_as_user`
4. Copiar Tenant ID y Application (client) ID a las variables de entorno

## Estructura

```
src/main/java/com/pedidos360
  controller/PedidosController.java   GET + POST
  entity/Pedido.java
  repository/PedidoRepository.java
  security/SecurityConfig.java        JWT + @Secured
```
