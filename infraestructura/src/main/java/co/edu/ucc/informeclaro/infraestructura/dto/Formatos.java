package co.edu.ucc.informeclaro.infraestructura.dto;

/** Formatos de validación compartidos por los DTO de la API REST. */
final class Formatos {

    /** Correo con usuario, @ y un dominio con al menos un punto. */
    static final String CORREO = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private Formatos() {
    }
}
