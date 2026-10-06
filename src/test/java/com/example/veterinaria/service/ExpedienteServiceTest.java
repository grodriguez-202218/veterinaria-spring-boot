package com.example.veterinaria.service;

import com.example.veterinaria.dto.expediente.CreateExpedienteRequest;
import com.example.veterinaria.dto.expediente.ExpedienteResponse;
import com.example.veterinaria.entity.CitaMedica;
import com.example.veterinaria.entity.ExpedienteClinico;
import com.example.veterinaria.entity.Mascota;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Especie;
import com.example.veterinaria.enums.EstadoCita;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.DuplicateResourceException;
import com.example.veterinaria.exception.ForbiddenOperationException;
import com.example.veterinaria.exception.ReglaNegocioException;
import com.example.veterinaria.repository.CitaMedicaRepository;
import com.example.veterinaria.repository.ExpedienteClinicoRepository;
import com.example.veterinaria.repository.MascotaRepository;
import com.example.veterinaria.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpedienteServiceTest {

    @Mock
    private ExpedienteClinicoRepository expedienteClinicoRepository;

    @Mock
    private CitaMedicaRepository citaMedicaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ExpedienteService expedienteService;

    private Usuario cliente;
    private Usuario veterinario;
    private Mascota mascota;
    private CitaMedica cita;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder()
                .id(1L)
                .nombre("Carlos Cliente")
                .email("carlos@example.com")
                .rol(Rol.CLIENTE)
                .build();

        veterinario = Usuario.builder()
                .id(2L)
                .nombre("Dr. Roberto")
                .email("vet@example.com")
                .rol(Rol.VET)
                .build();

        mascota = Mascota.builder()
                .id(10L)
                .nombre("Firulais")
                .especie(Especie.PERRO)
                .cliente(cliente)
                .build();

        cita = CitaMedica.builder()
                .id(100L)
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(LocalDateTime.now().minusHours(1))
                .motivo("Gastroenteritis")
                .estado(EstadoCita.PENDIENTE)
                .build();
    }

    @Test
    @DisplayName("Regla 4: Registrar expediente actualiza automáticamente la cita a COMPLETADA")
    void testCreateExpedienteSuccess() {
        CreateExpedienteRequest request = CreateExpedienteRequest.builder()
                .citaId(100L)
                .diagnostico("Gastroenteritis leve")
                .tratamiento("Suero y dieta blanda por 48h")
                .pesoKg(12.5)
                .build();

        when(citaMedicaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(expedienteClinicoRepository.existsByCitaId(100L)).thenReturn(false);

        ExpedienteClinico savedExpediente = ExpedienteClinico.builder()
                .id(500L)
                .cita(cita)
                .diagnostico(request.getDiagnostico())
                .tratamiento(request.getTratamiento())
                .pesoKg(request.getPesoKg())
                .fechaRegistro(LocalDateTime.now())
                .build();

        when(expedienteClinicoRepository.save(any(ExpedienteClinico.class))).thenReturn(savedExpediente);

        ExpedienteResponse response = expedienteService.createExpediente(request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals("Gastroenteritis leve", response.getDiagnostico());
        assertEquals(12.5, response.getPesoKg());

        // Verificar cambio de estado a COMPLETADA
        assertEquals(EstadoCita.COMPLETADA, cita.getEstado());
        verify(citaMedicaRepository).save(cita);
    }

    @Test
    @DisplayName("Regla 4: Rechazar creación de expediente si la cita ya tiene uno registrado")
    void testCreateExpedienteDuplicate() {
        CreateExpedienteRequest request = CreateExpedienteRequest.builder()
                .citaId(100L)
                .diagnostico("Diagnóstico repetido")
                .tratamiento("Tratamiento")
                .pesoKg(10.0)
                .build();

        when(citaMedicaRepository.findById(100L)).thenReturn(Optional.of(cita));
        when(expedienteClinicoRepository.existsByCitaId(100L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> expedienteService.createExpediente(request));
        verify(expedienteClinicoRepository, never()).save(any());
        verify(citaMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazar creación de expediente si la cita se encuentra cancelada")
    void testCreateExpedienteForCancelledCita() {
        cita.setEstado(EstadoCita.CANCELADA);

        CreateExpedienteRequest request = CreateExpedienteRequest.builder()
                .citaId(100L)
                .diagnostico("Diagnóstico")
                .tratamiento("Tratamiento")
                .pesoKg(10.0)
                .build();

        when(citaMedicaRepository.findById(100L)).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class, () -> expedienteService.createExpediente(request));
    }

    @Test
    @DisplayName("CLIENTE no puede consultar el historial de mascotas de otros clientes")
    void testGetHistorialOtherClientForbidden() {
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota)); // Dueño es cliente id=1
        when(securityUtils.isCliente()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(99L); // Usuario actual es id=99

        assertThrows(ForbiddenOperationException.class, () -> expedienteService.getHistorialByMascotaId(10L));
    }

    @Test
    @DisplayName("CLIENTE puede consultar el historial clínico de sus propias mascotas")
    void testGetHistorialOwnPetSuccess() {
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(securityUtils.isCliente()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        ExpedienteClinico expediente = ExpedienteClinico.builder()
                .id(1L)
                .cita(cita)
                .diagnostico("Chequeo anual")
                .tratamiento("Vitaminas")
                .pesoKg(14.0)
                .fechaRegistro(LocalDateTime.now())
                .build();

        when(expedienteClinicoRepository.findByMascotaIdOrderByFechaRegistroDesc(10L))
                .thenReturn(List.of(expediente));

        List<ExpedienteResponse> historial = expedienteService.getHistorialByMascotaId(10L);

        assertEquals(1, historial.size());
        assertEquals("Chequeo anual", historial.get(0).getDiagnostico());
    }
}
