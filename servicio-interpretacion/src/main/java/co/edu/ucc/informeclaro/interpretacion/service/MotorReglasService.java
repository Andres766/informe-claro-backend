package co.edu.ucc.informeclaro.interpretacion.service;

import co.edu.ucc.informeclaro.interpretacion.cliente.ClienteDocumentos;
import co.edu.ucc.informeclaro.interpretacion.dto.InterpretacionDto;
import co.edu.ucc.informeclaro.interpretacion.dto.ValorBaseDto;
import co.edu.ucc.informeclaro.interpretacion.dto.ValorInterpretadoDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Motor por reglas (modo demo o respaldo cuando la IA no está disponible): glosario y
 * plantillas en español e inglés, sin diagnosticar.
 */
@Service
public class MotorReglasService {

    private record Entrada(List<String> claves, String es, String en) {
    }

    // Qué mide cada examen; las claves cubren el nombre en español y en inglés.
    private static final List<Entrada> GLOSARIO = List.of(
            new Entrada(List.of("glucosa", "glucose"),
                    "mide la cantidad de azúcar en la sangre, que el cuerpo usa como fuente de energía",
                    "measures the amount of sugar in your blood, which your body uses for energy"),
            new Entrada(List.of("colesterol total", "total cholesterol"),
                    "mide el total de colesterol, un tipo de grasa que el cuerpo necesita en cierta cantidad",
                    "measures your total cholesterol, a type of fat your body needs in a certain amount"),
            new Entrada(List.of("hdl"),
                    "mide el colesterol HDL, conocido como 'colesterol bueno' porque ayuda a retirar grasa de las arterias",
                    "measures HDL cholesterol, known as 'good cholesterol' because it helps remove fat from the arteries"),
            new Entrada(List.of("ldl"),
                    "mide el colesterol LDL, la parte del colesterol que puede acumularse en las paredes de las arterias",
                    "measures LDL cholesterol, the part of cholesterol that can build up on artery walls"),
            new Entrada(List.of("triglic", "triglycer"),
                    "mide los triglicéridos, otro tipo de grasa en la sangre que viene en parte de lo que comemos",
                    "measures triglycerides, another type of fat in the blood that comes partly from what we eat"),
            new Entrada(List.of("hemoglobina glicosilada", "a1c", "glycated"),
                    "refleja el promedio de azúcar en la sangre de los últimos dos a tres meses",
                    "reflects your average blood sugar over the last two to three months"),
            new Entrada(List.of("hemoglobina", "hemoglobin", "haemoglobin"),
                    "mide la proteína de los glóbulos rojos que transporta el oxígeno por el cuerpo",
                    "measures the protein in red blood cells that carries oxygen around the body"),
            new Entrada(List.of("hematocrito", "hematocrit"),
                    "indica qué porcentaje de la sangre está formado por glóbulos rojos",
                    "shows what percentage of your blood is made up of red blood cells"),
            new Entrada(List.of("leucocitos", "white blood", "wbc", "leukocyte"),
                    "cuenta los glóbulos blancos, las células que participan en las defensas del cuerpo",
                    "counts white blood cells, the cells that help your body defend itself"),
            new Entrada(List.of("plaquetas", "platelet"),
                    "cuenta las plaquetas, las células que ayudan a que la sangre coagule",
                    "counts platelets, the cells that help your blood clot"),
            new Entrada(List.of("creatinina", "creatinine"),
                    "mide un desecho de los músculos que los riñones filtran; se usa para valorar su funcionamiento",
                    "measures a waste product from muscles that the kidneys filter; it is used to check how they work"),
            new Entrada(List.of("tsh"),
                    "mide una hormona que regula el funcionamiento de la glándula tiroides",
                    "measures a hormone that regulates how the thyroid gland works"),
            new Entrada(List.of("ácido úrico", "acido urico", "uric acid"),
                    "mide una sustancia de desecho que el cuerpo produce al procesar ciertos alimentos",
                    "measures a waste substance the body makes when it processes certain foods"),
            new Entrada(List.of("vitamina d", "vitamin d"),
                    "mide la vitamina D, que ayuda al cuerpo a aprovechar el calcio de los huesos",
                    "measures vitamin D, which helps your body use calcium for your bones"),
            new Entrada(List.of("vitamina b12", "vitamin b12"),
                    "mide la vitamina B12, que participa en la formación de la sangre y en el sistema nervioso",
                    "measures vitamin B12, which is involved in making blood and in the nervous system"));

    private static final Map<String, Map<String, String>> FRASE_ESTADO = Map.of(
            "es", Map.of(
                    "normal", "Tu resultado (%s) está dentro del rango de referencia del laboratorio (%s).",
                    "alto", "Tu resultado (%s) está por encima del rango de referencia del laboratorio (%s). "
                            + "Es un buen tema para conversar con tu médico.",
                    "bajo", "Tu resultado (%s) está por debajo del rango de referencia del laboratorio (%s). "
                            + "Es un buen tema para conversar con tu médico.",
                    "sin_rango", "El informe no trae un rango de referencia claro para este valor (%s)."),
            "en", Map.of(
                    "normal", "Your result (%s) is within the laboratory's reference range (%s).",
                    "alto", "Your result (%s) is above the laboratory's reference range (%s). "
                            + "It is a good topic to talk about with your doctor.",
                    "bajo", "Your result (%s) is below the laboratory's reference range (%s). "
                            + "It is a good topic to talk about with your doctor.",
                    "sin_rango", "The report does not include a clear reference range for this value (%s)."));

