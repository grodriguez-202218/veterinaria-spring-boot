package com.example.veterinaria.service;

import com.example.veterinaria.dto.mascota.CreateMascotaRequest;
import com.example.veterinaria.dto.mascota.MascotaResponse;
import com.example.veterinaria.entity.Mascota;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Especie;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.ReglaNegocioException;
import com.example.veterinaria.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private MascotaService mascotaService;

    private Usuario cliente;
    private Mascota mascota;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder()
                .id(1L)
                .nombre("Carlos Cliente")
                .email("carlos@example.com")
                .rol(Rol.CLIENTE)
                .build();

        mascota = Mascota.builder()
                .id(10L)
                .nombre("Firulais")
                .especie(Especie.PERRO)
                .raza("Labrador")
                .edad(3)
                .cliente(cliente)
                .build();
    }

    @Test
    @DisplayName("CLIENTE registra su mascota y se asigna automáticamente a su cuenta")
    void testCreateMascotaByCliente() {
        CreateMascotaRequest request = CreateMascotaRequest.builder()
                .nombre("Firulais")
                .especie(Especie.PERRO)
                .raza("Labrador")
                .edad(3)
                .clienteId(999L) // Debe ser ignorado porque es CLIENTE
                .build();

        when(securityUtils.isCliente()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.save(any(Mascota.class))).thenReturn(mascota);

        MascotaResponse response = mascotaService.createMascota(request);

        assertNotNull(response);
        assertEquals("Firulais", response.getNombre());
        assertEquals(1L, response.getClienteId());
        verify(mascotaRepository).save(argThat(m -> m.getCliente().getId().equals(1L)));
    }

    @Test
    @DisplayName("ADMIN registra mascota especificando el clienteId")
    void testCreateMascotaByAdmin() {
        CreateMascotaRequest request = CreateMascotaRequest.builder()
                .nombre("Michi")
                .especie(Especie.GATO)
                .raza("Siamés")
                .edad(2)
                .clienteId(1L)
                .build();

        when(securityUtils.isCliente()).thenReturn(false);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));

        Mascota gato = Mascota.builder()
                .id(11L)
                .nombre("Michi")
                .especie(Especie.GATO)
                .raza("Siamés")
                .edad(2)
                .cliente(cliente)
                .build();

        when(mascotaRepository.save(any(Mascota.class))).thenReturn(gato);

        MascotaResponse response = mascotaService.createMascota(request);

        assertNotNull(response);
        assertEquals("Michi", response.getNombre());
        assertEquals(1L, response.getClienteId());
    }

    @Test
    @DisplayName("ADMIN no puede registrar mascota sin especificar clienteId")
    void testCreateMascotaByAdminMissingClienteId() {
        CreateMascotaRequest request = CreateMascotaRequest.builder()
                .nombre("Michi")
                .especie(Especie.GATO)
                .edad(2)
                .clienteId(null)
                .build();

        when(securityUtils.isCliente()).thenReturn(false);
        when(securityUtils.isAdmin()).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> mascotaService.createMascota(request));
    }

    @Test
    @DisplayName("CLIENTE obtiene únicamente sus propias mascotas")
    void testGetMisMascotas() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(mascotaRepository.findByClienteId(1L)).thenReturn(List.of(mascota));

        List<MascotaResponse> responses = mascotaService.getMisMascotas();

        assertEquals(1, responses.size());
        assertEquals("Firulais", responses.get(0).getNombre());
    }

    @Test
    @DisplayName("Consultar mascota por ID inexistente lanza ResourceNotFoundException")
    void testGetMascotaNotFound() {
        when(mascotaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> mascotaService.getMascotaById(999L));
    }
}
