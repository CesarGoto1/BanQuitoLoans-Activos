package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FotoCarteraRepository extends JpaRepository<FotoCartera, FotoCarteraId> {

    List<FotoCartera> findByFechaFoto(LocalDate fechaFoto);

    List<FotoCartera> findByFechaFotoBetween(LocalDate fechaInicio, LocalDate fechaFin);

    List<FotoCartera> findByIdOperacionOrderByFechaFotoDesc(Integer idOperacion);

    Optional<FotoCartera> findFirstByIdOperacionAndFechaFotoBeforeOrderByFechaFotoDesc(Integer idOperacion, LocalDate fechaFoto);
}
