package com.grupob.bancobanquito.prestamos.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CuotaRepository extends JpaRepository<Cuota, Integer> {

    Page<Cuota> findByLoanId(Integer loanId, Pageable paginacion);

    long countByLoanId(Integer loanId);
}
