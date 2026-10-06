package com.example.veterinaria.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "expedientes_clinicos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_expediente_cita", columnNames = "cita_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteClinico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_expediente_cita"))
    private CitaMedica cita;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnostico;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tratamiento;

    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
