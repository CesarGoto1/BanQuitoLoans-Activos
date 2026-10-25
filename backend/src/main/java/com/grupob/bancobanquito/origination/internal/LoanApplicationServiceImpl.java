package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.origination.ApprovedLoanApplication;
import com.grupob.bancobanquito.origination.LoanApplicationService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;

    LoanApplicationServiceImpl(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    @Override
    public Optional<ApprovedLoanApplication> findApproved(Integer applicationId) {
        return loanApplicationRepository.findByIdAndStatus(applicationId, LoanApplicationStatus.APPROVED)
                .map(application -> new ApprovedLoanApplication(application.getId(), application.getCustomerId(),
                        application.getRequestedAmount(), application.getAssignedRate(), application.getTermMonths(),
                        application.getAmortizationMethod()));
    }
}
