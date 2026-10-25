package com.grupob.bancobanquito.loans;
import java.util.List;


public interface LoanOperationService {

    
    List<LoanOperationSummary> findByClient(String cedula);

    
    List<InstallmentSummary> findInstallments(String cedula, String numeroOperacion);
}















