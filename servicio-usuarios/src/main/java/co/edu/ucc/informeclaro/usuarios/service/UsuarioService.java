package co.edu.ucc.informeclaro.usuarios.service;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import co.edu.ucc.informeclaro.usuarios.model.Usuario;
import co.edu.ucc.informeclaro.usuarios.repository.UsuarioRepository;
import io.grpc.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Reglas de negocio de las cuentas. */
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordService passwords;
    private final TokenService tokens;

    public UsuarioService(UsuarioRepository repositorio, PasswordService passwords, TokenService tokens) {
        this.repositorio = repositorio;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional
    public Usuario registrar(String nombre, String email, String password) {
        String correo = normalizarCorreo(email);
        if (repositorio.existsByEmail(correo)) {
            throw new ReglaNegocioException(Status.Code.ALREADY_EXISTS, "correo_repetido");
        }
        return repositorio.save(new Usuario(nombre.strip(), correo, passwords.hashear(password)));
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String email, String password) {
        // Mismo error en ambos casos para no revelar qué correos están registrados.
        return repositorio.findByEmail(normalizarCorreo(email))
                .filter(u -> passwords.coincide(password, u.getPasswordHash()))
                .orElseThrow(() -> new ReglaNegocioException(Status.Code.UNAUTHENTICATED, "credenciales"));
    }

    @Transactional(readOnly = true)
    public Usuario desdeToken(String token) {
        return tokens.leer(token)
                .flatMap(repositorio::findById)
                .orElseThrow(() -> new ReglaNegocioException(Status.Code.UNAUTHENTICATED, "inicia_sesion"));
    }

    public String emitirToken(Usuario usuario) {
        return tokens.crear(usuario.getId());
    }

    private static String normalizarCorreo(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
