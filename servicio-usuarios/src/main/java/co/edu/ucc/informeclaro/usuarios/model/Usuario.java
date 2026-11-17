package co.edu.ucc.informeclaro.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cuenta de un paciente (rol "usuario") o de un administrador (rol "admin"). Nunca se guarda la
 * contraseña: solo su hash bcrypt.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    public static final String ROL_USUARIO = "usuario";
    public static final String ROL_ADMIN = "admin";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    // Nulo en las cuentas creadas antes de existir los roles: se leen como "usuario".
    @Column(length = 20)
    private String rol;

    protected Usuario() {
    }

    public Usuario(String nombre, String email, String passwordHash) {
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = ROL_USUARIO;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRol() {
        return rol == null ? ROL_USUARIO : rol;
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    public void hacerAdmin() {
        this.rol = ROL_ADMIN;
    }
}
