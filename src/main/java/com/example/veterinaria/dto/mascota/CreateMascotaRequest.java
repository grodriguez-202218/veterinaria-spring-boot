package com.example.veterinaria.dto.mascota;

import com.example.veterinaria.enums.Especie;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMascotaRequest {

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotNull(message = "La especie es obligatoria (PERRO, GATO, AVE, OTRO)")
    private Especie especie;

    @Size(max = 100, message = "La raza no puede exceder 100 caracteres")
    private String raza;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 0, message = "La edad no puede ser negativa")
    private Integer edad;

    // Opcional para CLIENTE (se asigna automáticamente al autenticado).
    // Requerido si quien registra es un ADMIN.
    private Long clienteId;
}
