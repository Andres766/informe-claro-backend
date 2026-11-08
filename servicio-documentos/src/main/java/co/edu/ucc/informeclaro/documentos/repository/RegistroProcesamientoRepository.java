package co.edu.ucc.informeclaro.documentos.repository;

import co.edu.ucc.informeclaro.documentos.model.RegistroProcesamiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroProcesamientoRepository extends JpaRepository<RegistroProcesamiento, Long> {

    long countByResultado(String resultado);
}
