package co.edu.ucc.informeclaro.documentos.service;

import co.edu.ucc.informeclaro.documentos.dto.ValorExtraidoDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extractor por reglas: reconoce líneas "nombre  valor  unidad  rango" del informe. */
@Service
public class ExtractorValoresService {

    private static final String NUM = ClasificadorService.NUM;
    private static final Pattern LINEA_VALOR = Pattern.compile(
            "^(?<nombre>[A-Za-zÁÉÍÓÚÜáéíóúüñÑ][A-Za-zÁÉÍÓÚÜáéíóúüñÑ0-9().,%/ -]*?)\\s+"
                    + "(?<valor>" + NUM + ")\\s+"
                    + "(?<unidad>\\S+)\\s+"
                    + "(?<rango>(?:[<>≤≥]=?\\s*" + NUM + ")|(?:" + NUM + "\\s*[-–]\\s*" + NUM + "))\\s*$");

    private final ClasificadorService clasificador;

    public ExtractorValoresService(ClasificadorService clasificador) {
        this.clasificador = clasificador;
    }

    public List<ValorExtraidoDto> extraer(String texto) {
        return texto.lines()
                .map(String::strip)
                .map(LINEA_VALOR::matcher)
                .filter(Matcher::matches)
                .map(m -> {
                    String valor = m.group("valor");
                    String rango = m.group("rango").strip();
                    return new ValorExtraidoDto(limpiarNombre(m.group("nombre")), valor, m.group("unidad"),
                            rango, clasificador.clasificar(valor, rango));
                })
                .toList();
    }

    /** Quita espacios, puntos y guiones sobrantes en los extremos del nombre. */
    static String limpiarNombre(String nombre) {
        int inicio = 0;
        int fin = nombre.length();
        while (inicio < fin && " .-".indexOf(nombre.charAt(inicio)) >= 0) {
            inicio++;
        }
        while (fin > inicio && " .-".indexOf(nombre.charAt(fin - 1)) >= 0) {
            fin--;
        }
        return nombre.substring(inicio, fin);
    }
}
