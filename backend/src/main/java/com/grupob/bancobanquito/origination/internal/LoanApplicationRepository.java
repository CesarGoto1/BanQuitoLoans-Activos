package com.grupob.bancobanquito.origination.internal;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

interface LoanApplicationRepository extends JpaRepository<LoanApplication, Integer> {

    Optional<LoanApplication> findByIdAndStatus(Integer id, LoanApplicationStatus status);

    boolean existsByCustomerIdAndStatus(Integer customerId, LoanApplicationStatus status);

    Page<LoanApplication> findByCustomerId(Integer customerId, Pageable pageable);

    Page<LoanApplication> findByCustomerIdAndStatus(Integer customerId, LoanApplicationStatus status, Pageable pageable);

    List<LoanApplication> findByStatusOrderByRegisteredAtAsc(LoanApplicationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "0"))
    @Query("select application from LoanApplication application where application.id = :applicationId")
    Optional<LoanApplication> findForDecision(@Param("applicationId") Integer applicationId);

    @Query("select application from LoanApplication application where application.status = :status "
            + "and not exists (select loan from CustomerLoan loan where loan.applicationId = application.id)")
    Page<LoanApplication> findWithoutLoan(@Param("status") LoanApplicationStatus status, Pageable pageable);
}
