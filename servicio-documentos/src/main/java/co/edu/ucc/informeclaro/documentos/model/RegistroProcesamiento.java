package co.edu.ucc.informeclaro.documentos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Bitácora de cada PDF procesado. Por privacidad NO guarda el PDF, su texto ni su nombre:
 * solo cuándo se procesó, su tamaño y el resultado.
 */
@Entity
@Table(name = "registro_procesamiento")
public class RegistroProcesamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false)
    private int paginas;

    @Column(nullable = false)
    private int caracteres;

    @Column(nullable = false, length = 30)
    private String resultado;

    protected RegistroProcesamiento() {
    }

    public RegistroProcesamiento(int paginas, int caracteres, String resultado) {
        this.fecha = Instant.now();
        this.paginas = paginas;
        this.caracteres = caracteres;
        this.resultado = resultado;
    }

    public Long getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public int getPaginas() {
        return paginas;
    }

    public int getCaracteres() {
        return caracteres;
    }

    public String getResultado() {
        return resultado;
    }
}
