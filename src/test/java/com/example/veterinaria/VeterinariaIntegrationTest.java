package com.example.veterinaria;

import com.example.veterinaria.dto.auth.LoginRequest;
import com.example.veterinaria.dto.auth.RegisterRequest;
import com.example.veterinaria.dto.cita.CreateCitaRequest;
import com.example.veterinaria.dto.mascota.CreateMascotaRequest;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Especie;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VeterinariaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Flujo E2E: Registro, Login, Creación de Mascota, Solicitud de Cita y Control de Acceso (403/401)")
    void testFullFlow() throws Exception {
        // 1. Verificar que acceder a un recurso protegido sin token retorna 401 Unauthorized
        mockMvc.perform(get("/api/v1/mascotas/mis-mascotas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());

        // 2. Registro público de nuevo cliente
        RegisterRequest registerReq = RegisterRequest.builder()
                .nombre("Mariana Cliente Test")
                .email("mariana.test@example.com")
                .password("password123")
                .telefono("555-998877")
                .build();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String clientToken = registerJson.get("token").asText();

        // 3. Login de cliente recién registrado
        LoginRequest loginReq = LoginRequest.builder()
                .email("mariana.test@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));

        // 4. Verificación de Autorización (Sección 20):
        // Un CLIENTE autenticado NO debe poder acceder a GET /api/v1/citas/agenda -> 403 Forbidden
        mockMvc.perform(get("/api/v1/citas/agenda")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").exists());

        // 5. CLIENTE registra una mascota
        CreateMascotaRequest petReq = CreateMascotaRequest.builder()
                .nombre("Rocky")
                .especie(Especie.PERRO)
                .raza("Pastor Alemán")
                .edad(4)
                .build();

        MvcResult petResult = mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(petReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Rocky"))
                .andExpect(jsonPath("$.especie").value("PERRO"))
                .andReturn();

        JsonNode petJson = objectMapper.readTree(petResult.getResponse().getContentAsString());
        long mascotaId = petJson.get("id").asLong();

        // 6. CLIENTE consulta "mis mascotas"
        mockMvc.perform(get("/api/v1/mascotas/mis-mascotas")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Rocky"));

        // Obtener ID del veterinario semilla (creado por DataInitializer)
        Usuario vet = usuarioRepository.findByEmail("vet@veterinaria.com").orElseThrow();

        // 7. CLIENTE crea una cita médica
        LocalDateTime citaHora = LocalDateTime.now().plusDays(3).withHour(11).withMinute(0).withSecond(0).withNano(0);
        CreateCitaRequest citaReq = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(citaHora)
                .motivo("Vacunación anual y desparasitación")
                .build();

        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.mascotaNombre").value("Rocky"))
                .andExpect(jsonPath("$.veterinarioNombre").value(vet.getNombre()));
    }
}
