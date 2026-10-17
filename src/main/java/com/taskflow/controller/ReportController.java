package com.taskflow.controller;

import com.taskflow.dto.ProjectProgressResponse;
import com.taskflow.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ReportController — endpoints de reportes agregados a la API.
 */
@RestController
@Tag(name = "Reports", description = "Informes agregados: progreso por proyecto.")
public class ReportController {

    private final ProjectService projectService;

    public ReportController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Progreso por proyecto",
            description = "Devuelve para cada proyecto el total de tareas, cuántas están DONE y el porcentaje completado (un decimal).")
    @GetMapping("/reports/progress")
    public List<ProjectProgressResponse> getProgress() {
        return projectService.progresoPorProyecto();
    }
}
