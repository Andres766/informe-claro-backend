package co.edu.ucc.informeclaro.infraestructura.error;

import org.springframework.http.HttpStatus;

/** Error de la API REST con su clave traducible. */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;
    private final String clave;
    private final transient Object[] datos;

    public ApiException(HttpStatus estado, String clave, Object... datos) {
        super(clave);
        this.estado = estado;
        this.clave = clave;
        this.datos = datos;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String clave() {
        return clave;
    }

    public Object[] datos() {
        return datos;
    }
}
