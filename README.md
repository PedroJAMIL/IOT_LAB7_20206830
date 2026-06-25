# Sustentación Arquitectónica — BiciPUCP

## Información del estudiante

- **Curso:** Servicios y Aplicaciones IoT (1TEL05)
- **Alumno:** Peter Minaya
- **Código PUCP:** 20206830
- **Laboratorio:** 7 — Firebase (Auth & Storage) y Ecosistema de Microservicios

---

## 1. Patrón arquitectónico global aplicado

La solución implementada en este laboratorio aplica el patrón **arquitectura de microservicios con descubrimiento centralizado y orquestación (Microservices + Service Discovery + API Gateway/Orchestrator)**.

Justificación de la elección:

- **Descomposición funcional clara.** Cada microservicio tiene una única responsabilidad bien delimitada: `eureka-server` se encarga del descubrimiento, `pucp-validador-service` aloja las reglas de validación de identidad académica y de integridad del candado IoT, y `orquestador-service` actúa como punto único de entrada (API gateway interno) para la aplicación móvil.

- **Bajo acoplamiento y alta cohesión.** El cliente Android no conoce ni necesita conocer la existencia del microservicio validador. Solo se comunica con el orquestador en el puerto 8080. Si mañana se reemplaza el validador por dos servicios separados (uno para alumnos y otro para candados), el cliente móvil no requiere cambios.

- **Service Discovery dinámico.** Eureka (puerto 8761) elimina el acoplamiento de red entre servicios. El orquestador no apunta a `http://localhost:8001`, sino al nombre lógico `pucp-validador-service`. Esto habilita escalamiento horizontal y tolerancia a fallos en producción.

- **Orquestación híbrida.** El orquestador combina dos clientes HTTP distintos (RestTemplate balanceado y Feign declarativo), demostrando que ambos paradigmas pueden coexistir y consumir el mismo servicio descubierto por Eureka.

- **Stateless en todos los servicios.** Ningún microservicio mantiene sesión del usuario; el estado (token IoT, timestamp de aprobación) se persiste en Firebase Firestore desde el lado del cliente.

Este patrón es el más adecuado para BiciPUCP porque la universidad necesita validar centralmente la identidad académica antes de otorgar acceso a la infraestructura IoT, manteniendo la flexibilidad de evolucionar cada componente de forma independiente.

---

## 2. Cumplimiento del principio Stateless en el microservicio validador

El microservicio `pucp-validador-service` cumple estrictamente con la restricción **Stateless** del estándar RESTful por los siguientes motivos:

1. **Ausencia total de sesiones HTTP.** El servicio no mantiene `HttpSession`, no usa cookies, ni ningún mecanismo de identificación entre peticiones. Cada request HTTP es atendida sin depender de requests anteriores.

2. **No persiste información del cliente entre llamadas.** Los endpoints `/validar/alumno/{codigo}` y `/validar/candado/{pin}` reciben todos los datos necesarios como parámetros de ruta. No hay variables de instancia ni atributos estáticos que recuerden quién llamó antes ni qué resultado devolvió.

3. **Lógica puramente funcional.** Las validaciones son funciones puras: el mismo input produce siempre el mismo output. Validar `20230145` siempre retornará `true`; validar `1123` siempre retornará `false`. No hay efectos colaterales ni dependencia del orden de las peticiones.

4. **No hay base de datos ni caché del lado del servidor.** El servicio no consulta ningún repositorio, ningún archivo, ni ningún almacén de datos. Toda la información necesaria viaja en cada solicitud, cumpliendo el principio de auto-contención de las peticiones REST.

5. **Escalabilidad horizontal trivial.** Como consecuencia directa de ser stateless, se podrían levantar N instancias del validador detrás de Eureka y un balanceador de carga podría distribuir las peticiones aleatoriamente sin riesgo de inconsistencias. Cualquier instancia puede responder a cualquier petición sin "memoria" previa.

Esta característica es lo que permite que el orquestador (cliente del validador) pueda usar indistintamente `RestTemplate` con `@LoadBalanced` o un `@FeignClient` declarativo: ambos modelos asumen un servidor sin estado donde cualquier nodo descubierto vía Eureka es intercambiable.