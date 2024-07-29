package org.epac.trace.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Trace {
    @Id
    @CreatedDate()
    @Column(nullable = false,unique = true)
    @Schema(description = "temp de l'execution de l'Opération à réaliser calculer automatiquement", required = false)
    private LocalDateTime timestamp;
    @Column(nullable = false)
    private String employerName;
    @Column(nullable = false)
    private String machineName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Operation operation;
}
