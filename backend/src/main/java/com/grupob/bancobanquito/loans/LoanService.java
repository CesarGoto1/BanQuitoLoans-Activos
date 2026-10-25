package com.grupob.bancobanquito.loans;

import java.util.Optional;

public interface LoanService {

    DisbursementPreview previewDisbursement(Integer applicationId);

    DisbursementResult disburse(Integer applicationId, String destinationAccount);

    Optional<DisbursementResult> findDisbursement(Integer applicationId);
}
