package com.example.veterinaria.service;

import com.example.veterinaria.dto.mascota.CreateMascotaRequest;
import com.example.veterinaria.dto.mascota.MascotaResponse;
import com.example.veterinaria.entity.Mascota;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.ForbiddenOperationException;
import com.example.veterinaria.exception.ReglaNegocioException;
import com.example.veterinaria.exception.ResourceNotFoundException;
import com.example.veterinaria.repository.MascotaRepository;
import com.example.veterinaria.repository.UsuarioRepository;
import com.example.veterinaria.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<MascotaResponse> getMisMascotas() {
        Long clienteId = securityUtils.getCurrentUserId();
        log.info("Consultando mascotas del cliente autenticado con id={}", clienteId);

        return mascotaRepository.findByClienteId(clienteId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MascotaResponse createMascota(CreateMascotaRequest request) {
        Usuario cliente;

        if (securityUtils.isCliente()) {
            // Regla de seguridad: Si quien registra es CLIENTE, la mascota se asocia automáticamente a su cuenta
            Long currentUserId = securityUtils.getCurrentUserId();
            cliente = usuarioRepository.findById(currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente autenticado no encontrado"));
        } else if (securityUtils.isAdmin()) {
            // Si quien registra es ADMIN, el clienteId es obligatorio en la petición
            if (request.getClienteId() == null) {
                throw new ReglaNegocioException("Como administrador debe especificar el ID del cliente propietario de la mascota");
            }
            cliente = usuarioRepository.findById(request.getClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("Propietario no encontrado con id: " + request.getClienteId()));

            if (cliente.getRol() != Rol.CLIENTE) {
                throw new ReglaNegocioException("El usuario asignado como propietario debe tener el rol CLIENTE");
            }
        } else {
            throw new ForbiddenOperationException("No tiene permisos para registrar mascotas");
        }

        Mascota mascota = Mascota.builder()
                .nombre(request.getNombre().trim())
                .especie(request.getEspecie())
                .raza(request.getRaza() != null ? request.getRaza().trim() : null)
                .edad(request.getEdad())
                .cliente(cliente)
                .build();

        Mascota guardada = mascotaRepository.save(mascota);
        log.info("Mascota registrada exitosamente: id={}, nombre={}, clienteId={}",
                guardada.getId(), guardada.getNombre(), cliente.getId());

        return mapToResponse(guardada);
    }

    @Transactional(readOnly = true)
    public MascotaResponse getMascotaById(Long id) {
        log.info("Consultando información de mascota id={}", id);
        Mascota mascota = mascotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mascota no encontrada con id: " + id));

        return mapToResponse(mascota);
    }

    public MascotaResponse mapToResponse(Mascota mascota) {
        return MascotaResponse.builder()
                .id(mascota.getId())
                .nombre(mascota.getNombre())
                .especie(mascota.getEspecie())
                .raza(mascota.getRaza())
                .edad(mascota.getEdad())
                .clienteId(mascota.getCliente().getId())
                .clienteNombre(mascota.getCliente().getNombre())
                .clienteEmail(mascota.getCliente().getEmail())
                .build();
    }
}
