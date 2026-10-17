package com.taskflow.unit;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import com.taskflow.exception.TaskValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgresoProyectosServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private Task tarea(long id, String title, TaskStatus status, long projectId) {
        try {
            return new Task(id, title, "desc", status, Priority.MED, 1L, projectId, LocalDate.now());
        } catch (TaskValidationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void progresoPorProyecto_calculaCorrectamenteYOrdenaPorId() {
        // proyectos devueltos en orden 3,1,2 (entrelazado con la espec)
        when(projectRepository.findAll()).thenReturn(List.of(
                new Project(3L, "P3", "d", 1L, null),
                new Project(1L, "P1", "d", 1L, null),
                new Project(2L, "P2", "d", 1L, null)
        ));

        // proyecto 1: 5 tareas, 1 DONE -> 20.0
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                tarea(1L, "T-1", TaskStatus.DONE, 1L),
                tarea(2L, "T-2", TaskStatus.TODO, 1L),
                tarea(3L, "T-3", TaskStatus.TODO, 1L),
                tarea(4L, "T-4", TaskStatus.TODO, 1L),
                tarea(5L, "T-5", TaskStatus.TODO, 1L)
        ));

        // proyecto 2: 3 tareas, 1 DONE -> 33.3
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
                tarea(6L, "T-6", TaskStatus.DONE, 2L),
                tarea(7L, "T-7", TaskStatus.TODO, 2L),
                tarea(8L, "T-8", TaskStatus.TODO, 2L)
        ));

        // proyecto 3: sin tareas
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        List<ProjectProgressResponse> resultado = projectService.progresoPorProyecto();

        assertEquals(3, resultado.size());
        // orden por projectId asc: 1,2,3
        assertEquals(1L, resultado.get(0).projectId());
        assertEquals(5L, resultado.get(0).totalTasks());
        assertEquals(1L, resultado.get(0).doneTasks());
        assertEquals(20.0, resultado.get(0).percentDone());

        assertEquals(2L, resultado.get(1).projectId());
        assertEquals(3L, resultado.get(1).totalTasks());
        assertEquals(1L, resultado.get(1).doneTasks());
        assertEquals(33.3, resultado.get(1).percentDone());

        assertEquals(3L, resultado.get(2).projectId());
        assertEquals(0L, resultado.get(2).totalTasks());
        assertEquals(0L, resultado.get(2).doneTasks());
        assertEquals(0.0, resultado.get(2).percentDone());
    }
}
