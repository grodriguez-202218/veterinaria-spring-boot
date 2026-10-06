package com.example.veterinaria.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Clave secreta para firmar los tokens JWT (mínimo 256 bits en Base64 o texto).
     */
    private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /**
     * Tiempo de expiración del token en milisegundos (por defecto 24 horas = 86,400,000 ms).
     */
    private long expiration = 86400000L;
}
