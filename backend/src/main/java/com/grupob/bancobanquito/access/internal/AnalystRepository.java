package com.grupob.bancobanquito.access.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AnalystRepository extends JpaRepository<Analyst, Integer> {

    Optional<Analyst> findByNationalId(String nationalId);
}
