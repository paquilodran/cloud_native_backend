# Pedidos360 — Informe técnico

**Stack:** Angular 19 + MSAL · Spring Boot 3 · Microsoft Entra ID · JWT  
**Repositorios:** `pedidos360-frontend` · `pedidos360-backend`

## 1. Arquitectura

Pedidos360 es un dashboard Cloud Native de gestión de pedidos. El usuario inicia sesión en el frontend (SPA). Microsoft Entra ID autentica y emite un JWT. El interceptor de Angular adjunta ese token en cada request. Spring Boot actúa como resource server: valida firma, issuer y audience. Solo entonces responde `GET` / `POST /api/pedidos`.

```
[Navegador Angular] → [MSAL / Entra ID] → [JWT]
        ↓
[Interceptor Authorization: Bearer]
        ↓
[Spring Boot + JPA] → [H2 local / PostgreSQL]
```

No hay sesión de servidor. La API es stateless.

## 2. Componentes

**Frontend (Angular).** Tres pantallas: Inicio (KPIs y flujo), Pedidos (tabla, alta, filtros) y Login Microsoft. MSAL redirige a Entra ID. `auth.guard` protege `/pedidos`. `jwt.interceptor` obtiene un access token con el scope `api://<API_CLIENT_ID>/access_as_user` y lo envía al backend.

**Backend (Spring Boot).** Un controlador con dos métodos: listar y crear pedidos. JPA + `PedidoRepository`. `SecurityConfig` registra un `JwtDecoder` (JWKS de Entra), valida issuer v1/v2 y audience (`client-id` o `api://client-id`). `@Secured("ROLE_AUTHENTICATED")` exige un JWT válido. `/api/public/health` queda abierto para comprobar que el servicio está arriba.

**Microsoft Entra ID.** Dos app registrations en el mismo tenant:

| App | Uso |
|---|---|
| Pedidos360-Web | SPA, redirect `http://localhost:4200` |
| Pedidos360-API | Resource server, URI `api://3609dffc-…`, scope `access_as_user` |

Tenant: `4531cbe0-83c7-406d-a972-e6302b1fb7d1`. Consentimiento de administrador concedido.

## 3. Flujo de autenticación

1. El usuario pulsa **Continuar con Microsoft**.
2. MSAL hace redirect a `login.microsoftonline.com/<tenant>`.
3. Entra ID autentica y vuelve a `http://localhost:4200` (Authorization Code + PKCE).
4. MSAL guarda tokens en `localStorage`.
5. Al llamar `/api/pedidos`, el interceptor ejecuta `acquireTokenSilent` y agrega `Authorization: Bearer <access_token>`.
6. Spring Boot descarga las claves JWKS, valida `iss`, `aud`, `exp` y responde 200 + JSON. Sin token o con token inválido responde **401**.

## 4. Instrucciones de deploy (copy-paste)

**Local — frontend**

```bash
cd pedidos360-frontend
npm install
npm start
```

**Local — backend** (JDK 17 + Maven)

```powershell
$env:AZURE_TENANT_ID="4531cbe0-83c7-406d-a972-e6302b1fb7d1"
$env:AZURE_API_CLIENT_ID="3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
$env:AZURE_API_APP_ID_URI="api://3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
cd pedidos360-backend
mvn spring-boot:run
```

**Azure (opcional)**

```bash
cd pedidos360-backend && mvn -DskipTests package
# JAR: target/pedidos360-backend-1.0.0.jar → App Service / VM

cd pedidos360-frontend && npm run build
# Subir dist/pedidos360-frontend/browser
```

En producción: mismas variables de entorno, `SPRING_PROFILES_ACTIVE=postgres`, `CORS_ALLOWED_ORIGINS` y redirect URI HTTPS de la SPA en Entra ID.

## 5. Evidencias (adjuntar)

- Tenant Entra + 2 app registrations  
- Usuario test / admin  
- Angular en `http://localhost:4200` y login Microsoft  
- Token en `localStorage` y `Authorization: Bearer` en consola  
- `GET /api/pedidos` sin token → 401 · con JWT → 200  
- Terminal `mvn spring-boot:run`
