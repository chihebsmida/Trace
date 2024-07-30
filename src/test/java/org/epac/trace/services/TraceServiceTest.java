package org.epac.trace.services;

import org.epac.trace.dto.WorkSummary;
import org.epac.trace.entity.Operation;
import org.epac.trace.entity.Trace;
import org.epac.trace.repository.TraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceServiceTest {
    private TraceRepository traceRepository;
    private TraceService traceService;
    @BeforeEach
    public void setUp() {
        traceRepository = mock(TraceRepository.class);
        traceService = new TraceService(traceRepository);
    }

    @Test
    void calculateDailyWorkSummaryByMachine() {
        LocalDate date = LocalDate.of(2024, 7, 27);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Trace> traces = List.of(
                new Trace(LocalDateTime.of(2024, 7, 27, 16, 50, 55),"oumaima", "machine3", Operation.START),
                new Trace(LocalDateTime.of(2024, 7, 27, 16, 51, 41),"oumaima", "machine3", Operation.PAUSE),
                new Trace(LocalDateTime.of(2024, 7, 27, 14, 33, 10),"oumaima", "machine2", Operation.START),
                new Trace(LocalDateTime.of(2024, 7, 27, 17, 33, 44),"oumaima", "machine2", Operation.PAUSE),
                new Trace(LocalDateTime.of(2024, 7, 27, 19, 35, 25),"oumaima", "machine2", Operation.STOP),
                new Trace(LocalDateTime.of(2024, 7, 26, 14, 8, 27),"oumaima", "machine1", Operation.PAUSE),
                new Trace(LocalDateTime.of(2024, 7, 27, 13, 25, 28),"oumaima", "machine1", Operation.START),
                new Trace(LocalDateTime.of(2024, 7, 27, 13, 28, 58),"oumaima", "machine1", Operation.STOP)
        );

        when(traceRepository.findByEmployerNameAndTimestampBetweenOrderByMachineNameAscTimestampAsc(eq("oumaima"), eq(startOfDay), eq(endOfDay)))
                .thenReturn(traces);

        Optional<Trace> lastTracePreviousDayOptMachine1 = Optional.of(new Trace( LocalDateTime.of(2024, 7, 26, 14, 8, 27), "oumaima", "machine1",Operation.PAUSE));
        when(traceRepository.findTopByEmployerNameAndMachineNameAndTimestampBetweenOrderByTimestampDesc(eq("oumaima"), eq("machine1"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(lastTracePreviousDayOptMachine1);

        Optional<Trace> lastTracePreviousDayOptMachine2 = Optional.ofNullable(null);
        when(traceRepository.findTopByEmployerNameAndMachineNameAndTimestampBetweenOrderByTimestampDesc(eq("oumaima"), eq("machine2"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(lastTracePreviousDayOptMachine2);

        Optional<Trace> lastTracePreviousDayOptMachine3 = Optional.ofNullable(null);
        when(traceRepository.findTopByEmployerNameAndMachineNameAndTimestampBetweenOrderByTimestampDesc(eq("oumaima"), eq("machine3"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(lastTracePreviousDayOptMachine3);

        Map<String, WorkSummary> workSummaryByMachine = traceService.calculateDailyWorkSummaryByMachine("oumaima", date);

        assertDurationEquals(Duration.ofSeconds(46), workSummaryByMachine.get("machine3").workDuration());
        assertDurationEquals(Duration.ofHours(7).plusMinutes(8).plusSeconds(18), workSummaryByMachine.get("machine3").pauseDuration());
        assertDurationEquals(Duration.ofHours(16).plusMinutes(50).plusSeconds(55), workSummaryByMachine.get("machine3").inactiveDuration());

        assertDurationEquals(Duration.ofHours(3).plusSeconds(34), workSummaryByMachine.get("machine2").workDuration());
        assertDurationEquals(Duration.ofHours(2).plusMinutes(1).plusSeconds(41), workSummaryByMachine.get("machine2").pauseDuration());
        assertDurationEquals(Duration.ofHours(18).plusMinutes(57).plusSeconds(44), workSummaryByMachine.get("machine2").inactiveDuration());

        assertDurationEquals(Duration.ofMinutes(3).plusSeconds(30), workSummaryByMachine.get("machine1").workDuration());
        assertDurationEquals(Duration.ofHours(13).plusMinutes(25).plusSeconds(28), workSummaryByMachine.get("machine1").pauseDuration());
        assertDurationEquals(Duration.ofHours(10).plusMinutes(31).plusSeconds(1), workSummaryByMachine.get("machine1").inactiveDuration());
    }


    // Helper method pour  comparaison Duration values
    private void assertDurationEquals(Duration expected, Duration actual) {
        long expectedSeconds = expected.getSeconds();
        long actualSeconds = actual.getSeconds();

        long expectedHours = expected.toHours();
        long actualHours = actual.toHours();
        long expectedMinutes = (expectedSeconds % 3600) / 60;
        long actualMinutes = (actualSeconds % 3600) / 60;
        long expectedSecondsOnly = expectedSeconds % 60;
        long actualSecondsOnly = actualSeconds % 60;

        assertEquals(expectedHours, actualHours, "Hours do not match");
        assertEquals(expectedMinutes, actualMinutes, "Minutes do not match");
        assertEquals(expectedSecondsOnly, actualSecondsOnly, "Seconds do not match");
    }


}