package com.grupob.bancobanquito.prestamos.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CambioEstadoOperacionRepository extends JpaRepository<CambioEstadoOperacion, Integer> {

    List<CambioEstadoOperacion> findByIdOperacionOrderByFechaCambioAscIdAsc(Integer idOperacion);
}
