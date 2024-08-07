package org.epac.trace.repository;

import org.epac.trace.entity.Trace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    List<Trace> findAllByOrderByTimestamp();
    List<Trace> findAllByEmployerNameOrderByTimestampDesc(String employerName);
    List<Trace> findAllByMachineNameAndEmployerNameOrderByTimestamp(String machineName, String employerName);
    @Query("SELECT DISTINCT t.employerName FROM Trace t order by t.employerName asc")
    List<String> findDistinctEmployerName();
    @Query("SELECT DISTINCT t.machineName FROM Trace t")
    List<String> findDistinctMachineNames();
    @Query("SELECT DISTINCT t.machineName FROM Trace t where t.employerName = ?1")
    List<String> findDistinctMachineNameByEmployerName(String employerName);

    List<Trace> findByMachineNameAndTimestampBetweenOrderByTimestampAsc(String machineName, LocalDateTime start, LocalDateTime end);

    Optional<Trace> findTopByMachineNameAndTimestampBetweenOrderByTimestampDesc(String machineName, LocalDateTime start, LocalDateTime end);

    List<Trace> findAllByMachineNameOrderByTimestampDesc(String machineName);
}
