package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperacionCobranzaRepository extends JpaRepository<OperacionCobranza, Integer> {

    Optional<OperacionCobranza> findByNumeroOperacion(String numeroOperacion);

    @Query("SELECT o FROM OperacionCobranza o WHERE o.estadoCartera <> 'CANCELADA' ORDER BY o.idOperacion ASC")
    List<OperacionCobranza> findOperacionesActivas();

    List<OperacionCobranza> findByEstadoCartera(String estadoCartera);
}
