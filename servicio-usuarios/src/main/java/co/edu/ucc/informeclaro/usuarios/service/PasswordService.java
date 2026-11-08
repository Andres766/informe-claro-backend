package co.edu.ucc.informeclaro.usuarios.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Hash de contraseñas con bcrypt. Compatible con los hashes ya guardados por la versión anterior
 * en Python ($2b$): las cuentas existentes siguen entrando.
 */
@Service
public class PasswordService {

    private static final int LIMITE_BYTES_BCRYPT = 72;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

    public String hashear(String password) {
        return bcrypt.encode(recortar(password));
    }

    public boolean coincide(String password, String hash) {
        return hash != null && !hash.isBlank() && bcrypt.matches(recortar(password), hash);
    }

    /** bcrypt solo usa los primeros 72 bytes; se recorta sin partir caracteres. */
    static String recortar(String password) {
        StringBuilder resultado = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < password.length(); ) {
            int cp = password.codePointAt(i);
            String caracter = new String(Character.toChars(cp));
            int tamano = caracter.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + tamano > LIMITE_BYTES_BCRYPT) {
                break;
            }
            resultado.append(caracter);
            bytes += tamano;
            i += Character.charCount(cp);
        }
        return resultado.toString();
    }
}
