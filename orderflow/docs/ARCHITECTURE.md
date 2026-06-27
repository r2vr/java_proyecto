# Arquitectura

OrderFlow sigue **arquitectura hexagonal** (puertos y adaptadores) con el flujo de
dependencias siempre hacia el dominio. Cuatro módulos Maven imponen esa dirección
en tiempo de compilación.

## Vista de componentes

```mermaid
flowchart TB
  client([Cliente HTTP])

  subgraph INFRA[orderflow-infrastructure]
    web[REST controllers + Security JWT]
    jpa[JPA adapter\nOrderRepository / OrderQueryPort]
    pub[OutboxDomainEventPublisher]
    relay[OutboxRelay - scheduled]
    brk[MessageBroker\nlog / Kafka]
  end

  subgraph APP[orderflow-application]
    uc[Casos de uso + puertos\nCreate / Confirm / Get / List]
  end

  subgraph DOM[orderflow-domain]
    dom[Order aggregate\nMoney · OrderStatus · OrderEvent]
  end

  db[(PostgreSQL)]

  client --> web --> uc --> dom
  uc -. OrderRepository .-> jpa
  uc -. OrderQueryPort .-> jpa
  uc -. DomainEventPublisher .-> pub
  jpa --> db
  pub --> db
  relay --> db
  relay --> brk
```

El dominio no conoce Spring, JPA ni HTTP. La aplicación define *puertos*; la
infraestructura los *implementa*. El broker (log o Kafka) se elige por perfil.

## Crear pedido (camino feliz)

```mermaid
sequenceDiagram
  autonumber
  participant C as Cliente
  participant Ctrl as OrderController
  participant UC as CreateOrderService
  participant Dom as Order (agregado)
  participant Repo as OrderRepository (JPA)
  participant Out as Outbox (misma TX)

  C->>Ctrl: POST /api/orders (Bearer JWT)
  Ctrl->>UC: handle(command)
  UC->>Dom: create() + addLine()
  Dom-->>UC: order + evento OrderCreated
  UC->>Repo: save(order)
  UC->>Out: publish(events)
  Note over Repo,Out: ambos en la misma transacción
  UC-->>Ctrl: orderId
  Ctrl-->>C: 201 Created (Location)
```

## Relay del outbox (entrega al menos una vez)

```mermaid
flowchart LR
  A[Tabla outbox\npublished = false] -->|poll cada 2s| B[OutboxRelay]
  B --> C{EventBroadcaster\nretry + circuit breaker}
  C -->|ok| D[MessageBroker\nlog / Kafka]
  C -->|fallo| A
  D --> E[marcar published = true]
```

Si el broker falla, la entrada queda sin publicar y se reintenta en el siguiente
ciclo: nunca se pierde un evento confirmado.

## Decisiones de diseño (resumen tipo ADR)

| Decisión | Por qué |
|---|---|
| Dominio sin frameworks | Reglas de negocio testables y estables; los frameworks son detalles reemplazables |
| Value objects (`Money`, IDs tipados) | Eliminan *primitive obsession* y bugs de divisa/redondeo |
| `OrderRepository` (escritura) vs `OrderQueryPort` (lectura) | Separación CQRS: las consultas evolucionan sin tocar el agregado |
| `@Transactional` en el caso de uso | Frontera transaccional explícita para el read-modify-write de confirmación |
| Bloqueo optimista (`@Version`) | Seguridad ante escrituras concurrentes sin bloqueos pesimistas |
| Transactional outbox | Atomicidad evento↔dato; desacopla del broker |
| Broker por perfil (`!kafka` / `kafka`) | Arranque y tests sin infraestructura; Kafka real cuando se quiere |
| JWT stateless | API escalable horizontalmente sin sesión de servidor |
