package com.grupob.bancobanquito.prestamos.internal;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DevengoDiarioRepository extends JpaRepository<DevengoDiario, Integer> {

    boolean existsByLoanIdAndAccrualDate(Integer loanId, LocalDate accrualDate);

    List<DevengoDiario> findByLoanIdAndAccrualDateBetweenOrderByAccrualDateAsc(
            Integer loanId, LocalDate from, LocalDate to);
}
