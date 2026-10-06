package com.example.veterinaria.dto.expediente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExpedienteRequest {

    @NotNull(message = "El identificador de la cita es obligatorio")
    private Long citaId;

    @NotBlank(message = "El diagnóstico es obligatorio")
    private String diagnostico;

    @NotBlank(message = "El tratamiento es obligatorio")
    private String tratamiento;

    @NotNull(message = "El peso en kilogramos es obligatorio")
    @Positive(message = "El peso debe ser un número positivo mayor que cero")
    private Double pesoKg;
}
