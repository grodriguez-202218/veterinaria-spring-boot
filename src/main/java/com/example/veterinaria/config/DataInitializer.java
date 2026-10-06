package com.example.veterinaria.config;

import com.example.veterinaria.entity.Usuario;
import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Verificando existencia de usuarios base del sistema...");

        seedUser("admin@veterinaria.com", "Administrador Principal", "555-100-0001", "admin123", Rol.ADMIN);
        seedUser("vet@veterinaria.com", "Dr. Roberto Gómez (Veterinario)", "555-200-0002", "vet123", Rol.VET);
        seedUser("cliente@veterinaria.com", "Carlos Cliente Demo", "555-300-0003", "cliente123", Rol.CLIENTE);
    }

    private void seedUser(String email, String nombre, String telefono, String rawPassword, Rol rol) {
        if (!usuarioRepository.existsByEmail(email)) {
            Usuario usuario = Usuario.builder()
                    .nombre(nombre)
                    .email(email)
                    .telefono(telefono)
                    .password(passwordEncoder.encode(rawPassword))
                    .rol(rol)
                    .build();
            usuarioRepository.save(usuario);
            log.info("Usuario inicial creado: email={}, rol={}", email, rol);
        }
    }
}
