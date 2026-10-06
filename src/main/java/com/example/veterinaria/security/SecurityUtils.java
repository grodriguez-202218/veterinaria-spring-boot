package com.example.veterinaria.security;

import com.example.veterinaria.enums.Rol;
import com.example.veterinaria.exception.ForbiddenOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenOperationException("No se encontró usuario autenticado en la sesión");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails;
        }

        throw new ForbiddenOperationException("El formato del usuario autenticado es inválido");
    }

    public Long getCurrentUserId() {
        return getCurrentUserDetails().getId();
    }

    public String getCurrentUserEmail() {
        return getCurrentUserDetails().getEmail();
    }

    public Rol getCurrentUserRol() {
        return getCurrentUserDetails().getRol();
    }

    public boolean hasRole(Rol rol) {
        return getCurrentUserRol() == rol;
    }

    public boolean isAdmin() {
        return hasRole(Rol.ADMIN);
    }

    public boolean isVet() {
        return hasRole(Rol.VET);
    }

    public boolean isCliente() {
        return hasRole(Rol.CLIENTE);
    }
}
