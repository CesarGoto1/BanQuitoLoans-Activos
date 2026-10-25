package com.grupob.bancobanquito.origination.internal;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoanApplicationRepository extends JpaRepository<LoanApplication, Integer> {

    Optional<LoanApplication> findByIdAndStatus(Integer id, LoanApplicationStatus status);

    boolean existsByCustomerIdAndStatus(Integer customerId, LoanApplicationStatus status);

    Page<LoanApplication> findByCustomerId(Integer customerId, Pageable pageable);

    Page<LoanApplication> findByCustomerIdAndStatus(Integer customerId, LoanApplicationStatus status, Pageable pageable);
}
