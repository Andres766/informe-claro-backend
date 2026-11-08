package co.edu.ucc.informeclaro.informes.repository;

import co.edu.ucc.informeclaro.informes.model.InformeCargado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InformeRepository extends JpaRepository<InformeCargado, Long> {

    List<InformeCargado> findByUsuarioIdOrderByFechaCargaDescIdDesc(Long usuarioId);

    /** Solo encuentra el informe si pertenece a ese usuario: cada quien ve solo su historial. */
    Optional<InformeCargado> findByIdAndUsuarioId(Long id, Long usuarioId);
}
