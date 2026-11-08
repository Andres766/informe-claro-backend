package co.edu.ucc.informeclaro.interpretacion.service;

import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.externo.AnthropicRestCliente;
import co.edu.ucc.informeclaro.interpretacion.model.RegistroInterpretacion;
import co.edu.ucc.informeclaro.interpretacion.repository.RegistroInterpretacionRepository;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Caso de uso: elegir el motor (Claude si hay API key; si no, reglas), caer al motor por reglas
 * si Claude falla, y dejar constancia en la bitácora.
 */
@Service
public class InterpretacionService {

    private static final Logger log = LoggerFactory.getLogger(InterpretacionService.class);
    static final String MOTOR_DEMO = "demo (reglas)";
    static final String MOTOR_RESPALDO = "respaldo (reglas)";

    private static final Map<String, String> AVISOS = Map.of(
            "es", "Esta explicación es solo informativa y no es un diagnóstico. "
                    + "Consulta siempre a tu médico tratante para interpretar tus resultados.",
            "en", "This explanation is for information only and is not a diagnosis. "
                    + "Always ask your treating doctor to interpret your results.");

    private final MotorClaudeService claude;
    private final MotorReglasService reglas;
    private final RegistroInterpretacionRepository registros;

    public InterpretacionService(MotorClaudeService claude, MotorReglasService reglas,
                                 RegistroInterpretacionRepository registros) {
        this.claude = claude;
        this.reglas = reglas;
        this.registros = registros;
    }

    public InterpretacionDto interpretar(String texto, String idioma) {
        String lengua = "en".equals(idioma) ? "en" : "es";
        long inicio = System.currentTimeMillis();
        InterpretacionDto resultado;
        int activaciones = 0;
        if (!claude.disponible()) {
            resultado = reglas.interpretar(texto, lengua, MOTOR_DEMO);
        } else {
            try {
                MotorClaudeService.Resultado r = claude.interpretar(texto, lengua);
                resultado = r.interpretacion();
                activaciones = r.guardrailsActivados();
            } catch (AnthropicRestCliente.ClaudeNoDisponibleException | StatusRuntimeException e) {
                log.error("Fallo en el LLM, se usa el motor por reglas: {}", e.getMessage());
                resultado = reglas.interpretar(texto, lengua, MOTOR_RESPALDO);
            }
        }
        registros.save(new RegistroInterpretacion(resultado.motor(), lengua, resultado.valores().size(),
                activaciones, System.currentTimeMillis() - inicio));
        return resultado;
    }

    public String aviso(String idioma) {
        return AVISOS.getOrDefault(idioma, AVISOS.get("es"));
    }

    public String motorActual() {
        return claude.disponible() ? claude.modelo() : MOTOR_DEMO;
    }
}
