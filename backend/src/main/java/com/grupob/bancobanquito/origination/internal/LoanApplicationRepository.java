package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.internal.LoanApplication.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoanApplicationRepository extends JpaRepository<LoanApplication, Integer> {

    boolean existsByCustomerIdAndStatus(Integer customerId, ApplicationStatus status);
}
