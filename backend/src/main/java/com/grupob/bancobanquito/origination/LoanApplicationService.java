package com.grupob.bancobanquito.origination;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanApplicationService {

    Optional<ApprovedLoanApplication> findApproved(Integer applicationId);

    Page<PendingDisbursementApplication> findPendingDisbursement(Pageable pageable);
}
