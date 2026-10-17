src/test/java/com/taskflow/unit/ProgresoProyectosServiceTest.java : línea 42

El constructor de Task recibe projectId antes de assigneeId (src/main/java/com/taskflow/model/Task.java:107-108). Aquí están intercambiados: las tareas preparadas para el proyecto 2 pertenecen realmente al 1, aunque el mock las devuelve para findByProjectId(2L). Pasa projectId como sexto argumento y 1L como responsable para que el escenario multiproyecto sea coherente con el repositorio real.

Suggested changeset:
return new Task(id, title, "desc", status, Priority.MED, projectId, 1L, LocalDate.now());