    private static final Map<String, List<String>> PREGUNTAS_GENERALES = Map.of(
            "es", List.of(
                    "¿Alguno de estos resultados requiere un examen de control? ¿Cuándo debería repetirlo?",
                    "¿Cómo se comparan estos resultados con mis exámenes anteriores?"),
            "en", List.of(
                    "Do any of these results need a follow-up test? When should I repeat it?",
                    "How do these results compare with my previous tests?"));

    private final ClienteDocumentos documentos;

    public MotorReglasService(ClienteDocumentos documentos) {
        this.documentos = documentos;
    }

    public InterpretacionDto interpretar(String texto, String idioma, String motor) {
        List<ValorInterpretadoDto> valores = documentos.extraerValores(texto).stream()
                .map(v -> new ValorInterpretadoDto(v.nombreValor(), v.valor(), v.unidad(), v.rangoReferencia(),
                        v.estado(), explicar(v, idioma)))
                .toList();
        List<ValorInterpretadoDto> fuera = valores.stream().filter(v -> esFueraDeRango(v.estado())).toList();

        List<String> preguntas = new ArrayList<>();
        fuera.stream().limit(4).forEach(v -> preguntas.add(preguntaFueraDeRango(v.nombreValor(), v.estado(), idioma)));
        preguntas.addAll(PREGUNTAS_GENERALES.get(idioma));

        return new InterpretacionDto(resumen(valores.size(), fuera.size(), idioma), valores, preguntas, motor);
    }

    static String queMide(String nombre, String idioma) {
        String minusculas = nombre.toLowerCase(Locale.ROOT);
        return GLOSARIO.stream()
                .filter(e -> e.claves().stream().anyMatch(minusculas::contains))
                .map(e -> idioma.equals("en") ? e.en() : e.es())
                .findFirst().orElse(null);
    }

    static String explicar(ValorBaseDto v, String idioma) {
        String queMide = queMide(v.nombreValor(), idioma);
        String base = queMide == null ? "" : (idioma.equals("en") ? "This test " : "Este examen ") + queMide + ". ";
        String valor = (v.valor() + " " + v.unidad()).strip();
        return base + FRASE_ESTADO.get(idioma).get(v.estado()).formatted(valor, v.rangoReferencia());
    }

    /** "Triglicéridos" -> plural, para decir «mis … estén» / «my … are». */
    static boolean esPlural(String nombre) {
        String[] palabras = nombre.strip().split("\\s+");
        String primera = palabras.length > 0 ? palabras[0].toLowerCase(Locale.ROOT) : "";
        return primera.endsWith("s") && primera.length() > 3;
    }

    /** "Colesterol LDL" -> "colesterol LDL", "Vitamina D" -> "vitamina D" (conserva siglas y letras sueltas). */
    static String nombreEnFrase(String nombre) {
        String[] palabras = nombre.strip().split("\\s+");
        StringBuilder frase = new StringBuilder();
        for (int i = 0; i < palabras.length; i++) {
            String p = palabras[i];
            boolean conservar = i > 0 && (esMayuscula(p) || p.length() == 1);
            frase.append(i > 0 ? " " : "").append(conservar ? p : p.toLowerCase(Locale.ROOT));
        }
        return frase.toString();
    }

    private static boolean esMayuscula(String palabra) {
        boolean tieneLetra = false;
        for (char c : palabra.toCharArray()) {
            if (Character.isLetter(c)) {
                tieneLetra = true;
                if (!Character.isUpperCase(c)) {
                    return false;
                }
            }
        }
        return tieneLetra;
    }

    static String preguntaFueraDeRango(String nombre, String estado, String idioma) {
        boolean plural = esPlural(nombre);
        if (idioma.equals("en")) {
            String posicion = estado.equals("alto") ? "above" : "below";
            return "What does it mean for me that my " + nombreEnFrase(nombre) + " "
                    + (plural ? "are" : "is") + " " + posicion + " the reference range?";
        }
        String posicion = estado.equals("alto") ? "por encima" : "por debajo";
        return "¿Qué significa para mí que " + (plural ? "mis" : "mi") + " " + nombreEnFrase(nombre) + " "
                + (plural ? "estén" : "esté") + " " + posicion + " del rango de referencia?";
    }

    static String resumen(int total, int fuera, String idioma) {
        if (idioma.equals("en")) {
            if (total == 0) {
                return "No values in the expected format were found in this report.";
            }
            if (fuera > 0) {
                return "Your report has " + total + " values; " + fuera + " are outside the laboratory's "
                        + "reference range. That alone does not mean something is wrong: review the suggested "
                        + "questions for your next appointment.";
            }
            return "Your report has " + total + " values and all of them are within the reference range.";
        }
        if (total == 0) {
            return "No se encontraron valores con el formato esperado en este informe.";
        }
        if (fuera > 0) {
            return "Tu informe tiene " + total + " valores; " + fuera + " están fuera del rango "
                    + "de referencia del laboratorio. Eso no significa por sí solo que algo esté mal: "
                    + "revisa las preguntas sugeridas para tu próxima consulta.";
        }
        return "Tu informe tiene " + total + " valores y todos están dentro del rango de referencia.";
    }

    static boolean esFueraDeRango(String estado) {
        return estado.equals("alto") || estado.equals("bajo");
    }
}
