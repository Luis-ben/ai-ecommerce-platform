# Java backend

Runtime requirement: Java 21.

Spring Boot 3 + Spring AI implementation of the shared shop contract. Production uses PostgreSQL, signed expiring tokens, BCrypt passwords, durable catalog/cart/order/support/settings tables, and optional OpenAI-compatible chat/RAG. The `local-demo` profile is intentionally a small in-memory fixture for UI smoke checks only.

## Production

Set the variables in `.env.example` through the deployment secret store. In particular, provide `SHOP_JAVA_DATABASE_*`, a non-default `SHOP_AUTH_SECRET`, `SHOP_BOOTSTRAP_ADMIN_*`, and `SHOP_CORS_ORIGINS`. Keep `SHOP_SEED_DEMO=false` in production.

```powershell
$env:MAVEN_OPTS='-Dmaven.repo.local=D:\work\m2'
D:\java\Maven\apache-maven-3.8.9\bin\mvn.cmd -f D:\work\zeshop_java\pom.xml spring-boot:run
```

## Local demo

```powershell
$env:SPRING_PROFILES_ACTIVE='local-demo'
$env:SHOP_JAVA_PORT='8083'
java -jar target\shop-server-java-0.1.0.jar
```

OpenAI-compatible settings are read from `JEV_*` and `MAIN_MODEL_*` environment variables. Provider credentials are never stored in the admin settings table.

The production API creates its commerce tables on startup for the first deployment; run the database backup/migration process before applying schema changes in a managed environment. `mvn -DskipTests package` validates compilation, while integration tests require a reachable PostgreSQL/pgvector service.