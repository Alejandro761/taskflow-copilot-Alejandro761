package com.taskflow.dto;

/**
 * ProjectProgressResponse — DTO de salida para el reporte de progreso por proyecto.
 *
 * Campos:
 *  - projectId: id del proyecto
 *  - projectName: nombre del proyecto
 *  - totalTasks: cuántas tareas tiene el proyecto
 *  - doneTasks: cuántas están en DONE
 *  - percentDone: porcentaje completado, redondeado a un decimal
 */
public record ProjectProgressResponse(Long projectId, String projectName, long totalTasks, long doneTasks, double percentDone) {
}
