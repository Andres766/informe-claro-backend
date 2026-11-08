package co.edu.ucc.informeclaro.interpretacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Bitácora de cada interpretación: qué motor respondió, en qué idioma, cuánto tardó y cuántas
 * veces actuaron los guardrails clínicos. No guarda el texto del informe.
 */
@Entity
@Table(name = "registro_interpretacion")
public class RegistroInterpretacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, length = 40)
    private String motor;

    @Column(nullable = false, length = 2)
    private String idioma;

    @Column(name = "total_valores", nullable = false)
    private int totalValores;

    @Column(name = "guardrails_activados", nullable = false)
    private int guardrailsActivados;

    @Column(name = "duracion_ms", nullable = false)
    private long duracionMs;

    protected RegistroInterpretacion() {
    }

    public RegistroInterpretacion(String motor, String idioma, int totalValores, int guardrailsActivados, long duracionMs) {
        this.fecha = Instant.now();
        this.motor = motor;
        this.idioma = idioma;
        this.totalValores = totalValores;
        this.guardrailsActivados = guardrailsActivados;
        this.duracionMs = duracionMs;
    }

    public Long getId() {
        return id;
    }

    public String getMotor() {
        return motor;
    }

    public String getIdioma() {
        return idioma;
    }

    public int getTotalValores() {
        return totalValores;
    }

    public int getGuardrailsActivados() {
        return guardrailsActivados;
    }

    public long getDuracionMs() {
        return duracionMs;
    }
}
