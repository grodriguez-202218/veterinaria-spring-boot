package com.example.veterinaria.dto.expediente;

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
public class ExpedienteResponse {

    private Long id;
    private Long citaId;
    private Long mascotaId;
    private String mascotaNombre;
    private Long clienteId;
    private String clienteNombre;
    private Long veterinarioId;
    private String veterinarioNombre;
    private String diagnostico;
    private String tratamiento;
    private Double pesoKg;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaRegistro;
}
