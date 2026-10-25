package com.grupob.bancobanquito.origination;

import java.util.Optional;

public interface LoanApplicationService {

    Optional<ApprovedLoanApplication> findApproved(Integer applicationId);
}
