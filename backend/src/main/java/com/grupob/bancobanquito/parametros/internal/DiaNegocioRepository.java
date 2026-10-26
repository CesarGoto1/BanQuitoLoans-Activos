package com.grupob.bancobanquito.parametros.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface DiaNegocioRepository extends JpaRepository<DiaNegocio, Integer> {

    Optional<DiaNegocio> findFirstByStatusNotOrderByFechaNegocioAsc(EstadoCierre status);
}
