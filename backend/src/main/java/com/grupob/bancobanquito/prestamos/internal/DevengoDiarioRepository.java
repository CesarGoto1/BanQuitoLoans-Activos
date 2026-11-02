package com.grupob.bancobanquito.prestamos.internal;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

interface DevengoDiarioRepository extends JpaRepository<DevengoDiario, Integer> {

    boolean existsByLoanIdAndAccrualDate(Integer loanId, LocalDate accrualDate);
}
