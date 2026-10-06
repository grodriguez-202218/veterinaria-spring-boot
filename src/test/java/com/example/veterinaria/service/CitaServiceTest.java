package com.example.veterinaria.service;

import com.example.veterinaria.dto.cita.CitaResponse;
import com.example.veterinaria.dto.cita.CreateCitaRequest;
import com.example.veterinaria.entity.CitaMedica;
import com.example.veterinaria.entity.Mascota;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Especie;
import com.example.veterinaria.enums.EstadoCita;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.ForbiddenOperationException;
import com.example.veterinaria.exception.ReglaNegocioException;
import com.example.veterinaria.repository.CitaMedicaRepository;
import com.example.veterinaria.repository.MascotaRepository;
import com.example.veterinaria.repository.UsuarioRepository;
import com.example.veterinaria.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaMedicaRepository citaMedicaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CitaService citaService;

    private Usuario cliente;
    private Usuario veterinario;
    private Mascota mascota;
    private LocalDateTime futureDate;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder()
                .id(1L)
                .nombre("Carlos Cliente")
                .email("cliente@example.com")
                .rol(Rol.CLIENTE)
                .build();

        veterinario = Usuario.builder()
                .id(2L)
                .nombre("Dr. Veterinario")
                .email("vet@example.com")
                .rol(Rol.VET)
                .build();

        mascota = Mascota.builder()
                .id(10L)
                .nombre("Firulais")
                .especie(Especie.PERRO)
                .edad(3)
                .cliente(cliente)
                .build();

        futureDate = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Crear cita exitosa cuando cumple todas las reglas")
    void testCreateCitaSuccess() {
        CreateCitaRequest request = CreateCitaRequest.builder()
                .mascotaId(10L)
                .veterinarioId(2L)
                .fechaHora(futureDate)
                .motivo("Revisión general")
                .build();

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.isCliente()).thenReturn(true);
        when(citaMedicaRepository.existsOverlappingCita(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(citaMedicaRepository.countCitasPendientesClienteEnDia(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        CitaMedica savedCita = CitaMedica.builder()
                .id(100L)
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(futureDate)
                .motivo("Revisión general")
                .estado(EstadoCita.PENDIENTE)
                .build();

        when(citaMedicaRepository.save(any(CitaMedica.class))).thenReturn(savedCita);

        CitaResponse response = citaService.createCita(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(EstadoCita.PENDIENTE, response.getEstado());
        assertEquals(futureDate, response.getFechaHora());
        assertEquals(futureDate.plusMinutes(30), response.getFechaHoraFin());
    }

    @Test
    @DisplayName("Regla 1: Rechazar cita si existe solapamiento temporal con otra cita del veterinario")
    void testCreateCitaOverlappingVetSchedule() {
        CreateCitaRequest request = CreateCitaRequest.builder()
                .mascotaId(10L)
                .veterinarioId(2L)
                .fechaHora(futureDate)
                .motivo("Vacunación")
                .build();

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.isCliente()).thenReturn(true);
        // Simular que el veterinario ya tiene una cita que se superpone
        when(citaMedicaRepository.existsOverlappingCita(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        ReglaNegocioException exception = assertThrows(
                ReglaNegocioException.class,
                () -> citaService.createCita(request)
        );

        assertTrue(exception.getMessage().contains("El veterinario no está disponible en el horario seleccionado"));
        verify(citaMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla 2: Rechazar cita si el cliente ya tiene 2 citas PENDIENTES ese mismo día")
    void testCreateCitaMaxPendingAppointmentsReached() {
        CreateCitaRequest request = CreateCitaRequest.builder()
                .mascotaId(10L)
                .veterinarioId(2L)
                .fechaHora(futureDate)
                .motivo("Control")
                .build();

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(securityUtils.isCliente()).thenReturn(true);
        when(citaMedicaRepository.existsOverlappingCita(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        // Simular que el cliente ya tiene 2 citas pendientes ese día
        when(citaMedicaRepository.countCitasPendientesClienteEnDia(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);

        ReglaNegocioException exception = assertThrows(
                ReglaNegocioException.class,
                () -> citaService.createCita(request)
        );

        assertTrue(exception.getMessage().contains("límite máximo de 2 citas en estado PENDIENTE"));
        verify(citaMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazar creación de cita si el cliente intenta agendar para mascota ajena")
    void testCreateCitaUnauthorizedMascota() {
        CreateCitaRequest request = CreateCitaRequest.builder()
                .mascotaId(10L)
                .veterinarioId(2L)
                .fechaHora(futureDate)
                .motivo("Control")
                .build();

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        // Usuario autenticado es id=99, pero la mascota pertenece a id=1
        when(securityUtils.getCurrentUserId()).thenReturn(99L);
        when(securityUtils.isCliente()).thenReturn(true);

        assertThrows(ForbiddenOperationException.class, () -> citaService.createCita(request));
        verify(citaMedicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Regla 3: Permitir cancelación cuando faltan más de 2 horas para la cita")
    void testCancelarCitaMoreThanTwoHoursSuccess() {
        // Cita programada dentro de 4 horas
        LocalDateTime citaHora = LocalDateTime.now().plusHours(4);
        CitaMedica cita = CitaMedica.builder()
                .id(50L)
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(citaHora)
                .motivo("Chequeo")
                .estado(EstadoCita.PENDIENTE)
                .build();

        when(citaMedicaRepository.findById(50L)).thenReturn(Optional.of(cita));
        when(securityUtils.isCliente()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(citaMedicaRepository.save(any(CitaMedica.class))).thenAnswer(i -> i.getArgument(0));

        CitaResponse response = citaService.cancelarCita(50L);

        assertNotNull(response);
        assertEquals(EstadoCita.CANCELADA, response.getEstado());
        verify(citaMedicaRepository).save(argThat(c -> c.getEstado() == EstadoCita.CANCELADA));
    }

    @Test
    @DisplayName("Regla 3: Rechazar cancelación cuando faltan 2 horas o menos")
    void testCancelarCitaLessThanTwoHoursRejected() {
        // Cita programada dentro de 1 hora y 30 minutos
        LocalDateTime citaHora = LocalDateTime.now().plusMinutes(90);
        CitaMedica cita = CitaMedica.builder()
                .id(50L)
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(citaHora)
                .motivo("Chequeo")
                .estado(EstadoCita.PENDIENTE)
                .build();

        when(citaMedicaRepository.findById(50L)).thenReturn(Optional.of(cita));
        when(securityUtils.isCliente()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        ReglaNegocioException exception = assertThrows(
                ReglaNegocioException.class,
                () -> citaService.cancelarCita(50L)
        );

        assertTrue(exception.getMessage().contains("más de 2 horas"));
        assertEquals(EstadoCita.PENDIENTE, cita.getEstado());
    }

    @Test
    @DisplayName("CLIENTE no puede cancelar citas de otros clientes")
    void testCancelarCitaOtherClientForbidden() {
        CitaMedica cita = CitaMedica.builder()
                .id(50L)
                .mascota(mascota) // Dueño es cliente id=1
                .veterinario(veterinario)
                .fechaHora(LocalDateTime.now().plusHours(5))
                .estado(EstadoCita.PENDIENTE)
                .build();

        when(citaMedicaRepository.findById(50L)).thenReturn(Optional.of(cita));
        when(securityUtils.isCliente()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(88L); // Usuario autenticado diferente

        assertThrows(ForbiddenOperationException.class, () -> citaService.cancelarCita(50L));
    }

    @Test
    @DisplayName("VET no puede consultar la agenda de otro veterinario")
    void testGetAgendaOtherVetForbidden() {
        when(securityUtils.isVet()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(2L); // Vet actual id=2

        // Intenta consultar la agenda del vet id=5
        assertThrows(ForbiddenOperationException.class, () -> citaService.getAgenda(LocalDate.now(), 5L));
    }

    @Test
    @DisplayName("ADMIN puede consultar la agenda de cualquier veterinario")
    void testGetAgendaAdminSuccess() {
        when(securityUtils.isVet()).thenReturn(false);

        CitaMedica cita = CitaMedica.builder()
                .id(1L)
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(futureDate)
                .motivo("Consulta")
                .estado(EstadoCita.PENDIENTE)
                .build();

        when(citaMedicaRepository.findAgenda(eq(2L), any(), any())).thenReturn(List.of(cita));

        List<CitaResponse> agenda = citaService.getAgenda(futureDate.toLocalDate(), 2L);

        assertEquals(1, agenda.size());
        assertEquals(1L, agenda.get(0).getId());
    }
}
