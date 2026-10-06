package com.example.veterinaria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping({"/", "/api/v1"})
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("sistema", "API REST - Sistema de Control de Citas de Clínica Veterinaria");
        info.put("version", "1.0.0");
        info.put("estado", "ONLINE");
        info.put("base_de_datos", "PostgreSQL (veterinaria_db)");
        info.put("documentacion", "Consulte README.md para detalles de todos los endpoints");

        Map<String, String> publicEndpoints = new HashMap<>();
        publicEndpoints.put("login", "POST /api/v1/auth/login");
        publicEndpoints.put("register", "POST /api/v1/auth/register");
        info.put("endpoints_publicos", publicEndpoints);

        List<Map<String, String>> usuariosDemo = List.of(
                Map.of("rol", "ADMIN", "email", "admin@veterinaria.com", "password", "admin123"),
                Map.of("rol", "VET", "email", "vet@veterinaria.com", "password", "vet123"),
                Map.of("rol", "CLIENTE", "email", "cliente@veterinaria.com", "password", "cliente123")
        );
        info.put("usuarios_demo", usuariosDemo);

        return ResponseEntity.ok(info);
    }
}
