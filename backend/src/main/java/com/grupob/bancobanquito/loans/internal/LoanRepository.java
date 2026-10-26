package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.EstadoCartera;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoanRepository extends JpaRepository<Loan, Integer> {

    boolean existsByApplicationId(Integer applicationId);

    Optional<Loan> findByApplicationId(Integer applicationId);

    Optional<Loan> findByLoanNumber(String loanNumber);

    Page<Loan> findByCustomerNationalId(String customerNationalId, Pageable pageable);

    Page<Loan> findByCustomerNationalIdAndPortfolioStatus(String customerNationalId, EstadoCartera portfolioStatus,
                                                          Pageable pageable);
}
