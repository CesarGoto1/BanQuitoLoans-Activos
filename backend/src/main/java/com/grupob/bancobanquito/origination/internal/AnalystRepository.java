package com.grupob.bancobanquito.origination.internal;

import java.util.Optional;
import org.springframework.data.repository.Repository;

interface AnalystRepository extends Repository<Analyst, Integer> {

    Optional<Analyst> findByIdentification(String identification);
}
