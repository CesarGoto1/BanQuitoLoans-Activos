package com.grupob.bancobanquito.origination;

import com.grupob.bancobanquito.origination.internal.LoanApplicationResponse;
import com.grupob.bancobanquito.origination.internal.RegisterLoanApplicationRequest;

public interface LoanApplicationService {

    /** RF-02: registra la solicitud del cliente y evalúa su capacidad de pago con la TEA referencial. */
    LoanApplicationResponse register(String identification, RegisterLoanApplicationRequest request);
}
