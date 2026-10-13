# Copilot instructions for TaskFlow API

This file helps future Copilot sessions understand and operate in this repository.

---

## 1) Build, test, and lint commands

- Build and run locally (Maven):
  - mvn -q -DskipTests package
  - mvn spring-boot:run
- Run full test suite:
  - mvn test
  - Run with coverage (quality gate): mvn verify  (activates JaCoCo profile `cobertura`)
- Run a single test class or method (surefire pattern):
  - mvn -Dtest=ClassName test
  - mvn -Dtest=ClassName#methodName test
- Testcontainers / Postgres integration tests (requires Docker up):
  - mvn test -Ddocker.tests=true
- Docker (recommended for quick runtime):
  - docker compose up --build
  - docker compose down

Note: no dedicated Java linter/formatter (Checkstyle/SpotBugs) is included by default in the repo.

---

## 2) High-level architecture

- Spring Boot (Java 21) REST API. Main layers:
  - controller: HTTP entry points (DTOs in/out)
  - service: orchestration, transactional logic
  - repository: Spring Data JPA derived queries
  - model: JPA entities (domain behaviour lives here)
  - dto + mapper: separation between entities and API surface
  - config/security: SecurityFilterChain, Jwt filter, CORS, PasswordEncoder
  - advice/exception: centralized exception handling (maps domain exceptions -> HTTP codes)

- Persistence profiles:
  - default: H2 (file or test-memory depending on profile)
  - docker: PostgreSQL (configured in application-docker.yml and docker-compose.yml)

- Build & distribution:
  - Multi-stage Dockerfile (compile with Maven JDK stage, runtime JRE stage)
  - docker-compose brings up `api` + `db` with sensible defaults and healthcheck

- Tests:
  - Unit, slices (@WebMvcTest/@DataJpaTest) and integration (@SpringBootTest)
  - JaCoCo coverage gate (70%) in profile `cobertura`
  - Testcontainers used for an optional Postgres-backed repository test when `-Ddocker.tests=true`

---

## 3) Key conventions and repo-specific patterns

- Domain rules are colocated with the entity (not in service):
  - e.g. Task.crear(...) validates dueDate on creation; Task.setStatus(...) enforces "no DONE without assignee"; Task.estaVencida() computes overdue state.
  - Distinguish creation vs rehydration: constructor allows rehydrating an already-expired task while the factory `crear` forbids creating one with past due date.

- Controllers always use DTOs; entities are not serialized directly. Use mappers in `mapper` package.

- Authorization pattern: URL-based coarse rules in SecurityConfig + data-driven checks with @PreAuthorize and beans (e.g. ProjectSecurity) for owner checks.

- Data seeding: DataSeeder seeds demo users/projects/tasks only if DB is empty. Tests should not depend on seed data (integration tests create their own data).

- Ordering & UI expectations: GET /tasks returns tasks ordered by TaskOrders.POR_URGENCIA (uses Task.estaVencida()). There is no dedicated "overdue" endpoint.

- Profiles & environment variables:
  - Default config: application.yml (H2 and JWT defaults for dev)
  - Docker runtime: application-docker.yml (Postgres); docker-compose injects DB credentials and JWT_SECRET via env or .env

- Testing notes for maintainers:
  - JaCoCo profile activation uses `!m2e.version` to avoid interfering with Eclipse runs.
  - Testcontainers requires api.version=1.41 (configured via the docker-it profile/system property when -Ddocker.tests=true).

---

## 4) Files to check first when debugging or extending features

- README.md (project overview & run instructions)
- pom.xml (build, profiles, JaCoCo, testcontainers config)
- Dockerfile & docker-compose.yml
- src/main/java/com/taskflow/config/SecurityConfig.java
- src/main/java/com/taskflow/model/Task.java (domain rules: crear, setStatus, estaVencida)
- src/main/java/com/taskflow/service/TaskService.java and TaskOrders.java
- src/main/java/com/taskflow/controller (TaskController, ProjectController, AuthController)
- src/main/java/com/taskflow/config/DataSeeder.java

---

## 5) Integration with other assistant configs

- No existing assistant config files were found (CLAUDE.md, AGENTS.md, .cursorrules, .windsurfrules, CONVENTIONS.md, etc.).
- Add any project-specific AI assistant docs here if introduced later; Copilot will prefer this file for repo hints.

---

If you want to tune Copilot sessions around testing or add an automated MCP server (e.g., Playwright for a real frontend), say so and the repo can be extended with recommended MCP server config.

---

Created by Copilot CLI helper to speed future automation and session setup. Ask to adjust wording, add missing commands, or include repository-specific snippets (example mvn -Dtest=...).
