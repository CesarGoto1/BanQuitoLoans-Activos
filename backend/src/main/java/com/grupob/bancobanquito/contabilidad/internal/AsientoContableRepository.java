package com.grupob.bancobanquito.contabilidad.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AsientoContableRepository extends JpaRepository<AsientoContable, Integer> {

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    List<AsientoContable> findByIdOperacionOrderByIdAsc(Integer idOperacion);
}
