package com.grupob.bancobanquito.loans.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface InstallmentRepository extends JpaRepository<Installment, Integer> {

    Page<Installment> findByLoanId(Integer loanId, Pageable pageable);

    long countByLoanId(Integer loanId);
}
