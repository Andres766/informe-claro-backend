package co.edu.ucc.informeclaro.documentos.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/** Oculta los datos personales antes de que el texto salga hacia la API del LLM. */
@Service
public class AnonimizadorService {

    private static final int BANDERAS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;

    private record Regla(Pattern patron, String reemplazo) {
    }

    private static final List<Regla> REGLAS = List.of(
            // "Paciente: Nombre Apellido" / "Patient name: ..." -> se oculta el valor de la línea
            new Regla(Pattern.compile("^(\\s*(paciente|nombre|documento|c\\.?c\\.?|identificaci[oó]n|direcci[oó]n|tel[eé]fono"
                    + "|fecha\\s+de\\s+nacimiento"
                    + "|patient(\\s+name)?|name|id|patient\\s+id|mrn|medical\\s+record(\\s+number)?"
                    + "|address|phone|date\\s+of\\s+birth|dob)\\s*:).*$", BANDERAS | Pattern.MULTILINE), "$1 [OCULTO]"),
            // Números largos (cédulas, teléfonos, historias clínicas)
            new Regla(Pattern.compile("\\b\\d{6,}\\b", BANDERAS), "[OCULTO]"),
            // Correos
            new Regla(Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+", BANDERAS), "[OCULTO]"));

    public String anonimizar(String texto) {
        String resultado = texto;
        for (Regla regla : REGLAS) {
            resultado = regla.patron().matcher(resultado).replaceAll(regla.reemplazo());
        }
        return resultado;
    }
}
