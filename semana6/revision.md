# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `316ec792-d3a2-4778-a08e-721f258a1aec`  
> - **Started:** 17/10/2026, 2:37:25 p.m.  
> - **Duration:** 44s  
> - **Exported:** 17/10/2026, 2:38:09 p.m.  

---

<sub>3s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/progress.md.

---

<sub>8s</sub>

### Copilot

Leeré el diff y la especificación para identificar archivos cambiados y los criterios a revisar. Abriendo semana6\proyecto-final.diff y specs\progress.md para analizar.

---

<sub>8s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-Alejandro761\semana6\proyecto-final.diff.**

C:\Users\Lenovo\taskflow-copilot-Alejandro761\semana6\proyecto-final.diff

<details>
<summary>259 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/ReportController.java b/src/main/java/com/taskflow/controller/ReportController.java
new file mode 100644
index 0000000..9b22f1c
--- /dev/null
+++ b/src/main/java/com/taskflow/controller/ReportController.java
@@ -0,0 +1,31 @@
+package com.taskflow.controller;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.service.ProjectService;
+import io.swagger.v3.oas.annotations.Operation;
+import io.swagger.v3.oas.annotations.tags.Tag;
+import org.springframework.web.bind.annotation.GetMapping;
+import org.springframework.web.bind.annotation.RestController;
+
+import java.util.List;
+
+/**
+ * ReportController — endpoints de reportes agregados a la API.
+ */
+@RestController
+@Tag(name = "Reports", description = "Informes agregados: progreso por proyecto.")
+public class ReportController {
+
+    private final ProjectService projectService;
+
+    public ReportController(ProjectService projectService) {
+        this.projectService = projectService;
+    }
+
+    @Operation(summary = "Progreso por proyecto",
+            description = "Devuelve para cada proyecto el total de tareas, cuántas están DONE y el porcentaje completado (un decimal).")
+    @GetMapping("/reports/progress")
+    public List<ProjectProgressResponse> getProgress() {
+        return projectService.progresoPorProyecto();
+    }
+}
diff --git a/src/main/java/com/taskflow/dto/ProjectProgressResponse.java b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
new file mode 100644
index 0000000..0ed0b28
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/ProjectProgressResponse.java
@@ -0,0 +1,14 @@
+package com.taskflow.dto;
+
+/**
+ * ProjectProgressResponse — DTO de salida para el reporte de progreso por proyecto.
+ *
+ * Campos:
+ *  - projectId: id del proyecto
+ *  - projectName: nombre del proyecto
+ *  - totalTasks: cuántas tareas tiene el proyecto
+ *  - doneTasks: cuántas están en DONE
+ *  - percentDone: porcentaje completado, redondeado a un decimal
+ */
+public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) {
+}
diff --git a/src/main/java/com/taskflow/mapper/ProjectMapper.java b/src/main/java/com/taskflow/mapper/ProjectMapper.java
index 6bbf540..f3178a3 100644
--- a/src/main/java/com/taskflow/mapper/ProjectMapper.java
+++ b/src/main/java/com/taskflow/mapper/ProjectMapper.java
@@ -28,5 +28,10 @@ public final class ProjectMapper {
     public static ProjectSummaryResponse aSummary(Long projectId, String projectName, long totalTasks, Map<String, Long> byStatus, long overdue) {
         return new ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
     }
+
+    /** Entidad -> DTO de salida del progreso por proyecto. */
+    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone) {
+        return new com.taskflow.dto.ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks, percentDone);
+    }
 }
 
