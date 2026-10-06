package com.example.veterinaria.service;

import com.example.veterinaria.dto.auth.AuthResponse;
import com.example.veterinaria.dto.auth.LoginRequest;
import com.example.veterinaria.dto.auth.RegisterRequest;
import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.DuplicateResourceException;
import com.example.veterinaria.exception.ResourceNotFoundException;
import com.example.veterinaria.repository.UsuarioRepository;
import com.example.veterinaria.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (usuarioRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Ya existe un usuario registrado con el correo: " + email);
        }

        // El rol se establece obligatoriamente como CLIENTE sin permitir roles privilegiados
        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .email(email)
                .telefono(request.getTelefono() != null ? request.getTelefono().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Nuevo cliente registrado con éxito: id={}, email={}", guardado.getId(), guardado.getEmail());

        String token = jwtTokenProvider.generateToken(
                guardado.getId(),
                guardado.getEmail(),
                guardado.getRol().name()
        );

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(guardado.getId())
                .nombre(guardado.getNombre())
                .email(guardado.getEmail())
                .rol(guardado.getRol().name())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Autenticación con Spring Security (valida credenciales con BCrypt)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con correo: " + email));

        String token = jwtTokenProvider.generateToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol().name()
        );

        log.info("Inicio de sesión exitoso para: id={}, email={}, rol={}", usuario.getId(), usuario.getEmail(), usuario.getRol());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build();
    }
}
