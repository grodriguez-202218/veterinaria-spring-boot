package com.example.veterinaria.service;

import com.example.veterinaria.dto.cita.CitaResponse;
import com.example.veterinaria.dto.cita.CreateCitaRequest;
import com.example.veterinaria.entity.CitaMedica;
import com.example.veterinaria.entity.Mascota;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.EstadoCita;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.ForbiddenOperationException;
import com.example.veterinaria.exception.ReglaNegocioException;
import com.example.veterinaria.exception.ResourceNotFoundException;
import com.example.veterinaria.repository.CitaMedicaRepository;
import com.example.veterinaria.repository.MascotaRepository;
import com.example.veterinaria.repository.UsuarioRepository;
import com.example.veterinaria.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitaService {

    private final CitaMedicaRepository citaMedicaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public CitaResponse createCita(CreateCitaRequest request) {
        log.info("Iniciando creación de cita para mascotaId={}, vetId={}, fechaHora={}",
                request.getMascotaId(), request.getVeterinarioId(), request.getFechaHora());

        // 1. Validar que la mascota exista
        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con id: " + request.getMascotaId()));

        // 2. Validar que el veterinario exista y tenga rol VET
        Usuario veterinario = usuarioRepository.findById(request.getVeterinarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado con id: " + request.getVeterinarioId()));

        if (veterinario.getRol() != Rol.VET) {
            throw new ReglaNegocioException("El usuario seleccionado no tiene el rol de veterinario (VET)");
        }

        // 3. Validar propiedad de la mascota cuando quien solicita es CLIENTE
        Long currentUserId = securityUtils.getCurrentUserId();
        if (securityUtils.isCliente()) {
            if (!mascota.getCliente().getId().equals(currentUserId)) {
                throw new ForbiddenOperationException("No tiene permisos para agendar citas para una mascota que no le pertenece");
            }
        }

        // 4. Validar que la fecha y hora sea en el futuro
        LocalDateTime fechaHora = request.getFechaHora();
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha y hora de la cita debe ser futura");
        }

        // 5. Regla 1 — Disponibilidad del veterinario (Duración exacta de 30 minutos sin solapamiento)
        LocalDateTime fechaFin = fechaHora.plusMinutes(30);
        LocalDateTime fechaInicioMenos30 = fechaHora.minusMinutes(30);

        boolean haySolapamiento = citaMedicaRepository.existsOverlappingCita(
                veterinario.getId(),
                fechaFin,
                fechaInicioMenos30
        );

        if (haySolapamiento) {
            throw new ReglaNegocioException("El veterinario no está disponible en el horario seleccionado");
        }

        // 6. Regla 2 — Máximo de 2 citas PENDIENTES por cliente durante el mismo día
        Long clienteId = mascota.getCliente().getId();
        LocalDate diaCita = fechaHora.toLocalDate();
        LocalDateTime inicioDia = diaCita.atStartOfDay();
        LocalDateTime finDia = diaCita.atTime(LocalTime.MAX);

        long citasPendientesHoy = citaMedicaRepository.countCitasPendientesClienteEnDia(
                clienteId,
                inicioDia,
                finDia
        );

        if (citasPendientesHoy >= 2) {
            throw new ReglaNegocioException(
                    "El cliente ya posee el límite máximo de 2 citas en estado PENDIENTE para el día " + diaCita
            );
        }

        // 7. Crear la cita con estado PENDIENTE
        CitaMedica cita = CitaMedica.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(fechaHora)
                .motivo(request.getMotivo().trim())
                .estado(EstadoCita.PENDIENTE)
                .build();

        CitaMedica guardada = citaMedicaRepository.save(cita);
        log.info("Cita médica creada exitosamente con id={}", guardada.getId());

        return mapToResponse(guardada);
    }

    @Transactional
    public CitaResponse cancelarCita(Long id) {
        log.info("Solicitud de cancelación para cita id={}", id);

        // 1. Verificar que la cita exista
        CitaMedica cita = citaMedicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita médica no encontrada con id: " + id));

        // 2. Verificar permisos: si es CLIENTE, debe ser dueño de la mascota
        if (securityUtils.isCliente()) {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (!cita.getMascota().getCliente().getId().equals(currentUserId)) {
                throw new ForbiddenOperationException("No tiene permisos para cancelar citas de otros clientes");
            }
        }

        // 3. Verificar que la cita pueda ser cancelada
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("La cita ya se encuentra cancelada");
        }
        if (cita.getEstado() == EstadoCita.COMPLETADA) {
            throw new ReglaNegocioException("No se puede cancelar una cita que ya fue completada");
        }

        // 4. Regla 3 — Cancelación con anticipación (> 2 horas antes de la hora programada)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limiteCancelacion = cita.getFechaHora().minusHours(2);

        if (!now.isBefore(limiteCancelacion)) {
            throw new ReglaNegocioException(
                    "Una cita únicamente puede cancelarse cuando faltan más de 2 horas para su hora programada"
            );
        }

        // 5. Cambiar el estado a CANCELADA
        cita.setEstado(EstadoCita.CANCELADA);
        CitaMedica cancelada = citaMedicaRepository.save(cita);
        log.info("Cita médica id={} cancelada exitosamente", id);

        return mapToResponse(cancelada);
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> getAgenda(LocalDate fecha, Long veterinarioId) {
        Long targetVetId = veterinarioId;

        // Si quien consulta es VET:
        if (securityUtils.isVet()) {
            Long currentVetId = securityUtils.getCurrentUserId();
            if (veterinarioId != null && !veterinarioId.equals(currentVetId)) {
                throw new ForbiddenOperationException("Los veterinarios únicamente pueden consultar su propia agenda");
            }
            targetVetId = currentVetId;
        }

        LocalDateTime startOfDay = null;
        LocalDateTime endOfDay = null;

        if (fecha != null) {
            startOfDay = fecha.atStartOfDay();
            endOfDay = fecha.atTime(LocalTime.MAX);
        }

        log.info("Consultando agenda: veterinarioId={}, fecha={}", targetVetId, fecha);

        return citaMedicaRepository.findAgenda(targetVetId, startOfDay, endOfDay)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CitaResponse mapToResponse(CitaMedica cita) {
        return CitaResponse.builder()
                .id(cita.getId())
                .mascotaId(cita.getMascota().getId())
                .mascotaNombre(cita.getMascota().getNombre())
                .especie(cita.getMascota().getEspecie())
                .clienteId(cita.getMascota().getCliente().getId())
                .clienteNombre(cita.getMascota().getCliente().getNombre())
                .veterinarioId(cita.getVeterinario().getId())
                .veterinarioNombre(cita.getVeterinario().getNombre())
                .fechaHora(cita.getFechaHora())
                .fechaHoraFin(cita.getFechaHora().plusMinutes(30))
                .motivo(cita.getMotivo())
                .estado(cita.getEstado())
                .build();
    }
}
