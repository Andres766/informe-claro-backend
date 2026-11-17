package co.edu.ucc.informeclaro.usuarios.service;

import co.edu.ucc.informeclaro.comun.error.ReglaNegocioException;
import co.edu.ucc.informeclaro.usuarios.model.Usuario;
import co.edu.ucc.informeclaro.usuarios.repository.UsuarioRepository;
import io.grpc.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de las cuentas. Los correos de ADMIN_EMAILS reciben el rol de administrador al
 * registrarse o al iniciar sesión; el rol queda guardado en la base de datos.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordService passwords;
    private final TokenService tokens;
    private final Set<String> correosAdmin;

    public UsuarioService(UsuarioRepository repositorio, PasswordService passwords, TokenService tokens,
                          @Value("${informeclaro.admins:}") String admins) {
        this.repositorio = repositorio;
        this.passwords = passwords;
        this.tokens = tokens;
        this.correosAdmin = Arrays.stream(admins.split(","))
                .map(String::strip).filter(c -> !c.isEmpty()).map(UsuarioService::normalizarCorreo)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public Usuario registrar(String nombre, String email, String password) {
        String correo = normalizarCorreo(email);
        if (repositorio.existsByEmail(correo)) {
            throw new ReglaNegocioException(Status.Code.ALREADY_EXISTS, "correo_repetido");
        }
        Usuario usuario = new Usuario(nombre.strip(), correo, passwords.hashear(password));
        if (correosAdmin.contains(correo)) {
            usuario.hacerAdmin();
        }
        return repositorio.save(usuario);
    }

    @Transactional
    public Usuario autenticar(String email, String password) {
        // Mismo error en ambos casos para no revelar qué correos están registrados.
        Usuario usuario = repositorio.findByEmail(normalizarCorreo(email))
                .filter(u -> passwords.coincide(password, u.getPasswordHash()))
                .orElseThrow(() -> new ReglaNegocioException(Status.Code.UNAUTHENTICATED, "credenciales"));
        if (!usuario.esAdmin() && correosAdmin.contains(usuario.getEmail())) {
            usuario.hacerAdmin(); // cuenta existente que se agregó a ADMIN_EMAILS
        }
        return usuario;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repositorio.findAll(Sort.by("id"));
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
