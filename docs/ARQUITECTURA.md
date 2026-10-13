# Arquitectura de TaskFlow

Documento breve para un desarrollador nuevo que llega al proyecto TaskFlow. Contiene las capas, flujo de una petición clave, dónde viven las reglas de negocio, cómo funciona la seguridad con JWT y cómo están organizados los tests.

---

## Capas y paquetes

- Capa de presentación / API
  - Controladores REST en `src/main/java/com/taskflow/controller/`.
  - Ejemplo: `TaskController` — `src/main/java/com/taskflow/controller/TaskController.java`.
  - DTOs en `src/main/java/com/taskflow/dto/` (por ejemplo `TaskRequest` — `src/main/java/com/taskflow/dto/TaskRequest.java`).

- Capa de servicio (lógica de aplicación)
  - Servicios en `src/main/java/com/taskflow/service/`.
  - Ejemplo: `TaskService` — `src/main/java/com/taskflow/service/TaskService.java`.
  - Orquestan validaciones de flujo, transacciones y llamadas a repositorios.

- Capa de persistencia / repositorios
  - Repositories Spring Data en `src/main/java/com/taskflow/repository/`.
  - Ejemplo: `TaskRepository` — `src/main/java/com/taskflow/repository/TaskRepository.java`.

- Dominio / modelos
  - Entidades JPA en `src/main/java/com/taskflow/model/`.
  - Ejemplo: `Task` — `src/main/java/com/taskflow/model/Task.java`, `Project` — `src/main/java/com/taskflow/model/Project.java`.

- Mappers / DTO mapping
  - Clases en `src/main/java/com/taskflow/mapper/`.
  - Ejemplo: `TaskMapper` — `src/main/java/com/taskflow/mapper/TaskMapper.java`.

- Configuración
  - Seguridad, CORS y beans en `src/main/java/com/taskflow/config/`.
  - Ejemplo: `SecurityConfig` — `src/main/java/com/taskflow/config/SecurityConfig.java`.
  - Data seeding en `src/main/java/com/taskflow/config/DataSeeder.java`.

---

## Flujo completo: POST /projects/{projectId}/tasks

1. El cliente hace POST a `/projects/{projectId}/tasks` con el `TaskRequest` (`src/main/java/com/taskflow/dto/TaskRequest.java`).
2. `TaskController.createTask` (`src/main/java/com/taskflow/controller/TaskController.java`) valida el cuerpo con `@Valid` y comprueba que el proyecto exista llamando a `ProjectService.buscarPorId(projectId)`; si no existe lanza `ProjectNotFoundException` (404).
3. El controlador llama a `TaskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
4. Dentro de `TaskService.crear`, el DTO se convierte en entidad nueva con `TaskMapper.aEntidadNueva(request, projectId)` (`src/main/java/com/taskflow/mapper/TaskMapper.java`), que delega a la factory de dominio `Task.crear(...)` para aplicar invariantes.
5. `TaskService.crear` persiste la entidad con `TaskRepository.save(...)` (`src/main/java/com/taskflow/repository/TaskRepository.java`), que asigna el `id` en la base de datos.
6. De vuelta en el controlador, se construye el header `Location` y se responde `201 Created` con el cuerpo `TaskMapper.aResponse(creada)` (`src/main/java/com/taskflow/dto/TaskResponse.java`).

Notas: las transacciones se configuran vía Spring (`@Transactional`) en servicios o métodos concretos.

---

## Dónde viven las reglas de negocio

- Reglas intrínsecas a la entidad (invariantes de dominio) están en la entidad JPA, por ejemplo en `Task` (`src/main/java/com/taskflow/model/Task.java`). Ejemplos: fábricas tipo `crear(...)`, `setStatus(...)`, comprobaciones como `estaVencida()`.
- Reglas de aplicación (flujos entre entidades, validaciones que requieren acceso a repositorios o políticas) en la capa de servicio, por ejemplo `TaskService` (`src/main/java/com/taskflow/service/TaskService.java`).
- Comprobaciones de autorización de negocio (por ejemplo, "el usuario puede crear tareas en este proyecto") pueden residir en beans especiales o componentes de seguridad como `ProjectSecurity` (en `src/main/java/com/taskflow/config/` o `src/main/java/com/taskflow/security/` dependiendo de la organización).

Principio: mantener reglas que dependen únicamente del estado de un objeto dentro del objeto; mantener coordinación y orquestación en servicios.

---

## Seguridad con JWT

- Configuración principal en `SecurityConfig` (`src/main/java/com/taskflow/config/SecurityConfig.java`).
- Flujo básico:
  1. El cliente obtiene un token JWT firmando credenciales contra el endpoint de autenticación.
  2. Para llamadas protegidas, el cliente envía `Authorization: Bearer <token>`.
  3. Un filtro/chain de seguridad (configurado por `SecurityConfig`) intercepta la petición, extrae y valida el JWT, reconstruye el `Authentication` y lo pone en el `SecurityContext`.
  4. Spring Security decide acceso a endpoints por reglas URL-based (en `SecurityConfig`) y por anotaciones `@PreAuthorize` o checks adicionales en servicios/beans (por ejemplo `ProjectSecurity`).
- Secretos y parámetros se toman de `application.yml`/perfil docker (`application-docker.yml`) o variables de entorno (ej. `JWT_SECRET`).
- Recomendación: revisar `SecurityConfig` y las clases de filtro relacionadas para entender dónde se valida y parsea el token.

---

## Organización de los tests

- Tests unitarios: en `src/test/java/com/taskflow/...`. Cubren lógica pura de clases/servicios.
- Tests de capa/slice: `@WebMvcTest` para controladores, `@DataJpaTest` para repositorios.
- Tests de integración: `@SpringBootTest` que levantan el contexto completo. Algunos tests de integración usan Testcontainers con Postgres; ejecutar con `-Ddocker.tests=true`.
- Comandos útiles:
  - Ejecutar todos los tests: `mvn test`.
  - Ejecutar un test específico: `mvn -Dtest=ClassName test` o `mvn -Dtest=ClassName#methodName test`.
  - Perfil de cobertura/quality gate: `mvn verify` (activa JaCoCo/cobertura según configuración del `pom.xml`).

Convención: los tests no deben depender de la data del `DataSeeder`; cada test crea su propio fixture.

---

## Puntos prácticos para empezar rápido

- Ver `README.md` para comandos de ejecución y `pom.xml` para perfiles.
- Revisar `src/main/java/com/taskflow/model/Task.java` para entender las invariantes de dominio.
- Abrir `src/main/java/com/taskflow/service/TaskService.java` para ver el flujo de negocio y transacciones.
- Chequear `src/main/java/com/taskflow/config/SecurityConfig.java` para entender la protección de endpoints y el filtro JWT.

---

Archivos clave (rápida referencia):

- `src/main/java/com/taskflow/controller/TaskController.java`
- `src/main/java/com/taskflow/service/TaskService.java`
- `src/main/java/com/taskflow/repository/TaskRepository.java`
- `src/main/java/com/taskflow/model/Task.java`
- `src/main/java/com/taskflow/mapper/TaskMapper.java`
- `src/main/java/com/taskflow/config/SecurityConfig.java`
- `src/main/java/com/taskflow/config/DataSeeder.java`
- `pom.xml`, `docker-compose.yml`, `README.md`


---

Este documento resume los puntos esenciales para comenzar a desarrollar y entender TaskFlow rápidamente. Para dudas concretas del código, abrir los archivos citados y ejecutar los tests locales con Maven. Las fechas límite se validan en la factory `Task.crear()` (`src/main/java/com/taskflow/model/Task.java`).
