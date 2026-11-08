package co.edu.ucc.informeclaro.interpretacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Segunda capa de guardrails clínicos: si un texto diagnostica, recomienda tratamiento o
 * genera alarma (en español o en inglés), se reemplaza por un texto seguro que remite al médico.
 */
@Service
public class GuardrailsService {

    private static final Logger log = LoggerFactory.getLogger(GuardrailsService.class);

    static final Pattern FRASES_PROHIBIDAS = Pattern.compile(
            // Español
            "\\b(usted|ud\\.?|tú|te)\\s+(tiene|tienes|padece|padeces|sufre|sufres)\\b"
            + "|\\b(podría|puede|podrías|puedes)\\s+(tener|padecer|indicar\\s+que\\s+tiene)\\b"
            + "|\\b(tome|toma|tomar|suspenda|suspende|aumente|disminuya)\\s+(el|la|los|las|un|una|su|tu)?\\s*"
            + "(medicamento|medicina|pastilla|dosis|suplemento|tratamiento)"
            + "|\\b(le|te)\\s+recomiendo\\b|\\bdebe(s|ría|rías)?\\s+tomar\\b|\\bdosis\\b"
            + "|\\b(grave|peligros[oa]|urgente|alarmante)\\b"
            // Inglés
            + "|\\byou\\s+(have|suffer\\s+from|are\\s+suffering|might\\s+have|may\\s+have|could\\s+have|probably\\s+have)\\b"
            + "|\\b(this|it)\\s+(means|indicates|suggests|shows)\\s+(that\\s+)?you\\s+(have|are)\\b"
            + "|\\b(take|stop|start|increase|decrease)\\s+(your|the|a|an)?\\s*"
            + "(medication|medicine|medicines|pills?|dose|dosage|supplements?|treatment)"
            + "|\\bI\\s+(recommend|suggest|advise)\\b|\\byou\\s+should\\s+(take|stop|start)\\b|\\b(dose|dosage)\\b"
            + "|\\b(serious|dangerous|urgent|alarming|emergency)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Map<String, String> TEXTO_SEGURO = Map.of(
            "es", "Este examen hace parte de tu informe. Tu médico es la persona indicada para explicarte "
                    + "qué significa este resultado en tu caso.",
            "en", "This test is part of your report. Your doctor is the right person to explain "
                    + "what this result means in your case.");

    /** Resultado de revisar un texto: el texto final y si el guardrail tuvo que actuar. */
    public record Revision(String texto, boolean activado) {
    }

    public boolean cruzaLaLinea(String texto) {
        return FRASES_PROHIBIDAS.matcher(texto).find();
    }

    public Revision revisar(String texto, String idioma) {
        if (cruzaLaLinea(texto)) {
            log.warn("Guardrail activado; texto reemplazado: {}", texto.length() > 200 ? texto.substring(0, 200) : texto);
            return new Revision(TEXTO_SEGURO.getOrDefault(idioma, TEXTO_SEGURO.get("es")), true);
        }
        return new Revision(texto, false);
    }

    public String aplicar(String texto, String idioma) {
        return revisar(texto, idioma).texto();
    }
}
