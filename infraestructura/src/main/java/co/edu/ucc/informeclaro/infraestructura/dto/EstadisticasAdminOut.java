package co.edu.ucc.informeclaro.infraestructura.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Panel de administración: cuentas y cifras agregadas de los microservicios. Nunca incluye el
 * contenido de los informes de un paciente.
 */
public record EstadisticasAdminOut(
        Totales totales,
        List<Usuario> usuarios,
        @JsonProperty("informes_por_mes") List<PorMes> informesPorMes,
        @JsonProperty("informes_por_idioma") List<PorIdioma> informesPorIdioma,
        Interpretaciones interpretaciones,
        List<Motor> motores,
        @JsonProperty("valores_fuera_de_rango") List<ValorFrecuente> valoresFueraDeRango,
        @JsonProperty("fallos_recientes") List<Fallo> fallosRecientes) {

    public record Totales(long usuarios, long administradores, long informes, long valores,
                          @JsonProperty("fuera_de_rango") long fueraDeRango) {
    }

    public record Usuario(long id, String nombre, String email, String rol, long informes,
                          @JsonProperty("ultimo_informe") String ultimoInforme) {
    }

    public record PorMes(String mes, long informes) {
    }

    public record PorIdioma(String idioma, long informes) {
    }

    public record Interpretaciones(long total, @JsonProperty("duracion_promedio_ms") long duracionPromedioMs,
                                   @JsonProperty("guardrails_activados") long guardrailsActivados, long respaldo) {
    }

    public record Motor(String motor, long interpretaciones, @JsonProperty("duracion_promedio_ms") long duracionPromedioMs) {
    }

    public record ValorFrecuente(@JsonProperty("nombre_valor") String nombreValor,
                                 @JsonProperty("fuera_de_rango") long fueraDeRango, long total) {
    }

    public record Fallo(String fecha, String causa) {
    }
}
