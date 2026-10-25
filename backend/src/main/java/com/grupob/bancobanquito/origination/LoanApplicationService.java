package com.grupob.bancobanquito.origination;

import com.grupob.bancobanquito.origination.internal.LoanApplicationResponse;
import com.grupob.bancobanquito.origination.internal.RegisterLoanApplicationRequest;

public interface LoanApplicationService {

    LoanApplicationResponse register(String identification, RegisterLoanApplicationRequest request);
}
