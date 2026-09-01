package cl.duoc.bancoxyz.repository;

import cl.duoc.bancoxyz.model.EstadoCuentaResumen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoCuentaResumenRepository
        extends JpaRepository<EstadoCuentaResumen, Long> {
}