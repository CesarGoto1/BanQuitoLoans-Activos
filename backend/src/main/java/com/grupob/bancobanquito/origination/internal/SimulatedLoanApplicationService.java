package com.grupob.bancobanquito.origination.internal;

import com.grupob.bancobanquito.amortization.AmortizationMethod;
import com.grupob.bancobanquito.origination.ApprovedLoanApplication;
import com.grupob.bancobanquito.origination.LoanApplicationService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class SimulatedLoanApplicationService implements LoanApplicationService {

    private static final BigDecimal RATE = new BigDecimal("0.1550");

    private static final Map<Integer, ApprovedLoanApplication> APPROVED = Map.of(
            9001, new ApprovedLoanApplication(9001, 1, new BigDecimal("10000.00"), RATE, 2, AmortizationMethod.FRENCH),
            9002, new ApprovedLoanApplication(9002, 2, new BigDecimal("5000.00"), RATE, 12, AmortizationMethod.GERMAN),
            9003, new ApprovedLoanApplication(9003, 3, new BigDecimal("30000.00"), RATE, 60, AmortizationMethod.FRENCH));

    @Override
    public Optional<ApprovedLoanApplication> findApproved(Integer applicationId) {
        return Optional.ofNullable(APPROVED.get(applicationId));
    }
}
