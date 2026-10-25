package com.grupob.bancobanquito.parameters.internal;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ParameterRepository extends JpaRepository<Parameter, Integer> {

    Optional<Parameter> findFirstByCodeAndValidFromLessThanEqualOrderByValidFromDesc(String code, LocalDate date);
}
