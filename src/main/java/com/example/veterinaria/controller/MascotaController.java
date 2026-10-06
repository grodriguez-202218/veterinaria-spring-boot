package com.example.veterinaria.controller;

import com.example.veterinaria.dto.mascota.CreateMascotaRequest;
import com.example.veterinaria.dto.mascota.MascotaResponse;
import com.example.veterinaria.service.MascotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mascotas")
@RequiredArgsConstructor
public class MascotaController {

    private final MascotaService mascotaService;

    @GetMapping("/mis-mascotas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<MascotaResponse>> getMisMascotas() {
        List<MascotaResponse> mascotas = mascotaService.getMisMascotas();
        return ResponseEntity.ok(mascotas);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<MascotaResponse> createMascota(@Valid @RequestBody CreateMascotaRequest request) {
        MascotaResponse response = mascotaService.createMascota(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VET', 'ADMIN')")
    public ResponseEntity<MascotaResponse> getMascotaById(@PathVariable Long id) {
        MascotaResponse response = mascotaService.getMascotaById(id);
        return ResponseEntity.ok(response);
    }
}
