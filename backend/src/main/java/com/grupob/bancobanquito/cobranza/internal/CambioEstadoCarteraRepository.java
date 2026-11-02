package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CambioEstadoCarteraRepository extends JpaRepository<CambioEstadoOperacion, Integer> {

    boolean existsByIdOperacionAndFechaCambio(Integer idOperacion, LocalDate fechaCambio);

    Optional<CambioEstadoOperacion> findByIdOperacionAndFechaCambio(Integer idOperacion, LocalDate fechaCambio);

    List<CambioEstadoOperacion> findByIdOperacionOrderByFechaCambioDesc(Integer idOperacion);
}
