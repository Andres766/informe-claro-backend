package co.edu.ucc.informeclaro.interpretacion.externo;

/** El proveedor de IA falló, se negó o respondió en otro formato: se usa el motor por reglas. */
public class LlmNoDisponibleException extends RuntimeException {

    public LlmNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
