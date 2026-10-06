package com.example.veterinaria.controller;

import com.example.veterinaria.dto.expediente.CreateExpedienteRequest;
import com.example.veterinaria.dto.expediente.ExpedienteResponse;
import com.example.veterinaria.service.ExpedienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expedientes")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteService expedienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('VET', 'ADMIN')")
    public ResponseEntity<ExpedienteResponse> createExpediente(@Valid @RequestBody CreateExpedienteRequest request) {
        ExpedienteResponse response = expedienteService.createExpediente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize("hasAnyRole('VET', 'CLIENTE', 'ADMIN')")
    public ResponseEntity<List<ExpedienteResponse>> getHistorialByMascotaId(@PathVariable Long mascotaId) {
        List<ExpedienteResponse> historial = expedienteService.getHistorialByMascotaId(mascotaId);
        return ResponseEntity.ok(historial);
    }
}
