package com.example.veterinaria.service;

import com.example.veterinaria.dto.auth.AuthResponse;
import com.example.veterinaria.dto.auth.LoginRequest;
import com.example.veterinaria.dto.auth.RegisterRequest;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.DuplicateResourceException;
import com.example.veterinaria.repository.UsuarioRepository;
import com.example.veterinaria.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private Usuario usuarioCliente;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .nombre("Juan Pérez")
                .email("juan@example.com")
                .password("password123")
                .telefono("555-1234")
                .build();

        usuarioCliente = Usuario.builder()
                .id(1L)
                .nombre("Juan Pérez")
                .email("juan@example.com")
                .password("encoded_pass")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario forzando el rol a CLIENTE")
    void testRegisterSuccess() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioCliente);
        when(jwtTokenProvider.generateToken(1L, "juan@example.com", "CLIENTE")).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals("CLIENTE", response.getRol());
        assertEquals("juan@example.com", response.getEmail());

        verify(usuarioRepository).save(argThat(u -> u.getRol() == Rol.CLIENTE));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el correo ya existe al registrar")
    void testRegisterDuplicateEmail() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe iniciar sesión correctamente y retornar token JWT")
    void testLoginSuccess() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("juan@example.com")
                .password("password123")
                .build();

        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioCliente));
        when(jwtTokenProvider.generateToken(1L, "juan@example.com", "CLIENTE")).thenReturn("jwt-login-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-login-token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Debe fallar login con credenciales erróneas")
    void testLoginBadCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("juan@example.com")
                .password("wrong-pass")
                .build();

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
    }
}
