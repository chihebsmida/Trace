package org.epac.trace.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.epac.trace.dto.WorkSummary;
import org.epac.trace.entity.Trace;
import org.epac.trace.exception.InvalidTraceOperationException;
import org.epac.trace.services.TraceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@AllArgsConstructor
public class TraceController {
    private final TraceService traceService;


    @PostMapping("/add")
    @Operation(summary = "Add a new trace")
    public ResponseEntity<?> addTrace(
            @Parameter(description = "Trace object to be stored in database", required = true) @RequestBody Trace trace) {
        try {
            Map<String, WorkSummary> workSummaryMap= traceService.addTrace(trace);
            return ResponseEntity.ok(workSummaryMap);
        } catch (InvalidTraceOperationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/work-summary/{employerName}")
    @Operation(summary = "Obtenir le résumé de travail pour un employeur et une date donnés")
    public WorkSummary getWorkSummary(
            @Parameter(description = "Nom de l'employeur", required = true) @PathVariable String employerName,
            @Parameter(description = "Date pour laquelle le résumé est requis format date yyyy-MM-DD", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return traceService.calculateDailyWorkSummary(employerName, date);
    }

    @GetMapping("/work-summary-by-employe")
    @Operation(summary = "Obtenir le résumé de travail par employer et machine pour une date donnés",
            description = "Retourne une map des résumés de travail de chaque employé, groupés par machine pour une journée donnée.")
    @ApiResponse(responseCode = "200", description = "Résumé du travail récupéré avec succès")
    public Map<String, Map<String, WorkSummary>> getWorkSummaryByEmployeeAndMachine(
            @Parameter(description = "Date pour laquelle le résumé est requis format date yyyy-MM-DD ", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return traceService.getWorkSummaryByEmployeeAndMachine( date);
    }
    @GetMapping("/work-summaryByMachine/{employerName}")
    @Operation(summary = "Obtenir le résumé de travail par machine pour un employeur et une date donnés")
    public Map<String, WorkSummary> getWorkSummaryByMachine(
            @Parameter(description = "Nom de l'employeur", required = true) @PathVariable String employerName,
            @Parameter(description = "Date pour laquelle le résumé est requis format date yyyy-MM-DD ", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return traceService.calculateDailyWorkSummaryByMachine(employerName, date);
    }
    @GetMapping("/work-summary-by-machine")
    @Operation(summary = "Obtenir le résumé du travail par machine et employé pour une date donnée",
            description = "Retourne une map des résumés de travail de chaque machine, groupés par employé pour une journée donnée.")
    @ApiResponse(responseCode = "200", description = "Résumé du travail récupéré avec succès")
    public Map<String, Map<String,WorkSummary>> getWorkSummaryByMachineAndEmployee(
            @Parameter(description = "Date pour laquelle le résumé est requis format date yyyy-MM-DD ", required = true)
            @RequestParam LocalDate date) {

        return traceService.getWorkSummaryByMachineAndEmployee(date);
    }

}
