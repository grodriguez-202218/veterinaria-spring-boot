package com.example.veterinaria.dto.cita;

import com.example.veterinaria.enums.Especie;
import com.example.veterinaria.enums.EstadoCita;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaResponse {

    private Long id;
    private Long mascotaId;
    private String mascotaNombre;
    private Especie especie;
    private Long clienteId;
    private String clienteNombre;
    private Long veterinarioId;
    private String veterinarioNombre;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHoraFin;

    private String motivo;
    private EstadoCita estado;
}
