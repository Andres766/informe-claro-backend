package co.edu.ucc.informeclaro.documentos.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compara un valor contra su rango de referencia de forma determinista: "normal", "alto",
 * "bajo" o "sin_rango". Lo decide el código, no el LLM, para que cada marca sea verificable.
 */
@Service
public class ClasificadorService {

    static final String NUM = "\\d+(?:[.,]\\d+)?";
    private static final Pattern NUMERO = Pattern.compile(NUM);
    private static final Pattern ENTRE = Pattern.compile("(" + NUM + ")\\s*[-–]\\s*(" + NUM + ")");
    private static final Pattern MENOR = Pattern.compile("(<|≤|<=)\\s*(" + NUM + ")");
    private static final Pattern MAYOR = Pattern.compile("(>|≥|>=)\\s*(" + NUM + ")");

    public String clasificar(String valor, String rango) {
        Matcher numero = NUMERO.matcher(valor == null ? "" : valor);
        if (!numero.find()) {
            return "sin_rango";
        }
        double v = aNumero(numero.group());
        String r = rango == null ? "" : rango.strip();

        Matcher m = ENTRE.matcher(r);
        if (m.matches()) {
            double bajo = aNumero(m.group(1));
            double alto = aNumero(m.group(2));
            return v < bajo ? "bajo" : v > alto ? "alto" : "normal";
        }
        m = MENOR.matcher(r);
        if (m.matches()) {
            double limite = aNumero(m.group(2));
            boolean dentro = m.group(1).equals("<") ? v < limite : v <= limite;
            return dentro ? "normal" : "alto";
        }
        m = MAYOR.matcher(r);
        if (m.matches()) {
            double limite = aNumero(m.group(2));
            boolean dentro = m.group(1).equals(">") ? v > limite : v >= limite;
            return dentro ? "normal" : "bajo";
        }
        return "sin_rango";
    }

    private static double aNumero(String texto) {
        return Double.parseDouble(texto.replace(',', '.'));
    }
}
