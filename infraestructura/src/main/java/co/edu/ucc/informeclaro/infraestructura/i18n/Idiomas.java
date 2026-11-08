package co.edu.ucc.informeclaro.infraestructura.i18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Idiomas de la API: español (por defecto) e inglés, según el encabezado Accept-Language. */
public final class Idiomas {

    public static final List<String> SOPORTADOS = List.of("es", "en");

    private Idiomas() {
    }

    /** "en-US,en;q=0.9,es;q=0.8" -> "en". Primer idioma soportado por orden de preferencia. */
    public static String normalizar(String cabecera, String porDefecto) {
        if (cabecera == null || cabecera.isBlank()) {
            return porDefecto;
        }
        record Candidato(double calidad, String codigo) {
        }
        List<Candidato> candidatos = new ArrayList<>();
        for (String parte : cabecera.split(",")) {
            String[] trozos = parte.strip().split(";", 2);
            double calidad = 1.0;
            if (trozos.length > 1 && trozos[1].strip().startsWith("q=")) {
                try {
                    calidad = Double.parseDouble(trozos[1].strip().substring(2));
                } catch (NumberFormatException e) {
                    calidad = 0.0;
                }
            }
            String etiqueta = trozos[0].strip().toLowerCase(Locale.ROOT);
            candidatos.add(new Candidato(calidad, etiqueta.length() >= 2 ? etiqueta.substring(0, 2) : etiqueta));
        }
        return candidatos.stream()
                .sorted(Comparator.comparingDouble(Candidato::calidad).reversed())
                .map(Candidato::codigo)
                .filter(SOPORTADOS::contains)
                .findFirst()
                .orElse(porDefecto);
    }

    public static boolean soportado(String valor) {
        return valor != null && valor.strip().length() >= 2
                && SOPORTADOS.contains(valor.strip().toLowerCase(Locale.ROOT).substring(0, 2));
    }
}
