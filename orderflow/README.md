# OrderFlow

[![CI](https://github.com/<TU_USUARIO>/orderflow/actions/workflows/ci.yml/badge.svg)](https://github.com/<TU_USUARIO>/orderflow/actions/workflows/ci.yml)

Backend de gestión de pedidos construido como **portfolio técnico**: un proyecto pequeño en superficie pero deliberado en cada decisión, pensado para evidenciar criterio senior en Java (no volumen de código, sino *cómo* está hecho).

Stack objetivo: **Java 21 · Spring Boot 3 · arquitectura hexagonal · PostgreSQL · Testcontainers · Docker**.

> Estado actual: **slices 0–6 completos**. API REST segura (JWT) sobre PostgreSQL, outbox transaccional, caché, resiliencia y observabilidad; lista para desplegar en Render. El dominio se verifica con `javac` + tests; las capas Spring se validan con `mvn verify` (Testcontainers).

---

## Por qué este proyecto demuestra experiencia

| Señal de seniority | Dónde se ve |
|---|---|
| Arquitectura hexagonal (puertos y adaptadores) | El dominio no depende de ningún framework; define `OrderRepository` como puerto |
| Modelado de dominio rico (DDD táctico) | `Order` es un *aggregate root* que custodia todas las invariantes; nadie muta `OrderLine` por fuera |
| Java 21 moderno | `record` para value objects, `sealed interface` para eventos, `switch` exhaustivo, text blocks, `Clock` inyectado |
| Evitar *primitive obsession* | IDs tipados (`OrderId`, `CustomerId`…) en vez de `UUID`/`String` sueltos |
| Dinero bien tratado | `Money` con `BigDecimal`, control de divisa y redondeo bancario — nunca `double` |
| Testabilidad por diseño | tiempo inyectado vía `Clock`; tests deterministas con JUnit 5 + AssertJ |
| Errores como parte del dominio | jerarquía `DomainException` con `code()` estable para el borde HTTP |
| Concurrencia y transacciones | caso de uso `@Transactional` (read-modify-write atómico) + bloqueo optimista con `@Version` |
| Mensajería fiable | *transactional outbox* + relay con entrega al menos una vez; el broker es un detalle aislado |
| Seguridad | JWT stateless (resource server + emisión), roles → authorities, rutas públicas mínimas |
| Resiliencia | Resilience4j (retry + circuit breaker) en la integración con el broker |
| Observabilidad y DX | Actuator/Prometheus, OpenAPI/Swagger, Docker + compose + blueprint de Render |
| Build profesional | Maven multi-módulo con BOMs, versiones centralizadas, módulo de dominio sin dependencias de producción |

---

## Qué mirar primero (guía para revisor técnico)

Recorrido de ~10 minutos por los ficheros que mejor muestran el criterio:

1. `orderflow-domain/.../order/model/Order.java` — agregado que custodia todas las invariantes.
2. `orderflow-domain/.../order/model/Money.java` — value object con divisa y redondeo bancario.
3. `orderflow-application/.../service/ConfirmOrderService.java` — frontera transaccional + evicción de caché.
4. `orderflow-infrastructure/.../persistence/OrderRepositoryAdapter.java` — puerto del dominio sobre JPA con bloqueo optimista.
5. `orderflow-infrastructure/.../order/outbox/` — outbox + relay con entrega al menos una vez.
6. `orderflow-infrastructure/.../security/SecurityConfig.java` — JWT stateless (emisión + validación).
7. `orderflow-bootstrap/.../OrderApiIntegrationTest.java` — prueba de extremo a extremo con Testcontainers.

Diagramas (componentes, secuencia y outbox) y decisiones de diseño: **[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)**.
Con la app en marcha, la API es explorable en **`/swagger-ui.html`** (botón *Authorize* para pegar el JWT).

## Arquitectura

Flujo de dependencias hacia adentro: la infraestructura conoce el dominio, **nunca al revés**.

```
        ┌──────────────────────────────────────────────┐
        │              orderflow-bootstrap             │  Spring Boot main + config
        └──────────────────────────────────────────────┘
                          │ depende de
        ┌──────────────────────────────────────────────┐
        │            orderflow-infrastructure          │  Adaptadores:
        │  web (REST) · persistence (JPA) · security   │  implementan los puertos
        │  messaging · cache                           │
        └──────────────────────────────────────────────┘
                          │ implementa puertos de
        ┌──────────────────────────────────────────────┐
        │             orderflow-application            │  Casos de uso (orquestación,
        │  CreateOrderUseCase, ConfirmOrderUseCase…    │  transacciones, puertos in/out)
        └──────────────────────────────────────────────┘
                          │ usa
        ┌──────────────────────────────────────────────┐
        │               orderflow-domain               │  Java puro, sin frameworks
        │  Order (aggregate) · Money · OrderStatus     │  Java puro, sin frameworks
        │  OrderEvent (sealed) · OrderRepository (port)│
        └──────────────────────────────────────────────┘
```

### Módulos

- **`orderflow-domain`** ✅ — modelo de negocio puro. Cero dependencias de producción.
- **`orderflow-application`** ✅ — casos de uso y puertos in/out (depende del dominio; usa `spring-tx` solo para declarar transacciones).
- **`orderflow-infrastructure`** ✅ — adaptadores REST, persistencia JPA + Flyway, outbox, seguridad JWT, caché y resiliencia.
- **`orderflow-bootstrap`** ✅ — arranque Spring Boot, wiring, configuración por entorno.

---

## El dominio de un vistazo

- `Order` — *aggregate root*. Único punto de entrada para mutar líneas y estado. Garantiza: solo se añaden líneas en `DRAFT`, no se confirma vacío, las transiciones siguen la máquina de estados, todas las líneas comparten divisa. Emite `DomainEvent`s que la capa de aplicación recoge con `pullDomainEvents()`.
- `OrderStatus` — máquina de estados `DRAFT → CONFIRMED → PAID → SHIPPED`, con `CANCELLED` como salida; las transiciones legales viven en un único sitio.
- `Money` — value object inmutable con divisa y redondeo `HALF_EVEN`; rechaza mezclar monedas.
- `OrderEvent` — jerarquía `sealed`: los consumidores pueden hacer `switch` exhaustivo verificado por el compilador.
- `OrderRepository` — puerto de salida; lo implementará el adaptador JPA.

---

## Hoja de ruta (slices verticales)

Cada slice atraviesa todas las capas y deja algo ejecutable, en vez de construir capas horizontales a medias.

- [x] **0. Dominio + build + CI** — aggregate, value objects, eventos, puerto, tests; workflow de GitHub Actions en verde.
- [x] **1. Crear pedido (vertical completo)** — caso de uso → adaptador JPA (Postgres + Flyway) → REST `POST /api/orders` → test de integración con Testcontainers; `Dockerfile` + `docker-compose` listos.
- [x] **2. Confirmar y consultar** — `POST /api/orders/{id}/confirm`, `GET /api/orders/{id}`, frontera transaccional en el caso de uso y bloqueo optimista (`@Version`).
- [x] **3. Eventos** — *transactional outbox* (eventos guardados en la misma transacción) + relay programado con entrega al menos una vez.
- [x] **4. Seguridad** — Spring Security 6 stateless, login que emite JWT (HS256) y resource server que lo valida; roles a authorities.
- [x] **5. Resiliencia y caché** — caché Caffeine en la consulta (con evicción al confirmar) y Resilience4j (retry + circuit breaker) en la salida al broker.
- [x] **6. Observabilidad y entrega** — Actuator + métricas Prometheus, OpenAPI/Swagger UI, `Dockerfile` + `docker-compose` + `render.yaml` para desplegar.

### Extras (más allá de la hoja de ruta)
- [x] **Listado paginado** — `GET /api/orders?page=&size=&status=` con separación lectura/escritura (`OrderQueryPort`).
- [x] **Broker Kafka real** — `MessageBroker` con perfil `kafka` (relay publica en Kafka); por defecto, log.
- [x] **Cobertura de tests** — dominio (`Order`, `Money`), aplicación (create/confirm/get) e integración (API completa).

---

## Hosting y revisión

- **Código (ahora):** GitHub. Cada push dispara CI (`.github/workflows/ci.yml`)
  que compila y pasa los tests — la insignia de arriba refleja el estado.
- **App viva (desde el slice 1):** Render (web service + PostgreSQL gratis,
  redeploy automático desde GitHub). Pasos y `Dockerfile` listos en
  [`DEPLOYMENT.md`](DEPLOYMENT.md).

## Cómo ejecutar y probar

```bash
# levanta Postgres + la app
docker compose up --build      # http://localhost:8080  (Swagger: /swagger-ui.html)

# o solo los tests (requiere JDK 21 + Maven 3.9; Testcontainers necesita Docker)
mvn -q verify
```

La API está protegida con JWT. Primero obtén un token (usuario demo `demo/demo123`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
```

Crear, consultar y confirmar un pedido (con el token):

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{
        "customerId": "11111111-1111-1111-1111-111111111111",
        "currency": "EUR",
        "lines": [
          {"productId":"22222222-2222-2222-2222-222222222222","sku":"SKU-1","unitPrice":19.99,"quantity":2}
        ]
      }'
# -> 201 Created, header Location: /api/orders/{id}, body {"orderId":"..."}

curl http://localhost:8080/api/orders/{id}            -H "Authorization: Bearer $TOKEN"   # 200 DRAFT
curl -X POST http://localhost:8080/api/orders/{id}/confirm -H "Authorization: Bearer $TOKEN"  # 200 CONFIRMED
```

Sin token, `/api/orders/**` responde `401`. El módulo de dominio se compila también
con solo `javac` por no tener dependencias de producción.

El módulo de dominio se compila también con solo `javac` por no tener dependencias de producción.

---

## Convenciones

- Inglés en código y commits; español en esta documentación de proyecto.
- Commits estilo *Conventional Commits* (`feat:`, `test:`, `refactor:`…).
- Un value object/aggregate por fichero; tests que se leen como especificaciones.