diff --git a/src/main/java/com/taskflow/service/ProjectService.java b/src/main/java/com/taskflow/service/ProjectService.java
index a9e3f79..38a04d1 100644
--- a/src/main/java/com/taskflow/service/ProjectService.java
+++ b/src/main/java/com/taskflow/service/ProjectService.java
@@ -77,6 +77,30 @@ public class ProjectService {
         return com.taskflow.mapper.ProjectMapper.aSummary(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
     }
 
+    /**
+     * Progreso por proyecto: lista con total, done y porcentaje redondeado a 1 decimal.
+     * Respeta la regla: aparecen todos los proyectos y orden por projectId asc.
+     */
+    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
+        java.util.List<Project> proyectos = projectRepository.findAll();
+        return proyectos.stream()
+                .sorted(java.util.Comparator.comparing(Project::getId))
+                .map(p -> {
+                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
+                    long total = tareas.size();
+                    long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
+                    double percent;
+                    if (total == 0) {
+                        percent = 0.0;
+                    } else {
+                        double raw = done * 100.0 / total;
+                        percent = Math.round(raw * 10.0) / 10.0;
+                    }
+                    return com.taskflow.mapper.ProjectMapper.aProgreso(p, total, done, percent);
+                })
+                .toList();
+    }
+
 
     /**
      * Crea un proyecto (POST): el request trae name y description; el ownerId NO es una constante
diff --git a/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
new file mode 100644
index 0000000..41a9b33
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java
@@ -0,0 +1,48 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.ReportController;
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.http.MediaType;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(ReportController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ProgresoProyectosControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void getProgress_retorna200YJSONConCamposEsperados() throws Exception {
+        when(projectService.progresoPorProyecto()).thenReturn(List.of(
+                new ProjectProgressResponse(1L, "Plataforma TaskFlow", 5L, 1L, 20.0),
+                new ProjectProgressResponse(2L, "App Móvil", 4L, 1L, 25.0)
+        ));
+
+        mockMvc.perform(get("/reports/progress").contentType(MediaType.APPLICATION_JSON))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].projectId").value(1))
+                .andExpect(jsonPath("$[0].percentDone").value(20.0))
+                .andExpect(jsonPath("$[1].totalTasks").value(4));
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
new file mode 100644
index 0000000..81a3d0c
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java
@@ -0,0 +1,95 @@
+package com.taskflow.unit;
+
+import com.taskflow.dto.ProjectProgressResponse;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Project;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.ProjectRepository;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.repository.UserRepository;
+import com.taskflow.service.ProjectService;
+import com.taskflow.exception.TaskValidationException;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.time.LocalDate;
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ProgresoProyectosServiceTest {
+
+    @Mock
+    private ProjectRepository projectRepository;
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @Mock
+    private UserRepository userRepository;
+
+    @InjectMocks
+    private ProjectService projectService;
+
+    private Task tarea(long id, String title, TaskStatus status, long projectId) {
+        try {
+            return new Task(id, title, "desc", status, Priority.MED, 1L, projectId, LocalDate.now());
+        } catch (TaskValidationException e) {
+            throw new IllegalStateException(e);
+        }
+    }
+
+    @Test
+    void progresoPorProyecto_calculaCorrectamenteYOrdenaPorId() {
+        // proyectos devueltos en orden 3,1,2 (entrelazado con la espec)
+        when(projectRepository.findAll()).thenReturn(List.of(
+                new Project(3L, "P3", "d", 1L, null),
+                new Project(1L, "P1", "d", 1L, null),
+                new Project(2L, "P2", "d", 1L, null)
+        ));
+
+        // proyecto 1: 5 tareas, 1 DONE -> 20.0
+        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
+                tarea(1L, "T-1", TaskStatus.DONE, 1L),
+                tarea(2L, "T-2", TaskStatus.TODO, 1L),
+                tarea(3L, "T-3", TaskStatus.TODO, 1L),
+                tarea(4L, "T-4", TaskStatus.TODO, 1L),
+                tarea(5L, "T-5", TaskStatus.TODO, 1L)
+        ));
+
+        // proyecto 2: 3 tareas, 1 DONE -> 33.3
+        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
+                tarea(6L, "T-6", TaskStatus.DONE, 2L),
+                tarea(7L, "T-7", TaskStatus.TODO, 2L),
+                tarea(8L, "T-8", TaskStatus.TODO, 2L)
+        ));
+
+        // proyecto 3: sin tareas
+        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
+
+        List<ProjectProgressResponse> resultado = projectService.progresoPorProyecto();
+
+        assertEquals(3, resultado.size());
+        // orden por projectId asc: 1,2,3
+        assertEquals(1L, resultado.get(0).projectId());
+        assertEquals(5L, resultado.get(0).totalTasks());
+        assertEquals(1L, resultado.get(0).doneTasks());
+        assertEquals(20.0, resultado.get(0).percentDone());
+
+        assertEquals(2L, resultado.get(1).projectId());
+        assertEquals(3L, resultado.get(1).totalTasks());
+        assertEquals(1L, resultado.get(1).doneTasks());
+        assertEquals(33.3, resultado.get(1).percentDone());
+
+        assertEquals(3L, resultado.get(2).projectId());
+        assertEquals(0L, resultado.get(2).totalTasks());
+        assertEquals(0L, resultado.get(2).doneTasks());
+        assertEquals(0.0, resultado.get(2).percentDone());
+    }
+}
```

</details>


---

<sub>12s</sub>

### Copilot

Leyendo specs/progress.md para comparar la especificación con los cambios del diff y aplicar la checklist de revisión. Ejecutando apertura del archivo.

---

<sub>12s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-Alejandro761\specs\progress.md.**

C:\Users\Lenovo\taskflow-copilot-Alejandro761\specs\progress.md

<details>
<summary>77 lines</summary>

````
# Especificación — `GET /reports/progress`

## Qué

Un reporte con el avance de **cada** proyecto: cuántas tareas tiene, cuántas están terminadas y qué
porcentaje representan. Sirve para una pantalla de «cómo vamos» sin pedir las tareas proyecto por
proyecto.

```
GET /reports/progress
Authorization: ******
```

## Respuesta 200

```json
[
  { "projectId": 1, "projectName": "Plataforma TaskFlow", "totalTasks": 5, "doneTasks": 1, "percentDone": 20.0 },
  { "projectId": 2, "projectName": "App Móvil",           "totalTasks": 4, "doneTasks": 1, "percentDone": 25.0 },
  { "projectId": 3, "projectName": "Migración Legacy",    "totalTasks": 0, "doneTasks": 0, "percentDone": 0.0 }
]
```

| Campo | Tipo | Qué es |
|---|---|---|
| `projectId` | número | el `id` del proyecto |
| `projectName` | texto | el `name` del proyecto |
| `totalTasks` | número | cuántas tareas tiene el proyecto |
| `doneTasks` | número | cuántas de esas están en `DONE` |
| `percentDone` | número decimal | `doneTasks` entre `totalTasks`, por 100, redondeado a **un decimal** |

## Reglas

1. Aparecen **todos** los proyectos, también los que no tienen tareas.
2. Proyecto sin tareas: `totalTasks` 0, `doneTasks` 0 y `percentDone` **`0.0`**. Nunca `NaN` ni un
   error por dividir entre cero.
3. El porcentaje se calcula en decimal **antes** de dividir (`doneTasks * 100.0 / totalTasks`) y se
   redondea a un decimal: 1 de 3 → `33.3`; 2 de 3 → `66.7`; 1 de 8 → `12.5`. Es la trampa `long/long`
   que ya documenta `ReportService.porcentajeCompletadas()`: con división entera, 1 de 4 da `0`.
4. Orden: por `projectId` ascendente, aunque el repositorio los devuelva en otro orden.
5. Seguridad: cualquier usuario autenticado puede pedirlo (sin token → `401`). No cambies
   `SecurityConfig`.

## Dónde

- DTO de salida nuevo en `com.taskflow.dto`:
  `public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone)`,
  con su Javadoc.
- Mapper: método nuevo `public static ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone)`
  en `ProjectMapper`.
- Método nuevo en `ProjectService`: `public List<ProjectProgressResponse> progresoPorProyecto()`.
  `ProjectService` ya tiene los dos repositorios que hacen falta (`projectRepository` y
  `taskRepository`): usa `projectRepository.findAll()` y `taskRepository.findByProjectId(Long)`.
  **No agregues métodos a los repositorios.** La cuenta y el redondeo van aquí, no en el controller.
- Controller **nuevo** `ReportController` en `com.taskflow.controller`, con
  `@Tag(name = "Reports", description = …)`, `@GetMapping("/reports/progress")` y `@Operation` en
  español. Recibe `ProjectService` por constructor. Es un controller nuevo, así que ningún slice test
  existente lo conoce y no rompe nada.

## Tests que deben existir al terminar

Los dos en **clases nuevas**:

- **Unit** `src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java`, con `@Mock` de
  `ProjectRepository`, `TaskRepository` y `UserRepository` (los tres del constructor de
  `ProjectService`) e `@InjectMocks ProjectService`. Un caso con tres proyectos que el mock devuelve
  en orden `3, 1, 2`:
  - el 1 con 5 tareas y 1 `DONE` → `20.0`;
  - el 2 con 3 tareas y 1 `DONE` → `33.3` (prueba el redondeo);
  - el 3 sin tareas → `0.0`;
  - y la lista sale en orden `1, 2, 3`.
- **Slice** `src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java`, con
  `@WebMvcTest(ReportController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `ProjectService` y `JwtAuthenticationFilter`. Caso: `200` y cada campo del JSON de los dos
  primeros elementos que devuelve el mock (`$[0].projectId`, `$[0].percentDone`, `$[1].totalTasks`…).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /reports/progress` | `200` con los tres objetos del ejemplo de arriba, en ese orden |
| `PATCH /tasks/1/status` con `{"status": "DONE"}` y luego `GET /reports/progress` | el proyecto 1 pasa a `doneTasks` 2 y `percentDone` `40.0` (la tarea 1 tiene responsable, así que el PATCH responde 200) |
| `GET /reports/progress` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `ProjectProgressResponse.java` (nuevo), `ProjectMapper.java`,
  `ProjectService.java`, `ReportController.java` (nuevo) y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-progress.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>22s</sub>

### Copilot

Reading the ProjectMapper.java current file to verify imports and consistency. Then will open ProjectService.java around changes to ensure imports and repository fields exist. I'll fetch both files in parallel.

---

<sub>22s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-Alejandro761\src\main\java\com\taskflow\mapper\ProjectMapper.java.**

C:\Users\Lenovo\taskflow-copilot-Alejandro761\src\main\java\com\taskflow\mapper\ProjectMapper.java

<details>
<summary>30 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.ProjectResponse;
import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Project;
import java.util.Map;

/**
 * ProjectMapper — puente DTO &lt;-&gt; dominio del lado Project. Estático, a mano, sin MapStruct.
 *
 * Mapeo trivial: la entidad guarda 'ownerId' directo (no un objeto User), así que aResponse no
 * deriva el id desde un objeto — lee p.getOwnerId() tal cual. Cuando el
 * dominio coincide con el contrato de salida (ProjectResponse con ownerId Long), el mapeo es una línea.
 */
