package com.grupob.bancobanquito.liabilities.internal;

import com.grupob.bancobanquito.liabilities.Account;
import com.grupob.bancobanquito.liabilities.Customer;
import com.grupob.bancobanquito.liabilities.DisbursementInstruction;
import com.grupob.bancobanquito.liabilities.LiabilitiesService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
class SimulatedLiabilitiesService implements LiabilitiesService {

    static final String FAILING_ACCOUNT = "9999999999";

    private static final Map<Integer, Customer> CUSTOMERS = Map.of(
            1, new Customer(1, "1710034065", "María Fernanda", "Andrade Lema"),
            2, new Customer(2, "1712345675", "Luis Alberto", "Paredes Mora"),
            3, new Customer(3, "1709876543", "Ana Lucía", "Gómez Vela"));

    private static final Map<Integer, List<Account>> ACCOUNTS = Map.of(
            1, List.of(new Account("2200000001", "AHORROS"), new Account("3100000001", "CORRIENTE"),
                    new Account(FAILING_ACCOUNT, "AHORROS")),
            2, List.of(new Account("2200000002", "AHORROS")),
            3, List.of(new Account("2200000003", "AHORROS")));

    @Override
    public Optional<Customer> findCustomer(Integer customerId) {
        return Optional.ofNullable(CUSTOMERS.get(customerId));
    }

    @Override
    public List<Account> findActiveAccounts(Integer customerId) {
        return ACCOUNTS.getOrDefault(customerId, List.of());
    }

    @Override
    public void creditDisbursement(DisbursementInstruction instruction) {
        if (FAILING_ACCOUNT.equals(instruction.destinationAccount())) {
            throw new IllegalStateException("The liabilities core did not respond.");
        }
    }
}
