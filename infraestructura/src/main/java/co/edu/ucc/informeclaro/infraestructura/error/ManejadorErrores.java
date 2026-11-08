package co.edu.ucc.informeclaro.infraestructura.error;

import co.edu.ucc.informeclaro.infraestructura.i18n.Idiomas;
import co.edu.ucc.informeclaro.infraestructura.i18n.Mensajes;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Convierte cualquier error en la misma respuesta JSON de siempre: {@code {"detail": "..."}}, o la lista
 * de errores de validación por campo que el frontend ya sabe leer.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
    private static final List<String> ORDEN_CAMPOS = List.of("nombre", "email", "password");

    private final String idiomaPorDefecto;
    private final int maxPdfMb;

    public ManejadorErrores(@Value("${informeclaro.idioma-por-defecto}") String idiomaPorDefecto,
                            @Value("${informeclaro.max-pdf-mb}") int maxPdfMb) {
        this.idiomaPorDefecto = idiomaPorDefecto;
        this.maxPdfMb = maxPdfMb;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e, HttpServletRequest peticion) {
        return detalle(e.estado(), Mensajes.t(e.clave(), idioma(peticion), e.datos()));
    }

    /** Errores que llegan de los microservicios por gRPC: el código define el HTTP y la descripción es la clave. */
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, Object>> grpc(StatusRuntimeException e, HttpServletRequest peticion) {
        String clave = e.getStatus().getDescription();
        HttpStatus estado = switch (e.getStatus().getCode()) {
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT, FAILED_PRECONDITION -> HttpStatus.UNPROCESSABLE_CONTENT;
            case UNAVAILABLE, DEADLINE_EXCEEDED -> {
                clave = "servicio_no_disponible";
                yield HttpStatus.SERVICE_UNAVAILABLE;
            }
            default -> {
                log.error("Error interno en un microservicio", e);
                clave = "error_interno";
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
        };
        return detalle(estado, Mensajes.t(clave == null ? "error_interno" : clave, idioma(peticion)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        List<Map<String, Object>> errores = e.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparingInt(f -> {
                    int i = ORDEN_CAMPOS.indexOf(f.getField());
                    return i < 0 ? ORDEN_CAMPOS.size() : i;
                }))
                .map(f -> Map.<String, Object>of("type", "value_error", "loc", List.of("body", f.getField()),
                        "msg", f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("detail", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoInvalido() {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("detail",
                List.of(Map.of("type", "json_invalid", "loc", List.of("body"), "msg", "JSON decode error"))));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> faltaArchivo(HttpServletRequest peticion) {
        return detalle(HttpStatus.UNPROCESSABLE_CONTENT, Mensajes.t("archivo_requerido", idioma(peticion)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> muyGrande(HttpServletRequest peticion) {
        return detalle(HttpStatus.CONTENT_TOO_LARGE, Mensajes.t("pdf_grande", idioma(peticion), maxPdfMb));
    }

    private String idioma(HttpServletRequest peticion) {
        return Idiomas.normalizar(peticion.getHeader(HttpHeaders.ACCEPT_LANGUAGE), idiomaPorDefecto);
    }

    private static ResponseEntity<Map<String, Object>> detalle(HttpStatus estado, String mensaje) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(estado);
        if (estado == HttpStatus.UNAUTHORIZED) {
            respuesta.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return respuesta.body(Map.of("detail", mensaje));
    }
}
