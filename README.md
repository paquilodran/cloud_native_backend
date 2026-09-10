# Pedidos360 — Backend (Spring Boot + JWT)

**Asignatura:** DSY1107 Desarrollo Cloud Native I  
**Evaluación:** Parcial N° 1 — Encargo 2025  
**Repositorio:** https://github.com/paquilodran/cloud_native_backend  
**Frontend:** https://github.com/paquilodran/cloud_native_frontend

Informe del **componente backend**: API REST Spring Boot que actúa como resource server / BFF. Valida el JWT emitido por Microsoft Entra ID y solo entonces permite consumir los pedidos.

---

## 1. Objetivo

Cumplir el indicador de la pauta (40%):

- Validar **issuer** y **audience** contra el IDaaS
- Verificar **firma** (JWKS) y **vigencia** (`exp`)
- Autorizar por **scope/rol** (`SCOPE_access_as_user`)
- Responder **401** si el token falta o es inválido y **403** si no tiene el scope

---

## 2. Arquitectura (lado servidor)

```
Authorization: Bearer <jwt>
    → Spring Security OAuth2 Resource Server
    → JwtDecoder (JWKS Entra ID)
    → IssuerValidator + AudienceValidator + JwtTimestampValidator
    → @PreAuthorize("hasAuthority('SCOPE_access_as_user')")
    → PedidoRepository (JPA)
```

API stateless: no hay sesión HTTP. Cada request se valida de nuevo.

---

## 3. Stack y estructura

- Java 17 + Spring Boot 3.4
- `spring-boot-starter-oauth2-resource-server` (Nimbus JOSE)
- Spring Data JPA
- H2 (perfil default) / PostgreSQL (perfil `postgres`)

```
src/main/java/com/pedidos360
  controller/PedidosController.java    GET + POST /api/pedidos
  controller/HealthController.java     GET /api/public/health
  entity/Pedido.java
  repository/PedidoRepository.java
  security/SecurityConfig.java         JWT + method security
  security/IssuerValidator.java
  security/AudienceValidator.java
src/main/resources/application.yml
```

Endpoints:

| Método | Ruta | Seguridad |
|---|---|---|
| GET | `/api/public/health` | público |
| GET | `/api/pedidos` | JWT + scope `access_as_user` |
| POST | `/api/pedidos` | JWT + scope `access_as_user` |

`.gitignore` excluye `target/` y `.env`.

---

## 4. Validación JWT

`SecurityConfig` construye un `NimbusJwtDecoder` con:

```
https://login.microsoftonline.com/<tenant>/discovery/v2.0/keys
```

- **Firma:** claves JWKS de Entra ID  
- **Issuer:** `https://login.microsoftonline.com/<tenant>/v2.0` o `https://sts.windows.net/<tenant>/`  
- **Audience:** client id de Pedidos360-API o `api://<client-id>`  
- **Expiración:** `JwtTimestampValidator`  
- **Scope:** el claim `scp` se mapea a `SCOPE_access_as_user`; `@PreAuthorize` lo exige

App registration **Pedidos360-API**  
Tenant: `4531cbe0-83c7-406d-a972-e6302b1fb7d1`  
Client ID: `3609dffc-ca49-4133-a6e5-2dbf3ba2a120`  
URI: `api://3609dffc-ca49-4133-a6e5-2dbf3ba2a120`  
Scope: `access_as_user`

---

## 5. Persistencia

Entidad `Pedido` (id, nombre, cliente, estado, total, fecha) y `PedidoRepository` (JPA).  
Perfil `h2`: base en memoria con seed de 3 pedidos.  
Perfil `postgres`: `docker compose up -d` y `SPRING_PROFILES_ACTIVE=postgres`.

---

## 6. Puesta en marcha

JDK 17 + Maven 3.9+.

```powershell
$env:AZURE_TENANT_ID="4531cbe0-83c7-406d-a972-e6302b1fb7d1"
$env:AZURE_API_CLIENT_ID="3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
$env:AZURE_API_APP_ID_URI="api://3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
mvn spring-boot:run
```

```bash
curl http://localhost:8080/api/public/health
```

---

## 7. Evidencias de este repositorio

Importar `postman/Pedidos360.postman_collection.json` o `http/pedidos.http`.

| Request | Token | Resultado |
|---|---|---|
| `GET /api/public/health` | no | **200** |
| `GET /api/pedidos` | no | **401** |
| `GET /api/pedidos` | token inválido | **401** |
| `GET /api/pedidos` | JWT Entra válido + scope | **200** + JSON |
| `POST /api/pedidos` | JWT Entra válido + scope | **201** |
| `GET /api/pedidos` | JWT válido sin `access_as_user` | **403** |

El access token se copia desde el frontend (Inicio → claims, o header que adjunta `MsalInterceptor`). Inspección: https://jwt.ms

El login MSAL y las vistas viven en el repositorio **frontend**.
