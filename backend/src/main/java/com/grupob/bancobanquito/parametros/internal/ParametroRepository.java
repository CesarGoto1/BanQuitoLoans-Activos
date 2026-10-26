package com.grupob.bancobanquito.parametros.internal;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ParametroRepository extends JpaRepository<Parametro, Integer> {

    Optional<Parametro> findFirstByCodeAndValidFromLessThanEqualOrderByValidFromDesc(String code, LocalDate date);
}
