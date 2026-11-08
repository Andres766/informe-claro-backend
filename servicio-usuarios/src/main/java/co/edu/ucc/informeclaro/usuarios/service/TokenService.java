package co.edu.ucc.informeclaro.usuarios.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/** Tokens de sesión JWT (HS256), compatibles con los emitidos por la versión anterior. */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final SecretKey clave;
    private final Duration vigencia;

    public TokenService(@Value("${informeclaro.jwt.secreto:}") String secreto,
                        @Value("${informeclaro.jwt.horas:8}") long horas) {
        String efectivo = secreto;
        if (efectivo == null || efectivo.isBlank()) {
            log.warn("JWT_SECRET no definido: se usa una clave temporal (las sesiones se pierden al reiniciar)");
            byte[] aleatorio = new byte[48];
            new SecureRandom().nextBytes(aleatorio);
            efectivo = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        }
        this.clave = Keys.hmacShaKeyFor(efectivo.getBytes(StandardCharsets.UTF_8)); // exige 32 bytes o más
        this.vigencia = Duration.ofHours(horas);
    }

    public String crear(long usuarioId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(usuarioId))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    /** Id del usuario del token, o vacío si es inválido o expiró. */
    public Optional<Long> leer(String token) {
        try {
            String sujeto = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(Long.parseLong(sujeto));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
