package com.taskflow.unit;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskServiceTest — UNIT PURO con Mockito: aquí NO arranca Spring — la clase corre en MILISEGUNDOS.
 *
 * Por qué Mockito: TaskRepository es una interfaz cuya única implementación es un proxy de Spring Data
 * respaldado por una BD. Un *fake* en memoria (rápido y determinista) también sería legítimo, pero no
 * existe: para probar el service EN AISLAMIENTO no hay implementación de juguete que instanciar
 * -> por eso existe Mockito: el mock SUSTITUYE al fake cuando el colaborador real exige infraestructura.
 *
 * Reglas aplicadas:
 *   - @ExtendWith(MockitoExtension.class): sin ella, @Mock queda null.
 *   - Los DATOS (Task) se construyen DE VERDAD con el constructor de rehidratación; SOLO el COLABORADOR
 *     (el repository) se mockea.
 *   - MockitoExtension corre en strict-stubs: un when(...) que ningún camino usa FALLA la clase
 *     (UnnecessaryStubbingException) -> aquí cada stub tiene propósito.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private com.taskflow.service.TaskService service;

    private static final Long PROYECTO = 1L;

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        void crear_valida_delegaEnSaveYDevuelveTareaConId() throws TaskValidationException {
            // El request pasa por TaskMapper.aEntidadNueva -> Task.crear (status=TODO, id=null). El
            // repositorio, mockeado, "devuelve" una tarea YA con id (lo que la BD real haría en save).
            TaskRequest request = new TaskRequest("Redactar informe", "desc", Priority.HIGH, 5L, null);
            Task tareaConId = new Task(99L, "Redactar informe", "desc",
                    TaskStatus.TODO, Priority.HIGH, PROYECTO, 5L, null);
            when(repository.save(any(Task.class))).thenReturn(tareaConId);

            Task creada = service.crear(request, PROYECTO);

            assertNotNull(creada.getId());
            assertEquals(99L, creada.getId());
        }

        @Test
        @DisplayName("thenThrow: el mock simula una BD caída — lo que el colaborador real no te da nunca")
        void crear_conBdCaida_propagaLaExcepcion() {
            // thenThrow: con la BD real NO puedes provocar una caída a demanda; el mock sí.
            TaskRequest request = new TaskRequest("Tarea válida", "desc", Priority.MED, 5L, null);
            when(repository.save(any(Task.class)))
                    .thenThrow(new RuntimeException("Conexión a la BD perdida"));

            assertThrows(RuntimeException.class, () -> service.crear(request, PROYECTO));
        }
    }

    @Nested
    @DisplayName("cambiarStatus")
    class CambiarStatus {

        @Test
        void cambiarStatus_conAssignee_guardaDone() {
            // El dato se construye REAL (constructor de rehidratación), con assignee -> SÍ puede pasar a
            // DONE. Solo el repository (colaborador) se mockea.
            Task tareaConAssignee = tarea(1L, "Con responsable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(tareaConAssignee));
            when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            service.cambiarStatus(1L, TaskStatus.DONE);

            // ArgumentCaptor: capturar el Task que VIAJÓ a save. El retorno no basta cuando lo que
            // importa es el OBJETO MUTADO que se persistió.
            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(repository).save(captor.capture());
            assertEquals(TaskStatus.DONE, captor.getValue().getStatus());
        }

        @Test
        void cambiarStatus_idInexistente_lanzaTaskNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> service.cambiarStatus(999L, TaskStatus.DONE));
            // No se llegó a guardar nada.
            verify(repository, never()).save(any(Task.class));
        }

        @Test
        void cambiarStatus_sinAssignee_lanzaTaskStateExceptionYNoGuarda() {
            // Sin assignee, setStatus(DONE) lanza la checked; cambiarStatus la TRADUCE a TaskStateException.
            // La traducción, probada EN AISLAMIENTO (sin HTTP, sin BD).
            Task sinAssignee = tarea(2L, "Sin responsable", null);
            when(repository.findById(2L)).thenReturn(Optional.of(sinAssignee));

            assertThrows(TaskStateException.class,
                    () -> service.cambiarStatus(2L, TaskStatus.DONE));
            // El never() es el assert más valioso del bloque: la regla PROHIBIÓ persistir.
            verify(repository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        void eliminar_existente_llamaDeleteById() {
            Task existente = tarea(1L, "Borrable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            // verify sobre una operación MUDA (void): el estado no lo delata, la interacción sí.
            verify(repository).deleteById(1L);
        }

        @Test
        void eliminar_inexistente_lanzaTaskNotFoundExceptionYNoBorra() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> service.eliminar(999L));
            // never() + anyLong(): NO se borró nada. (Regla "todos matchers o ninguno": aquí anyLong()).
            verify(repository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("vencidas")
    class Vencidas {

        @Test
        void vencidas_filtraSoloVencidasYOrdenaPorFecha() {
            try {
                Task tAntigua = new Task(10L, "Antigua", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, java.time.LocalDate.now().minusDays(5));
                Task tReciente = new Task(11L, "Reciente", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, java.time.LocalDate.now().minusDays(1));
                Task sinFecha = new Task(12L, "Sin fecha", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, null);
                Task donePasada = new Task(13L, "Hecha", "d", TaskStatus.DONE, Priority.MED, PROYECTO, 1L, java.time.LocalDate.now().minusDays(3));

                when(repository.findAll()).thenReturn(java.util.List.of(sinFecha, donePasada, tReciente, tAntigua));

                java.util.List<Task> resultado = service.vencidas();

                // Debe incluir solo tAntigua y tReciente, en orden por dueDate asc (más antigua primero)
                org.junit.jupiter.api.Assertions.assertEquals(2, resultado.size());
                org.junit.jupiter.api.Assertions.assertEquals(10L, resultado.get(0).getId());
                org.junit.jupiter.api.Assertions.assertEquals(11L, resultado.get(1).getId());
            } catch (TaskValidationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Test
        void vencidas_noIncluyeTareasSinDueDateONoVencidasONoIncluyeDONE() {
            try {
                Task pendienteFutura = new Task(20L, "Futura", "d", TaskStatus.TODO, Priority.MED, PROYECTO, 1L, java.time.LocalDate.now().plusDays(1));
                Task sinFecha = new Task(21L, "Sin fecha", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, null);
                Task donePasada = new Task(22L, "Hecha pasada", "d", TaskStatus.DONE, Priority.MED, PROYECTO, 1L, java.time.LocalDate.now().minusDays(2));

                when(repository.findAll()).thenReturn(java.util.List.of(pendienteFutura, sinFecha, donePasada));

                java.util.List<Task> resultado = service.vencidas();

                org.junit.jupiter.api.Assertions.assertTrue(resultado.isEmpty());
            } catch (TaskValidationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Nested
    @DisplayName("SinResponsable")
    class SinResponsable {

        @Test
        void sinResponsable_filtraYOrdenaPorFecha() {
            try {
                Task conDue10 = new Task(201L, "Sin10", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, null, java.time.LocalDate.now().plusDays(10));
                Task conResp = new Task(202L, "ConResp", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 5L, java.time.LocalDate.now().plusDays(5));
                Task sinFecha = new Task(203L, "SinFecha", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, null, null);
                Task conDue2 = new Task(204L, "Sin2", "d", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, null, java.time.LocalDate.now().plusDays(2));

                // El repositorio devuelve, EN ESTE ORDEN: 10d, conResp, sinFecha, 2d
                when(repository.findAll()).thenReturn(java.util.List.of(conDue10, conResp, sinFecha, conDue2));

                java.util.List<Task> resultado = service.sinResponsable();

                // Debe devolver solo las 3 sin responsable, ordenadas por fecha asc (2d,10d,null)
                org.junit.jupiter.api.Assertions.assertEquals(3, resultado.size());
                java.util.List<Long> ids = resultado.stream().map(Task::getId).toList();
                org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(204L, 201L, 203L), ids);
            } catch (TaskValidationException e) {
                throw new IllegalStateException(e);
            }
        }

        @Test
        void sinResponsable_siTodasConAssignee_devuelveVacio() {
            Task con1 = tarea(301L, "Con1", 5L);
            when(repository.findAll()).thenReturn(java.util.List.of(con1));

            java.util.List<Task> resultado = service.sinResponsable();
            org.junit.jupiter.api.Assertions.assertEquals(0, resultado.size());
        }
    }

    /** Fabrica una Task de rehidratación REAL (dato, no mock). assigneeId null = sin responsable. */
    private Task tarea(Long id, String title, Long assigneeId) {
        try {
            return new Task(id, title, "desc", TaskStatus.TODO, Priority.MED, PROYECTO, assigneeId, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }
}
