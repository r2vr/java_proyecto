# Despliegue y revisión

Dos planos distintos: **revisar el código** (ya disponible) y **ver la app viva**
(a partir del slice 1, cuando exista `orderflow-bootstrap` y produzca un JAR).

---

## 1. Revisar el código — GitHub (ahora)

GitHub es donde irás siguiendo el avance: historial de commits y, en cada push, el
workflow de CI (`.github/workflows/ci.yml`) compila y ejecuta los tests. Un check
verde por commit = build sano antes de desplegar nada.

```bash
# desde la raíz del proyecto, una sola vez:
git init -b main
git add .
git commit -m "feat: capa de dominio (Order aggregate) + build multimódulo"

# crea un repo vacío en github.com (sin README ni .gitignore) y luego:
git remote add origin git@github.com:<TU_USUARIO>/orderflow.git
git push -u origin main
```

En el README, sustituye `<TU_USUARIO>` en la insignia de CI para que se vea el
estado del build directamente en la portada del repo.

---

## 2. Ver la app viva — Render (desde el slice 1)

Render despliega gratis desde GitHub y ofrece PostgreSQL gratis. El `Dockerfile`
y el `docker-compose.yml` ya están en la raíz del repo (slice 1), así que el
plan es:

1. **`orderflow-bootstrap`** genera el JAR ejecutable (`target/orderflow.jar`).
2. Render construye con el `Dockerfile` de la raíz (no tiene runtime Java nativo,
   pero sí soporta Docker).
3. Cada `git push` a `main` redespliega automáticamente.

> El `Dockerfile` de referencia ya incluido en la raíz es este (multi-stage,
> compila con Maven y deja solo el runtime):

```dockerfile
# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY orderflow-domain/pom.xml orderflow-domain/
# (añadir aquí los pom.xml del resto de módulos conforme se creen)
RUN mvn -q -B dependency:go-offline
COPY . .
RUN mvn -q -B -DskipTests package

# ---- run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/orderflow-bootstrap/target/*.jar app.jar
# Ajuste de memoria: el free tier de Render da 512 MB y la JVM consume bastante.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

### Pasos en Render

Opción rápida: **Blueprint**. En Render, *New → Blueprint* apuntando al repo usa
el `render.yaml` de la raíz (crea el web service Docker + el PostgreSQL gratis y
genera `JWT_SECRET`). Solo queda fijar `SPRING_DATASOURCE_URL` en formato JDBC
(ver nota más abajo).

Opción manual:

1. **New → Web Service**, conecta el repo de GitHub.
2. Runtime: **Docker** (detecta el `Dockerfile`).
3. **New → PostgreSQL** (plan free) y copia la *Internal Database URL*.
4. En el Web Service, variables de entorno:
   `SPRING_DATASOURCE_URL` (en forma `jdbc:postgresql://<host>:<port>/<db>`),
   `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, y `JWT_SECRET`
   (cadena de 32+ caracteres).
5. Deploy. La URL pública será `https://orderflow-xxxx.onrender.com`
   (Swagger en `/swagger-ui.html`, health en `/actuator/health`).

> Notas del free tier: el servicio se duerme tras 15 min de inactividad
> (primer arranque ~30 s) y la BD gratuita tiene límites de retención —
> suficiente para portfolio/demo, no para producción.

### Alternativas equivalentes
- **Railway**: misma idea con Postgres integrado; va con crédito mensual de 5 $.
- **Google Cloud Run** + Postgres gestionado (Neon/Supabase free): no se duerme
  igual y autoescala; algo más de configuración inicial.
