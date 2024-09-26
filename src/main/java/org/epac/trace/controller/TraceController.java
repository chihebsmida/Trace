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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TraceController {
    private final TraceService traceService;


    @PostMapping("/add")
    @Operation(summary = "Add a new trace", description = "Add a new trace to the database no need to provide timestamp it will be generated automatically")
    @ApiResponse(responseCode = "200", description = "Trace added successfully")
    @PreAuthorize("hasRole('admin_role')")
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
    @Operation(summary = "Obtain the work summary for an employer for a given date.")
    @PreAuthorize("hasRole('admin_role')")
    public WorkSummary getWorkSummary(
            @Parameter(description = "Employee name", required = true) @PathVariable String employerName,
            @Parameter(description = "Date for which the summary is required in the format yyyy-MM-DD.", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return traceService.calculateDailyWorkSummary(employerName, date);
    }

    @GetMapping("/work-summary-by-employe")
    @Operation(summary = "Obtain the work summary by employer and machine for a given date.",
            description = "Return a map of work summaries for each employee, grouped by machine for a given day.")
    @ApiResponse(responseCode = "200", description = "Résumé du travail récupéré avec succès")
    @PreAuthorize("hasRole('admin_role')")
    public Map<String, Map<String, WorkSummary>> getWorkSummaryByEmployeeAndMachine(
            @Parameter(description = "Date for which the summary is required in the format yyyy-MM-DD", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return traceService.getWorkSummaryByEmployeeAndMachine( date);
    }
    @GetMapping("/work-summaryByMachine/{employerName}")
    @Operation(summary = "Get the work summary by machine for a given employer and date.")
    @PreAuthorize("hasRole('admin_role')")
    public Map<String, WorkSummary> getWorkSummaryByMachine(
            @Parameter(description = "Employee name", required = true) @PathVariable String employerName,
            @Parameter(description = "Date for which the summary is required in the format yyyy-MM-DD.", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return traceService.calculateDailyWorkSummaryByMachine(employerName, date);
    }
    @GetMapping("/work-summary-by-machine")
    @Operation(summary = "Obtain the work summary by machine and employer for a given date.",
            description = "Return a map of work summaries for each machine, grouped by employee for a given day.")
    @ApiResponse(responseCode = "200", description = "Work summary retrieved successfully.")
    @PreAuthorize("hasRole('admin_role')")
    public Map<String, Map<String,WorkSummary>> getWorkSummaryByMachineAndEmployee(
            @Parameter(description = "Date for which the summary is required in the format yyyy-MM-DD. ", required = true)
            @RequestParam LocalDate date) {

        return traceService.getWorkSummaryByMachineAndEmployee(date);
    }
    @GetMapping("/weekly-work-summary")
    @Operation(summary = "Get the weekly work summary for all employees by week.",
            description = "Return a map of work summaries for each employee, grouped by week.")
    @PreAuthorize("hasRole('admin_role')")
    Map<String, Map<LocalDate, WorkSummary>> calculateWeeklyWorkSummaryForAllEmployees() {
        return traceService.calculateWeeklyWorkSummaryForAllEmployees();
    }
    @GetMapping("/weekly-work-summary-by-employer")
    @Operation(summary = "Get the weekly work summary by week for an employee.",
            description = "Return a map of work summaries for each employee, grouped by week.")
    @PreAuthorize("hasRole('admin_role')")
    Map<LocalDate, WorkSummary> calculateWeeklyWorkSummaryForAllEmployees(
            @Parameter(description = "Employer name", required = true)
            @RequestParam String employerName) {
        return traceService.calculateWeeklyWorkSummaryByEmployee(employerName);
    }
    @GetMapping("/daily-work-summary")
    @Operation(summary = "Get the daily work summary for all employees.",
            description = "Return a map of work summaries for each employee, grouped by day.")
    @PreAuthorize("hasRole('admin_role')")
    Map<String, Map<LocalDate, WorkSummary>> calculateDailyWorkSummaryForAllEmployees() {
        return traceService.calculateDailyWorkSummaryForAllEmployees();
    }
    @GetMapping("/monthly-work-summary")
    @Operation(summary = "Get the monthly work summary for all employees.",
            description = "Return a map of work summaries for each employee, grouped by month.")
    Map<String, Map<LocalDate, WorkSummary>> calculateMonthlyWorkSummaryForAllEmployees() {
        return traceService.calculateMonthlyWorkSummaryForAllEmployees();
    }

    @GetMapping("/monthly-work-summary-by-employer")
    @Operation(summary = "Get the monthly work summary for an employee.",
            description = "Return a map of work summaries for each employee, grouped by month.")
    @PreAuthorize("hasRole('admin_role')")
    Map<LocalDate, WorkSummary> calculateMonthlyWorkSummaryByEmployee(
            @Parameter(description = "Employee", required = true)
            @RequestParam String employerName) {
        return traceService.calculateMonthlyWorkSummaryByEmployee(employerName);
    }

    @GetMapping("/daily-work-summary-by-employee-and-machine")
    @Operation(summary = "Get the daily work summary of an employee by machine.",
            description = "Return a map of work summaries by machine for a given employee, grouped by day.")
    @PreAuthorize("hasRole('user-role')")
    public Map<LocalDate, WorkSummary> calculateDailyWorkSummaryByEmployeeAndMachine(
            @RequestParam @Parameter(description = "Employee name", example = "oumaima") String employerName,
            @RequestParam @Parameter(description = "Machine name", example = "press1") String machineName) {
        return traceService.calculateDailyWorkSummaryByEmployeeAndMachine(employerName,machineName);
    }
    @GetMapping("/weekly-work-summary-by-employee-and-machine")
    @Operation(summary = "Get the weekly work summary of an employee by machine.",
            description = "Return a map of work summaries by machine for a given employee, grouped by week.")
    @PreAuthorize("hasRole('user-role')")
    public Map<LocalDate, WorkSummary> calculateWeeklyWorkSummaryByEmployeeAndMachine(
            @RequestParam @Parameter(description = "Employee name", example = "oumaima") String employerName,
            @RequestParam @Parameter(description = "Machine name", example = "press1") String machineName) {
        return traceService.calculateWeeklyWorkSummaryByEmployeeAndMachine(employerName,machineName);
    }
    @GetMapping("/monthly-work-summary-by-employee-and-machine")
    @Operation(summary = "Get the monthly work summary of an employee by machine.",
            description = "Return a map of work summaries by machine for a given employee, grouped by month.")
    @PreAuthorize("hasRole('user-role')")
    public Map<LocalDate, WorkSummary> calculateMonthlyWorkSummaryByEmployeeAndMachine(
            @RequestParam @Parameter(description = "Employee name", example = "oumaima") String employerName,
            @RequestParam @Parameter(description = "Machine name", example = "press1") String machineName) {
        return traceService.calculateMonthlyWorkSummaryByEmployeeAndMachine(employerName,machineName);
    }
    @GetMapping("/findDistinctMachineName")
    @Operation(summary = "Get the list of machine names.",
            description = "Return a list of distinct machine names.")
    @PreAuthorize("hasRole('user-role')")
    public List<String> getAllMachineNames(
            @Parameter(description = "Employee name", example = "oumaima") @RequestParam(required = true) String employerName
    ) {
        return traceService.findDistinctMachineNameByEmployerName(employerName);
    }
    @GetMapping("/healthCheck")
    public String healthCheck() {
        return "The application is running";
    }
}
