package org.epac.trace.services;

import lombok.AllArgsConstructor;
import org.epac.trace.dto.WorkSummary;
import org.epac.trace.entity.Operation;
import org.epac.trace.entity.Trace;
import org.epac.trace.exception.InvalidTraceOperationException;
import org.epac.trace.repository.TraceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TraceService {
    private final TraceRepository traceRepository;



    public Map<String, WorkSummary> addTrace(Trace trace) throws InvalidTraceOperationException {
        // Récupérer la dernière opération de l'employé
        Optional<Trace> lastTraceOpt = traceRepository.findTopByEmployerNameOrderByTimestampDesc(trace.getEmployerName());
        Optional<Trace> lastTraceOptMachine = traceRepository.findTopByMachineNameOrderByTimestampDesc(trace.getMachineName());
 // tester si la machine est en marche ou pause par autre employé
        if (lastTraceOptMachine.isPresent()) {
            Trace lastTraceMachine = lastTraceOptMachine.get();
            if ((lastTraceMachine.getOperation() == Operation.START || lastTraceMachine.getOperation() == Operation.PAUSE)&&!lastTraceMachine.getEmployerName().equals(trace.getEmployerName())){
                throw new InvalidTraceOperationException("La machine est en marche par un autre employé.");
            }
        }
        if (lastTraceOpt.isPresent()) {
            Trace lastTrace = lastTraceOpt.get();

            // Vérifier si la dernière opération est sur une machine différente
            if (!lastTrace.getMachineName().equals(trace.getMachineName())) {
                if (lastTrace.getOperation() != Operation.STOP) {
                    throw new InvalidTraceOperationException("La dernière opération sur une autre machine doit être STOP.");
                }
            } else {
                // Si la dernière opération est sur la même machine
                if (lastTrace.getOperation() == Operation.STOP && trace.getOperation() != Operation.START) {
                    throw new InvalidTraceOperationException("Après une opération STOP, l'opération attendue est START.");
                }
            }
        }

        // Sauvegarder la nouvelle trace
        traceRepository.save(trace);
        return calculateDailyWorkSummaryByMachine(trace.getEmployerName(), LocalDate.now());
    }
    public WorkSummary calculateDailyWorkSummary(String employerName, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Récupérer les opérations du jour actuel
        List<Trace> traces = traceRepository.findByEmployerNameAndTimestampBetweenOrderByTimestampAsc(employerName, startOfDay, endOfDay);
       if (traces.isEmpty())
           return new WorkSummary(Duration.ZERO, Duration.ZERO, Duration.ofDays(1));
        // Récupérer la dernière opération du jour précédent
        LocalDateTime startOfPreviousDay = startOfDay.minusDays(1);
        LocalDateTime endOfPreviousDay = startOfDay.minusNanos(1);
        Optional<Trace> lastTracePreviousDayOpt = traceRepository.findTopByEmployerNameAndTimestampBetweenOrderByTimestampDesc(
                employerName, startOfPreviousDay, endOfPreviousDay);

        Duration workDuration = Duration.ZERO;
        Duration pauseDuration = Duration.ZERO;
        LocalDateTime lastTimestamp = null;
        Operation lastOperation = null;

        // Traiter la dernière opération du jour précédent si elle existe
        if (lastTracePreviousDayOpt.isPresent()) {
            Trace lastTracePreviousDay = lastTracePreviousDayOpt.get();
            lastTimestamp = lastTracePreviousDay.getTimestamp();
            lastOperation = lastTracePreviousDay.getOperation();

            // Si la dernière opération du jour précédent est de type START
            if (lastOperation == Operation.START) {
                // Calculer la durée de travail de minuit à la première opération du jour actuel
                if (!traces.isEmpty()) {
                    LocalDateTime firstOperationTime = traces.get(0).getTimestamp();
                    Duration duration = Duration.between(startOfDay, firstOperationTime);
                    workDuration = workDuration.plus(duration);
                }
            } else if (lastOperation == Operation.PAUSE && !traces.isEmpty()) {
                // Si la dernière opération du jour précédent est de type PAUSE
                // Calculer la durée de pause de minuit à la première opération du jour actuel
                LocalDateTime firstOperationTime = traces.get(0).getTimestamp();
                Duration duration = Duration.between(startOfDay, firstOperationTime);
                pauseDuration = pauseDuration.plus(duration);
            }

            // Vérifier si la liste traces n'est pas vide avant d'accéder à son premier élément
            if (!traces.isEmpty()) {
                lastTimestamp = traces.get(0).getTimestamp();
            }
        }

        // Traiter les opérations du jour actuel
        for (Trace trace : traces) {
            if (lastTimestamp != null) {
                Duration duration = Duration.between(lastTimestamp, trace.getTimestamp());

                if (lastOperation == Operation.START) {
                    workDuration = workDuration.plus(duration);
                } else if (lastOperation == Operation.PAUSE) {
                    pauseDuration = pauseDuration.plus(duration);
                }
            }

            lastTimestamp = trace.getTimestamp();
            lastOperation = trace.getOperation();
        }

        // Si la dernière opération est de type START
        if (lastOperation == Operation.START) {
            // Calculer la durée de travail de la dernière opération du jour actuel à la fin de la journée
            Duration duration = Duration.between(lastTimestamp, endOfDay);
            workDuration = workDuration.plus(duration);
        } else if (lastOperation == Operation.PAUSE) {
            // Si la dernière opération est de type PAUSE
            // Calculer la durée de pause de la dernière opération du jour actuel à la fin de la journée
            Duration duration = Duration.between(lastTimestamp, endOfDay);
            pauseDuration = pauseDuration.plus(duration);
        }

        // Calculer la durée inactive
        Duration inactiveDuration = Duration.between(startOfDay, endOfDay)
                .minus(workDuration)
                .minus(pauseDuration);

        return new WorkSummary(workDuration, pauseDuration, inactiveDuration);
    }

    public Map<String, WorkSummary> calculateDailyWorkSummaryByMachine(String employerName, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Récupérer les opérations du jour actuel, triées par machine et par timestamp
        List<Trace> traces = traceRepository.findByEmployerNameAndTimestampBetweenOrderByMachineNameAscTimestampAsc(employerName, startOfDay, endOfDay);
       if(traces.isEmpty())
           return new HashMap<>();
        // Initialisation des maps pour garder les traces par machine
        Map<String, List<Trace>> tracesByMachine = new HashMap<>();
        for (Trace trace : traces) {
            tracesByMachine.computeIfAbsent(trace.getMachineName(), k -> new ArrayList<>()).add(trace);
        }

        // Calculer le résumé de travail pour chaque machine
        Map<String, WorkSummary> workSummaryByMachine = new HashMap<>();

        for (Map.Entry<String, List<Trace>> entry : tracesByMachine.entrySet()) {
            String machine = entry.getKey();
            List<Trace> machineTraces = entry.getValue();

            // Récupérer la dernière opération du jour précédent pour cette machine
            LocalDateTime startOfPreviousDay = startOfDay.minusDays(1);
            LocalDateTime endOfPreviousDay = startOfDay.minusNanos(1);
            Optional<Trace> lastTracePreviousDayOpt = traceRepository.findTopByEmployerNameAndMachineNameAndTimestampBetweenOrderByTimestampDesc(
                    employerName, machine, startOfPreviousDay, endOfPreviousDay);

            Duration workDuration = Duration.ZERO;
            Duration pauseDuration = Duration.ZERO;
            LocalDateTime lastTimestamp = null;
            Operation lastOperation = null;

            // Traiter la dernière opération du jour précédent si elle existe
            if (lastTracePreviousDayOpt.isPresent()) {
                Trace lastTracePreviousDay = lastTracePreviousDayOpt.get();
                lastOperation = lastTracePreviousDay.getOperation();

                // Si la dernière opération du jour précédent est de type START
                if (lastOperation == Operation.START) {
                    // Calculer la durée de travail de minuit à la première opération du jour actuel
                    if (!machineTraces.isEmpty()) {
                        LocalDateTime firstOperationTime = machineTraces.get(0).getTimestamp();
                        Duration duration = Duration.between(startOfDay, firstOperationTime);
                        workDuration = workDuration.plus(duration);
                    }
                } else if (lastOperation == Operation.PAUSE && !machineTraces.isEmpty()) {
                    // Si la dernière opération du jour précédent est de type PAUSE
                    // Calculer la durée de pause de minuit à la première opération du jour actuel
                        LocalDateTime firstOperationTime = machineTraces.get(0).getTimestamp();
                        Duration duration = Duration.between(startOfDay, firstOperationTime);
                        pauseDuration = pauseDuration.plus(duration);

                }
                lastTimestamp = machineTraces.get(0).getTimestamp();

            }

            // Traiter les opérations du jour actuel
            for (Trace trace : machineTraces) {
                if (lastTimestamp != null) {
                    Duration duration = Duration.between(lastTimestamp, trace.getTimestamp());

                    if (lastOperation == Operation.START) {
                        workDuration = workDuration.plus(duration);
                    } else if (lastOperation == Operation.PAUSE) {
                        pauseDuration = pauseDuration.plus(duration);
                    }
                }

                lastTimestamp = trace.getTimestamp();
                lastOperation = trace.getOperation();
            }
            //si la dernière opération est de type START
            if (lastOperation == Operation.START) {
                // Calculer la durée de travail de la dernière opération du jour actuel à la fin de la journée
                Duration duration = Duration.between(lastTimestamp, endOfDay);
                workDuration = workDuration.plus(duration);
            } else if (lastOperation == Operation.PAUSE) {
                // Si la dernière opération est de type PAUSE
                // Calculer la durée de pause de la dernière opération du jour actuel à la fin de la journée
                Duration duration = Duration.between(lastTimestamp, endOfDay);
                pauseDuration = pauseDuration.plus(duration);
            }

            // Calculer la durée inactive
            Duration inactiveDuration = Duration.between(startOfDay, endOfDay)
                    .minus(workDuration)
                    .minus(pauseDuration);

            workSummaryByMachine.put(machine, new WorkSummary(workDuration, pauseDuration, inactiveDuration));
        }
        return workSummaryByMachine;
    }


    public Map<String, Map<String, WorkSummary>> getWorkSummaryByEmployeeAndMachine(LocalDate date) {
        // Initialisation des cartes
        Map<String, Map<String, WorkSummary>> workSummaryByEmployee = new HashMap<>();

        // Récupérer les traces pour tous les employés et la date spécifiée
        List<Trace> traces = traceRepository.findByTimestampBetween(date.atStartOfDay(), date.atTime(LocalTime.MAX));

        // Grouper les traces par employé et machine
        Map<String, Map<String, List<Trace>>> tracesByEmployeeAndMachine = traces.stream()
                .collect(Collectors.groupingBy(
                        Trace::getEmployerName, // Clé externe : nom de l'employé
                        Collectors.groupingBy(
                                Trace::getMachineName, // Clé interne : nom de la machine
                                Collectors.toList() // Liste des traces pour chaque machine
                        )
                ));

        // Calculer le résumé du travail pour chaque employé et machine
        for (Map.Entry<String, Map<String, List<Trace>>> employeeEntry : tracesByEmployeeAndMachine.entrySet()) {
            String employerName = employeeEntry.getKey();
            Map<String, List<Trace>> tracesByMachine = employeeEntry.getValue();
            Map<String, WorkSummary> workSummaryByMachine = new HashMap<>();

            for (Map.Entry<String, List<Trace>> machineEntry : tracesByMachine.entrySet()) {
                String machineName = machineEntry.getKey();
                // Calculer le résumé de travail pour la machine
                WorkSummary workSummary = calculateDailyWorkSummaryByMachine(employerName, date).get(machineName);
                workSummaryByMachine.put(machineName, workSummary);
            }

            workSummaryByEmployee.put(employerName, workSummaryByMachine);
        }

        return workSummaryByEmployee;
    }

    public Map<String, Map<String, WorkSummary>> getWorkSummaryByMachineAndEmployee(LocalDate date) {
    // Initialisation des cartes
        Map<String, Map<String, WorkSummary>> workSummaryByMachine = new HashMap<>();
    // Récupérer les traces pour tous les employés et la date spécifiée
        List<Trace> traces = traceRepository.findByTimestampBetween(date.atStartOfDay(), date.atTime(LocalTime.MAX));
    // Grouper les traces par machine et employé
        Map<String, Map<String, List<Trace>>> tracesByMachineAndEmployee = traces.stream().collect(Collectors.groupingBy(Trace::getMachineName,
                Collectors.groupingBy(Trace::getEmployerName,
                        Collectors.toList()
    // Liste des traces pour chaque employé
                )));
    // Calculer le résumé du travail pour chaque machine et employé
        for (Map.Entry<String, Map<String, List<Trace>>> machineEntry : tracesByMachineAndEmployee.entrySet()) {
            String machineName = machineEntry.getKey();
            Map<String, List<Trace>> tracesByEmployee = machineEntry.getValue();
            Map<String, WorkSummary> workSummaryByEmployee = new HashMap<>();
            for (Map.Entry<String, List<Trace>> employeeEntry : tracesByEmployee.entrySet()) {
                String employerName = employeeEntry.getKey();
    // Calculer le résumé de travail pour l'employé
                WorkSummary workSummary = calculateDailyWorkSummaryByMachine(employerName, date).get(machineName);
                workSummaryByEmployee.put(employerName, workSummary);
            }
            workSummaryByMachine.put(machineName, workSummaryByEmployee);
        }
        return workSummaryByMachine;
    }
    public Map<LocalDate, WorkSummary> calculateDailyWorkSummaryByEmployee(String employerName) {
        // Récupérer toutes les traces de la base de données
        List<Trace> traces = traceRepository.findAllByEmployerNameOrderByTimestampDesc(employerName);

        // Grouper les traces par jours
        Map<LocalDate, List<Trace>> tracesByDay = traces.stream()
                .collect(Collectors.groupingBy(trace -> LocalDate.from(trace.getTimestamp().toLocalDate().atStartOfDay())));

        // Utiliser un TreeMap pour que les jours soient ordonnées (plus de complexité mais c'est obligatoire pour les chart
        Map<LocalDate, WorkSummary> dailyWorkSummary = new TreeMap<>();
        for (Map.Entry<LocalDate, List<Trace>> dayEntry : tracesByDay.entrySet()) {
            LocalDate startOfDay = dayEntry.getKey();
            dailyWorkSummary.put(startOfDay, calculateDailyWorkSummary(employerName, startOfDay));
        }
        return dailyWorkSummary;
    }

    public Map<LocalDate, WorkSummary> calculateWeeklyWorkSummaryByEmployee(String employerName) {
        // Récupérer toutes les traces de la base de données
        List<Trace> traces = traceRepository.findAllByEmployerNameOrderByTimestampDesc(employerName);

        // Grouper les traces par semaine
        Map<LocalDate, List<Trace>> tracesByWeek = traces.stream()
                .collect(Collectors.groupingBy(trace -> trace.getTimestamp().toLocalDate().with(java.time.DayOfWeek.MONDAY)));

        // Utiliser un TreeMap pour que les semaines soient ordonnées (plus de complexité mais c'est obligatoire pour les chart
        Map<LocalDate, WorkSummary> weeklyWorkSummary = new TreeMap<>();

        for (Map.Entry<LocalDate, List<Trace>> weekEntry : tracesByWeek.entrySet()) {
            LocalDate startOfWeek = weekEntry.getKey();
            LocalDate endOfWeek = startOfWeek.plusDays(6);

            calculerWorkSummarybyemployerAndIntervalDate(employerName, weeklyWorkSummary, startOfWeek, endOfWeek);
        }

        return weeklyWorkSummary;
    }


    public Map<LocalDate, WorkSummary> calculateMonthlyWorkSummaryByEmployee(String employerName) {
        // Récupérer toutes les traces de la base de données
        List<Trace> traces = traceRepository.findAllByEmployerNameOrderByTimestampDesc(employerName);

        // Grouper les traces par mois
        Map<LocalDate, List<Trace>> tracesByMonth = traces.stream()
                .collect(Collectors.groupingBy(trace -> trace.getTimestamp().toLocalDate().withDayOfMonth(1)));

        Map<LocalDate, WorkSummary> monthlyWorkSummary = new HashMap<>();

        for (Map.Entry<LocalDate, List<Trace>> monthEntry : tracesByMonth.entrySet()) {
            LocalDate startOfMonth = monthEntry.getKey();
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

            calculerWorkSummarybyemployerAndIntervalDate(employerName, monthlyWorkSummary, startOfMonth, endOfMonth);
        }
        return monthlyWorkSummary;
    }
// Méthode pour calculer le résumé du travail pour un employeur et un intervalle de dates donnés
    private void calculerWorkSummarybyemployerAndIntervalDate(String employerName, Map<LocalDate, WorkSummary> monthlyWorkSummary, LocalDate startOfMonth, LocalDate endOfMonth) {
        Duration totalWorkDuration = Duration.ZERO;
        Duration totalPauseDuration = Duration.ZERO;
        Duration totalInactiveDuration = Duration.ZERO;

        for (LocalDate date = startOfMonth; !date.isAfter(endOfMonth); date = date.plusDays(1)) {
            WorkSummary dailySummary = calculateDailyWorkSummary(employerName, date);
            totalWorkDuration = totalWorkDuration.plus(dailySummary.workDuration());
            totalPauseDuration = totalPauseDuration.plus(dailySummary.pauseDuration());
            totalInactiveDuration = totalInactiveDuration.plus(dailySummary.inactiveDuration());
        }

        monthlyWorkSummary.put(startOfMonth, new WorkSummary(totalWorkDuration, totalPauseDuration, totalInactiveDuration));
    }
    public Map<String, Map<LocalDate, WorkSummary>> calculateDailyWorkSummaryForAllEmployees()
    {
        // Récupérer toutes les traces de la base de données
        List<String> employerName = traceRepository.findDistinctEmployerName();
        Map<String, Map<LocalDate, WorkSummary>> dailyWorkSummaryByEmployee = new HashMap<>();
        for (String employer : employerName) {
            Map<LocalDate, WorkSummary> dailyWorkSummary = calculateDailyWorkSummaryByEmployee(employer);
            dailyWorkSummaryByEmployee.put(employer, dailyWorkSummary);
        }
        return dailyWorkSummaryByEmployee;
    }
    public Map<String, Map<LocalDate, WorkSummary>> calculateWeeklyWorkSummaryForAllEmployees() {
        // Récupérer toutes les traces de la base de données
        List<String> employerName = traceRepository.findDistinctEmployerName();
        Map<String, Map<LocalDate, WorkSummary>> weeklyWorkSummaryByEmployee = new HashMap<>();
        for (String employer : employerName) {
            Map<LocalDate, WorkSummary> weeklyWorkSummary = calculateWeeklyWorkSummaryByEmployee(employer);
            weeklyWorkSummaryByEmployee.put(employer, weeklyWorkSummary);
        }
        return weeklyWorkSummaryByEmployee;
    }

    public Map<String, Map<LocalDate, WorkSummary>> calculateMonthlyWorkSummaryForAllEmployees() {
        // Récupérer toutes les employer name de la base de données
        List<String> employerName = traceRepository.findDistinctEmployerName();
        Map<String, Map<LocalDate, WorkSummary>> monthlyWorkSummaryByEmployee = new TreeMap<>();
        for (String employer : employerName) {
            Map<LocalDate, WorkSummary> monthlyWorkSummary = calculateMonthlyWorkSummaryByEmployee(employer);
            monthlyWorkSummaryByEmployee.put(employer, monthlyWorkSummary);
        }
        return monthlyWorkSummaryByEmployee;
    }


    public Map<LocalDate, WorkSummary> calculateDailyWorkSummaryByEmployeeAndMachine(String employerName,String machineName) {
        // Récupérer toutes les traces de l'employé
        List<Trace> traces = traceRepository.findAllByMachineNameAndEmployerNameOrderByTimestamp(machineName,employerName);
            // Grouper les traces par jour
            Map<LocalDate, List<Trace>> tracesByDate = traces.stream()
                    .collect(Collectors.groupingBy(trace -> trace.getTimestamp().toLocalDate()));
            Map<LocalDate, WorkSummary> dailyWorkSummary = new HashMap<>();
            for (Map.Entry<LocalDate, List<Trace>> dateEntry : tracesByDate.entrySet()) {
                LocalDate date = dateEntry.getKey();
                // Calculer le résumé de travail pour chaque jour puisque la liste trace findByMachineName alors le map dailyWorkSummary contient une seule valeur c'est la machineName
                WorkSummary workSummary = calculateDailyWorkSummaryByMachine(employerName, date).get(machineName);
                dailyWorkSummary.put(date, workSummary);
            }
return dailyWorkSummary;

    }
    public Map<LocalDate, WorkSummary> calculateWeeklyWorkSummaryByEmployeeAndMachine(String employerName, String machineName) {
        // Récupérer toutes les traces de l'employé
        List<Trace> traces = traceRepository.findAllByMachineNameAndEmployerNameOrderByTimestamp(machineName, employerName);
        // Grouper les traces par semaine
        Map<LocalDate, List<Trace>> tracesByWeek = traces.stream()
                .collect(Collectors.groupingBy(trace -> trace.getTimestamp().toLocalDate().with(java.time.DayOfWeek.MONDAY)));
        // Utiliser un TreeMap pour que les semaines soient ordonnées (plus de complexité mais c'est obligatoire pour les chart
        Map<LocalDate, WorkSummary> weeklyWorkSummary = new TreeMap<>();
        for (Map.Entry<LocalDate, List<Trace>> weekEntry : tracesByWeek.entrySet()) {
            LocalDate startOfWeek = weekEntry.getKey();
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            calculerWorkSummarybyemployerAndIntervalDate(employerName, weeklyWorkSummary, startOfWeek, endOfWeek);
        }
        return weeklyWorkSummary;
    }

    public Map<LocalDate, WorkSummary> calculateMonthlyWorkSummaryByEmployeeAndMachine(String employerName, String machineName) {
        // Récupérer toutes les traces de l'employé
        List<Trace> traces = traceRepository.findAllByMachineNameAndEmployerNameOrderByTimestamp(machineName, employerName);
        // Grouper les traces par mois
        Map<LocalDate, List<Trace>> tracesByMonth = traces.stream()
                .collect(Collectors.groupingBy(trace -> trace.getTimestamp().toLocalDate().withDayOfMonth(1)));
        Map<LocalDate, WorkSummary> monthlyWorkSummary = new HashMap<>();
        for (Map.Entry<LocalDate, List<Trace>> monthEntry : tracesByMonth.entrySet()) {
            LocalDate startOfMonth = monthEntry.getKey();
            LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
            calculerWorkSummarybyemployerAndIntervalDate(employerName, monthlyWorkSummary, startOfMonth, endOfMonth);
        }
        return monthlyWorkSummary;
    }
    public List<String> findDistinctMachineNameByEmployerName(String employerName) {
        return traceRepository.findDistinctMachineNameByEmployerName(employerName);
    }



}
