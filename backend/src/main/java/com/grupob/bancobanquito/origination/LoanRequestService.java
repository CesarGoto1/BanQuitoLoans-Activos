package com.grupob.bancobanquito.origination;
import java.util.List;


public interface LoanRequestService {

    
    List<LoanRequestSummary> findByClient(String cedula, String estado);
}















