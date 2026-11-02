package com.grupob.bancobanquito.cierre.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface EjecucionEtapaRepository extends JpaRepository<EjecucionEtapa, Integer> {

    List<EjecucionEtapa> findByIdCierreOrderByNumeroEtapaAscNumeroIntentoAsc(Integer idCierre);

    List<EjecucionEtapa> findByEstadoEtapa(EstadoEtapa estadoEtapa);
}
