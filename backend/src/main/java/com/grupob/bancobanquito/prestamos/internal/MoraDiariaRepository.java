package com.grupob.bancobanquito.prestamos.internal;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

interface MoraDiariaRepository extends JpaRepository<MoraDiaria, Integer> {

    boolean existsByInstallmentIdAndDefaultDate(Integer installmentId, LocalDate defaultDate);
}
