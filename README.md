# IOT_LAB7_20206830 — BiciPUCP
# Video Explicativo: https://www.youtube.com/watch?v=D-FpqO-XXqQ

Laboratorio 7 del curso de Servicios y Aplicaciones IoT (1TEL05) — PUCP.
Sistema de bike sharing con candados IoT validados por microservicios Spring Boot + app Android nativa + Firebase (Auth, Firestore, Storage).

**Alumno:** Peter Minaya
**Código PUCP:** 20206830

## Estructura del monorepo

- `eureka-server/` — Servidor de descubrimiento (puerto 8761)
- `pucp-validador-service/` — Microservicio de reglas + Actuator (puerto 8001)
- `orquestador-service/` — Microservicio orquestador híbrido RestTemplate + Feign (puerto 8080)
- `android-app/` — Aplicación Android nativa en Java (API 34)
- `ARQUITECTURA.md` — Sustentación arquitectónica (Pregunta 3.1)

## Orden de arranque del backend

1. `eureka-server` (8761)
2. `pucp-validador-service` (8001)
3. `orquestador-service` (8080)
4. App Android contra `http://<IP>:8080`

## Cómo correr cada microservicio

```bash
cd <microservicio>
./mvnw spring-boot:run
```

## Stack tecnológico

- **Backend:** Spring Boot 4.1.0, Spring Cloud 2025.1.2, Java 17, Maven
- **Patrones:** Service Discovery (Netflix Eureka), Orquestación, RestTemplate balanceado, OpenFeign declarativo
- **App móvil:** Android (Java, API 34), Retrofit, Firebase Auth + Firestore + Storage, Glide
- **Persistencia:** Firebase Firestore (perfil de usuario) + Firebase Storage (foto del carné)
