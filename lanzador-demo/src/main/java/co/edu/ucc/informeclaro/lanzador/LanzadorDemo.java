package co.edu.ucc.informeclaro.lanzador;

import co.edu.ucc.informeclaro.documentos.DocumentosApplication;
import co.edu.ucc.informeclaro.informes.InformesApplication;
import co.edu.ucc.informeclaro.infraestructura.InfraestructuraApplication;
import co.edu.ucc.informeclaro.interpretacion.InterpretacionApplication;
import co.edu.ucc.informeclaro.usuarios.UsuariosApplication;

/**
 * Arranca todos los módulos en una sola JVM para entornos con poca memoria.
 *
 * <p>Cada módulo levanta su propio contexto de Spring, su configuración ({@code <modulo>.yml}) y su
 * servidor gRPC; la comunicación entre ellos sigue siendo gRPC sobre HTTP/2 por 127.0.0.1, igual que
 * cuando corren en contenedores separados con docker-compose.</p>
 */
public final class LanzadorDemo {

    private LanzadorDemo() {
    }

    public static void main(String[] args) {
        UsuariosApplication.main(args);
        DocumentosApplication.main(args);
        InterpretacionApplication.main(args);
        InformesApplication.main(args);
        InfraestructuraApplication.main(args); // por último: la API REST queda lista cuando todo está arriba
    }
}
