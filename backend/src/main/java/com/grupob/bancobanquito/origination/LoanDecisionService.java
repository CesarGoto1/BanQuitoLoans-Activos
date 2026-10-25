package com.grupob.bancobanquito.origination;

import java.math.BigDecimal;

public interface LoanDecisionService {

    LoanDecisionResult approveApplication(Integer applicationId, String identification, BigDecimal annualEffectiveRate);

    LoanDecisionResult rejectApplication(Integer applicationId, String identification, String rejectionReason);
}
