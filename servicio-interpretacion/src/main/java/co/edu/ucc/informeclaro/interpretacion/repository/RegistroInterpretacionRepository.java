package co.edu.ucc.informeclaro.interpretacion.repository;

import co.edu.ucc.informeclaro.interpretacion.model.RegistroInterpretacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistroInterpretacionRepository extends JpaRepository<RegistroInterpretacion, Long> {

    Optional<RegistroInterpretacion> findTopByOrderByIdDesc();
}
