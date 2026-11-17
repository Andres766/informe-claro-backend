package co.edu.ucc.informeclaro.interpretacion.dto;

import java.time.Instant;
import java.util.List;

/** Uso de cada motor, tiempos y últimos fallos de la IA, para el panel de administración. */
public record EstadisticasInterpretacionDto(
        long total,
        long duracionPromedioMs,
        long guardrailsActivados,
        long respaldo,
        List<UsoMotor> motores,
        List<Fallo> fallosRecientes) {

    public record UsoMotor(String motor, long interpretaciones, long duracionPromedioMs) {
    }

    public record Fallo(Instant fecha, String causa) {
    }
}
