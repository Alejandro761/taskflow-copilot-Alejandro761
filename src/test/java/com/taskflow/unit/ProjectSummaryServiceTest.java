package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSummaryServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private final Project proyecto = new Project(2L, "App Móvil", "d", 1L, null);

    @Test
    void resumen_proyectoConTareas_yUnaVencida() throws Exception {
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
                tarea(10L, TaskStatus.TODO, null, null),
                tarea(11L, TaskStatus.IN_PROGRESS, null, null),
                tarea(12L, TaskStatus.IN_PROGRESS, 1L, LocalDate.now().minusDays(1)), // vencida
                tarea(13L, TaskStatus.DONE, 2L, LocalDate.now().minusDays(2))
        ));

        ProjectSummaryResponse expected = new ProjectSummaryResponse(2L, "App Móvil", 4,
                Map.of("TODO", 1L, "IN_PROGRESS", 2L, "DONE", 1L), 1L);

        assertEquals(expected, service.resumen(proyecto));
    }

    @Test
    void resumen_proyectoSinTareas_todoEnCero() {
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of());

        ProjectSummaryResponse expected = new ProjectSummaryResponse(2L, "App Móvil", 0,
                Map.of("TODO", 0L, "IN_PROGRESS", 0L, "DONE", 0L), 0L);

        assertEquals(expected, service.resumen(proyecto));
    }

    private Task tarea(Long id, TaskStatus status, Long assigneeId, LocalDate dueDate) throws Exception {
        return new Task(id, "Tarea " + id, "d", status, Priority.MED, 2L, assigneeId, dueDate);
    }
}
