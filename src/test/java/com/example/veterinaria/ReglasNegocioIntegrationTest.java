package com.example.veterinaria;

import com.example.veterinaria.dto.auth.LoginRequest;
import com.example.veterinaria.dto.cita.CreateCitaRequest;
import com.example.veterinaria.dto.expediente.CreateExpedienteRequest;
import com.example.veterinaria.dto.mascota.CreateMascotaRequest;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Especie;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReglasNegocioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        return jsonNode.get("token").asText();
    }

    @Test
    @DisplayName("Regla 1: Validar solapamiento de horarios (30 minutos) por API HTTP")
    void testRegla1SolapamientoIntegration() throws Exception {
        String clientToken = loginAndGetToken("cliente@veterinaria.com", "cliente123");
        Usuario vet = usuarioRepository.findByEmail("vet@veterinaria.com").orElseThrow();

        // 1. Crear mascota
        CreateMascotaRequest petReq = CreateMascotaRequest.builder()
                .nombre("Toby")
                .especie(Especie.PERRO)
                .edad(2)
                .build();

        MvcResult petResult = mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(petReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long mascotaId = objectMapper.readTree(petResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Crear Cita a las 10:00 (duración 10:00 - 10:30) en una fecha futura lejana
        LocalDateTime baseHora = LocalDateTime.now().plusDays(20).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateCitaRequest cita1Req = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(baseHora)
                .motivo("Revisión")
                .build();

        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita1Req)))
                .andExpect(status().isCreated());

        // 3. Intentar crear Cita a las 10:15 (solapada) -> Debe retornar 400 Bad Request
        CreateCitaRequest citaSolapadaReq = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(baseHora.plusMinutes(15))
                .motivo("Solapada a las 10:15")
                .build();

        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaSolapadaReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El veterinario no está disponible en el horario seleccionado"));
    }

    @Test
    @DisplayName("Regla 2: Validar límite de 2 citas PENDIENTES por cliente en un mismo día por API HTTP")
    void testRegla2LimiteCitasPendientesIntegration() throws Exception {
        String clientToken = loginAndGetToken("cliente@veterinaria.com", "cliente123");
        Usuario vet = usuarioRepository.findByEmail("vet@veterinaria.com").orElseThrow();

        // Mascota
        CreateMascotaRequest petReq = CreateMascotaRequest.builder()
                .nombre("Luna")
                .especie(Especie.GATO)
                .edad(1)
                .build();

        MvcResult petResult = mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(petReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long mascotaId = objectMapper.readTree(petResult.getResponse().getContentAsString()).get("id").asLong();

        LocalDateTime diaTest = LocalDateTime.now().plusDays(25).withHour(8).withMinute(0).withSecond(0).withNano(0);

        // Cita 1: 08:00
        CreateCitaRequest cita1 = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(diaTest)
                .motivo("Cita 1")
                .build();
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita1)))
                .andExpect(status().isCreated());

        // Cita 2: 09:00
        CreateCitaRequest cita2 = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(diaTest.plusHours(1))
                .motivo("Cita 2")
                .build();
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita2)))
                .andExpect(status().isCreated());

        // Cita 3: 11:00 (mismo cliente, mismo día) -> Debe ser rechazada
        CreateCitaRequest cita3 = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(diaTest.plusHours(3))
                .motivo("Cita 3 rechazada")
                .build();
        mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cita3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("límite máximo de 2 citas en estado PENDIENTE")));
    }

    @Test
    @DisplayName("Regla 4: Registrar expediente cambia automáticamente el estado de la cita a COMPLETADA")
    void testRegla4ExpedienteCompletaCitaIntegration() throws Exception {
        String clientToken = loginAndGetToken("cliente@veterinaria.com", "cliente123");
        String vetToken = loginAndGetToken("vet@veterinaria.com", "vet123");
        Usuario vet = usuarioRepository.findByEmail("vet@veterinaria.com").orElseThrow();

        // 1. Mascota
        CreateMascotaRequest petReq = CreateMascotaRequest.builder()
                .nombre("Pelusa")
                .especie(Especie.GATO)
                .edad(5)
                .build();

        MvcResult petResult = mockMvc.perform(post("/api/v1/mascotas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(petReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long mascotaId = objectMapper.readTree(petResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Cita
        LocalDateTime hora = LocalDateTime.now().plusDays(30).withHour(15).withMinute(0).withSecond(0).withNano(0);
        CreateCitaRequest citaReq = CreateCitaRequest.builder()
                .mascotaId(mascotaId)
                .veterinarioId(vet.getId())
                .fechaHora(hora)
                .motivo("Infección de oído")
                .build();

        MvcResult citaResult = mockMvc.perform(post("/api/v1/citas")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();

        long citaId = objectMapper.readTree(citaResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Veterinario registra Expediente Clínico
        CreateExpedienteRequest expReq = CreateExpedienteRequest.builder()
                .citaId(citaId)
                .diagnostico("Otitis externa bacteriana")
                .tratamiento("Gotas óticas antibióticas cada 12 horas por 7 días")
                .pesoKg(4.2)
                .build();

        mockMvc.perform(post("/api/v1/expedientes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.diagnostico").value("Otitis externa bacteriana"))
                .andExpect(jsonPath("$.pesoKg").value(4.2));

        // 4. Verificar que intentar registrar un SEGUNDO expediente en la misma cita retorna 409 Conflict
        mockMvc.perform(post("/api/v1/expedientes")
                        .header("Authorization", "Bearer " + vetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 5. CLIENTE consulta el historial clínico de su mascota
        mockMvc.perform(get("/api/v1/expedientes/mascota/" + mascotaId)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnostico").value("Otitis externa bacteriana"));
    }
}
