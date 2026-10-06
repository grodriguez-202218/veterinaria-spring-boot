package com.example.veterinaria.service;

import com.example.veterinaria.dto.expediente.CreateExpedienteRequest;
import com.example.veterinaria.dto.expediente.ExpedienteResponse;
import com.example.veterinaria.entity.CitaMedica;
import com.example.veterinaria.entity.ExpedienteClinico;
import com.example.veterinaria.entity.Mascota;
import com.example.veterinaria.enums.EstadoCita;
import com.example.veterinaria.exception.DuplicateResourceException;
import com.example.veterinaria.exception.ForbiddenOperationException;
import com.example.veterinaria.exception.ReglaNegocioException;
import com.example.veterinaria.exception.ResourceNotFoundException;
import com.example.veterinaria.repository.CitaMedicaRepository;
import com.example.veterinaria.repository.ExpedienteClinicoRepository;
import com.example.veterinaria.repository.MascotaRepository;
import com.example.veterinaria.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private final ExpedienteClinicoRepository expedienteClinicoRepository;
    private final CitaMedicaRepository citaMedicaRepository;
    private final MascotaRepository mascotaRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public ExpedienteResponse createExpediente(CreateExpedienteRequest request) {
        log.info("Iniciando registro de expediente clínico para citaId={}", request.getCitaId());

        // 1. Validar que la cita exista
        CitaMedica cita = citaMedicaRepository.findById(request.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita médica no encontrada con id: " + request.getCitaId()));

        // 2. Validar que la cita no esté cancelada
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("No se puede registrar un expediente para una cita cancelada");
        }

        // 3. Regla 4 — Validar que todavía no tenga expediente (1:1 estricto)
        if (expedienteClinicoRepository.existsByCitaId(cita.getId())) {
            throw new DuplicateResourceException("La cita ya cuenta con un expediente clínico registrado previamente");
        }

        // 4. Registrar el expediente clínico
        ExpedienteClinico expediente = ExpedienteClinico.builder()
                .cita(cita)
                .diagnostico(request.getDiagnostico().trim())
                .tratamiento(request.getTratamiento().trim())
                .pesoKg(request.getPesoKg())
                .fechaRegistro(LocalDateTime.now())
                .build();

        ExpedienteClinico guardado = expedienteClinicoRepository.save(expediente);

        // 5. Cambiar el estado de la cita a COMPLETADA (atómico mediante @Transactional)
        cita.setEstado(EstadoCita.COMPLETADA);
        citaMedicaRepository.save(cita);

        log.info("Expediente id={} registrado exitosamente y cita id={} actualizada a COMPLETADA",
                guardado.getId(), cita.getId());

        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<ExpedienteResponse> getHistorialByMascotaId(Long mascotaId) {
        log.info("Consultando historial clínico de la mascota id={}", mascotaId);

        // 1. Validar que la mascota exista
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con id: " + mascotaId));

        // 2. Si el usuario es CLIENTE, validar propiedad
        if (securityUtils.isCliente()) {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (!mascota.getCliente().getId().equals(currentUserId)) {
                throw new ForbiddenOperationException("No tiene permisos para consultar el historial de mascotas de otros clientes");
            }
        }

        // 3. Obtener el historial clínico ordenado
        return expedienteClinicoRepository.findByMascotaIdOrderByFechaRegistroDesc(mascotaId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ExpedienteResponse mapToResponse(ExpedienteClinico expediente) {
        CitaMedica cita = expediente.getCita();
        return ExpedienteResponse.builder()
                .id(expediente.getId())
                .citaId(cita.getId())
                .mascotaId(cita.getMascota().getId())
                .mascotaNombre(cita.getMascota().getNombre())
                .clienteId(cita.getMascota().getCliente().getId())
                .clienteNombre(cita.getMascota().getCliente().getNombre())
                .veterinarioId(cita.getVeterinario().getId())
                .veterinarioNombre(cita.getVeterinario().getNombre())
                .diagnostico(expediente.getDiagnostico())
                .tratamiento(expediente.getTratamiento())
                .pesoKg(expediente.getPesoKg())
                .fechaRegistro(expediente.getFechaRegistro())
                .build();
    }
}
