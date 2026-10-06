package com.example.veterinaria.controller;

import com.example.veterinaria.dto.cita.CitaResponse;
import com.example.veterinaria.dto.cita.CreateCitaRequest;
import com.example.veterinaria.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<CitaResponse> createCita(@Valid @RequestBody CreateCitaRequest request) {
        CitaResponse response = citaService.createCita(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<CitaResponse> cancelarCita(@PathVariable Long id) {
        CitaResponse response = citaService.cancelarCita(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('VET', 'ADMIN')")
    public ResponseEntity<List<CitaResponse>> getAgenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long veterinarioId
    ) {
        List<CitaResponse> agenda = citaService.getAgenda(fecha, veterinarioId);
        return ResponseEntity.ok(agenda);
    }
}
