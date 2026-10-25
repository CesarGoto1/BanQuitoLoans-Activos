package com.grupob.bancobanquito.loans;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoanService {

    DisbursementPreview previewDisbursement(Integer applicationId);

    DisbursementResult disburse(Integer applicationId, String destinationAccount);

    Optional<DisbursementResult> findDisbursement(Integer applicationId);

    Page<PendingDisbursement> findPendingDisbursements(Pageable pageable);
}
