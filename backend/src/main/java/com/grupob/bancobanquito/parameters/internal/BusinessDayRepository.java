package com.grupob.bancobanquito.parameters.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface BusinessDayRepository extends JpaRepository<BusinessDay, Integer> {

    Optional<BusinessDay> findFirstByStatusNotOrderByBusinessDateAsc(ClosingStatus status);
}
