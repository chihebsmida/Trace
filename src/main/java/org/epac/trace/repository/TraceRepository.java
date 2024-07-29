package org.epac.trace.repository;

import org.epac.trace.entity.Trace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TraceRepository extends JpaRepository<Trace, LocalDateTime> {
    List<Trace> findByEmployerNameAndTimestampBetweenOrderByTimestampAsc(String employerName, LocalDateTime start, LocalDateTime end);
    List<Trace> findByEmployerNameAndTimestampBetweenOrderByMachineNameAscTimestampAsc(String employerName, LocalDateTime start, LocalDateTime end);
    Optional<Trace> findTopByEmployerNameAndTimestampBetweenOrderByTimestampDesc(String employerName, LocalDateTime start, LocalDateTime end);
    Optional<Trace> findTopByEmployerNameAndMachineNameAndTimestampBetweenOrderByTimestampDesc(String employerName,String machineName, LocalDateTime start, LocalDateTime end);

    Optional<Trace> findTopByEmployerNameOrderByTimestampDesc(String employerName);


    List<Trace> findByTimestampBetween(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    Optional<Trace> findTopByMachineNameOrderByTimestampDesc(String machineName);
}
