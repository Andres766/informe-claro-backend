package co.edu.ucc.informeclaro.informes.dto;

import java.time.Instant;
import java.util.List;

/** Cifras agregadas del historial para el panel de administración. No incluye contenido de informes. */
public record EstadisticasInformesDto(
        long totalInformes,
        long totalValores,
        long fueraDeRango,
        List<PorUsuario> porUsuario,
        List<PorMes> porMes,
        List<PorIdioma> porIdioma,
        List<ValorFrecuente> valoresFueraDeRango) {

    public record PorUsuario(long usuarioId, long informes, Instant ultimoInforme) {
    }

    public record PorMes(String mes, long informes) {
    }

    public record PorIdioma(String idioma, long informes) {
    }

    public record ValorFrecuente(String nombreValor, long fueraDeRango, long total) {
    }
}