public final class ProjectMapper {

    private ProjectMapper() {
        // no instanciable
    }

    /** Entidad -> DTO de salida. ownerId sale directo del campo (sin puente por objeto). */
    public static ProjectResponse aResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(),
                p.getOwnerId(), p.getCreatedAt());
    }

    /** Entidad -> DTO de salida del resumen de proyecto. */
    public static ProjectSummaryResponse aSummary(Long projectId, String projectName, long totalTasks, Map<String, Long> byStatus, long overdue) {
        return new ProjectSummaryResponse(projectId, projectName, totalTasks, byStatus, overdue);
    }

    /** Entidad -> DTO de salida del progreso por proyecto. */
    public static com.taskflow.dto.ProjectProgressResponse aProgreso(Project proyecto, long totalTasks, long doneTasks, double percentDone) {
        return new com.taskflow.dto.ProjectProgressResponse(proyecto.getId(), proyecto.getName(), totalTasks, doneTasks, percentDone);
    }
}
```

</details>


---

<sub>22s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-Alejandro761\src\main\java\com\taskflow\service\ProjectService.java.**

C:\Users\Lenovo\taskflow-copilot-Alejandro761\src\main\java\com\taskflow\service\ProjectService.java

<details>
<summary>130 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.ProjectRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProjectService — la capa de negocio del lado Project. Cero HTTP aquí (el "no existe" se traduce con
 * orElseThrow -> ProjectNotFoundException; el advice le pone el 404).
 *
 * Dos decisiones que se ven aquí:
 *   - Project guarda un ownerId (Long), no un objeto User, así que 'crear' y 'reemplazar' manejan
 *     ese Long. 'crear' construye con id null y la BD asigna el id (IDENTITY).
 *   - 'tareasDe' usa la derived query taskRepository.findByProjectId(id) en vez de filtrar findAll()
 *     con un stream: el filtro lo hace la BD.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;   // para resolver el owner desde el username del JWT

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository,
                          UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** Todos los proyectos. */
    public List<Project> listar() {
        return projectRepository.findAll();
    }

    /** Un proyecto por id (el Optional sube tal cual: el controller decide 404 vs 200). */
    public Optional<Project> buscarPorId(Long id) {
        return projectRepository.findById(id);
    }

    /**
     * Las tareas de un proyecto, con la derived query taskRepository.findByProjectId(projectId):
     * el WHERE project_id = ? lo hace
     * la BD (viajan solo las filas del proyecto). "no existe el proyecto" (404) lo decide el
     * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
     * [] (-> 200 con []). No son lo mismo.
     */
    public List<Task> tareasDe(Long projectId) {
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Resumen del proyecto: cuántas tareas por estado y cuántas están vencidas.
     * Recibe el proyecto ya encontrado (el controller decide 404 con buscarPorId).
     */
    public com.taskflow.dto.ProjectSummaryResponse resumen(Project proyecto) {
        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
        long total = tareas.size();
        java.util.Map<com.taskflow.model.TaskStatus, Long> counts = tareas.stream()
                .collect(java.util.stream.Collectors.groupingBy(Task::getStatus, java.util.stream.Collectors.counting()));
        java.util.Map<String, Long> byStatus = new java.util.HashMap<>();
        for (com.taskflow.model.TaskStatus s : com.taskflow.model.TaskStatus.values()) {
            byStatus.put(s.name(), counts.getOrDefault(s, 0L));
        }
        long overdue = tareas.stream().filter(Task::estaVencida).count();
        return com.taskflow.mapper.ProjectMapper.aSummary(proyecto.getId(), proyecto.getName(), total, byStatus, overdue);
    }

    /**
     * Progreso por proyecto: lista con total, done y porcentaje redondeado a 1 decimal.
     * Respeta la regla: aparecen todos los proyectos y orden por projectId asc.
     */
    public java.util.List<com.taskflow.dto.ProjectProgressResponse> progresoPorProyecto() {
        java.util.List<Project> proyectos = projectRepository.findAll();
        return proyectos.stream()
                .sorted(java.util.Comparator.comparing(Project::getId))
                .map(p -> {
                    java.util.List<Task> tareas = taskRepository.findByProjectId(p.getId());
                    long total = tareas.size();
                    long done = tareas.stream().filter(t -> t.getStatus() == com.taskflow.model.TaskStatus.DONE).count();
                    double percent;
                    if (total == 0) {
                        percent = 0.0;
                    } else {
                        double raw = done * 100.0 / total;
                        percent = Math.round(raw * 10.0) / 10.0;
                    }
                    return com.taskflow.mapper.ProjectMapper.aProgreso(p, total, done, percent);
                })
                .toList();
    }


    /**
     * Crea un proyecto (POST): el request trae name y description; el ownerId NO es una constante
     * — se resuelve del USERNAME AUTENTICADO (el que puso el JWT en el
     * Authentication). El dueño no lo decide el cliente ni una semilla: es QUIEN llama. createdAt = hoy;
     * id == null -> la BD lo asigna (IDENTITY).
     */
    public Project crear(ProjectRequest request, String ownerUsername) {
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new UsernameNotFoundException(ownerUsername));
        Project nuevo = new Project(null, request.name(), request.description(),
                owner.getId(), LocalDate.now());
        return projectRepository.save(nuevo);
    }

    /**
     * Reemplazo COMPLETO (PUT): existe -> reconstruye con el MISMO id, conservando ownerId y createdAt
     * (el cliente no los toca); name y description vienen del request. save con id presente = update.
     * No existe -> 404.
     */
    public Project reemplazar(Long id, ProjectRequest request) {
        Project actual = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        Project reemplazo = new Project(actual.getId(), request.name(), request.description(),
                actual.getOwnerId(), actual.getCreatedAt());
        return projectRepository.save(reemplazo);
    }

    /**
     * Elimina un proyecto (DELETE) con CASCADA MANUAL: borra primero sus tareas (regla "no Task sin
     * Project" — y además la FK task.project_id lo EXIGE: borrar el proyecto con tareas colgando daría
     * una violación de integridad) y luego el proyecto. No existe -> 404. La regla owner/ADMIN no
     * vive aquí: la aplica el @PreAuthorize del controller antes de llegar a este método.
     */
    public void eliminar(Long id) {
        projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
        projectRepository.deleteById(id);
    }
}
```

