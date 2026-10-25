package com.grupob.bancobanquito.liabilities;

import java.util.List;
import java.util.Optional;

public interface LiabilitiesService {

    Optional<Customer> findCustomer(Integer customerId);

    List<Account> findActiveAccounts(Integer customerId);

    void creditDisbursement(DisbursementInstruction instruction);
}