</details>


---

<sub>44s</sub>

### Copilot

Resumen breve: el diff implementa exactamente lo pedido en specs/progress.md. No hay fallos que bloqueen el merge; hay un par de sugerencias menores.

| # | Gravedad | Archivo:línea | Punto de la checklist | Qué pasa | Qué harías |
|---|---|---:|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/mapper/ProjectMapper.java: + | Convenciones / estilo | El método aProgreso usa el tipo FQCN com.taskflow.dto.ProjectProgressResponse en lugar de import. | Importar el DTO para claridad (estético). |
| 2 | Sugerencia | src/main/java/com/taskflow/service/ProjectService.java: + | Regla cálculo y precisión | Redondeo con Math.round está bien; para casos de precisión extremo BigDecimal sería más explícito. | Opcional: documentar el método de redondeo o usar BigDecimal si se desea máxima exactitud. |
| 3 | Sugerencia | src/test/java/com/taskflow/slice/ProgresoProyectosControllerTest.java: + | Tests que prueban algo | El slice test verifica campos clave; no cubre respuesta 401 (seguridad) que la spec menciona en "Resultado esperado". | (Opcional) Añadir un test que compruebe 401 al no enviar token (aunque la spec no exige el test). |

Casos sin test:
- Ninguno: los dos tests requeridos por la especificación (unit y slice) están presentes y cubren los casos indicados (orden, redondeo y proyecto sin tareas).

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
