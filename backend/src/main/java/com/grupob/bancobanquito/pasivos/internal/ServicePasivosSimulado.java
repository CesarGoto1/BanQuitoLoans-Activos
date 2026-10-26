package com.grupob.bancobanquito.pasivos.internal;

import com.grupob.bancobanquito.pasivos.Cuenta;
import com.grupob.bancobanquito.pasivos.Cliente;
import com.grupob.bancobanquito.pasivos.InstruccionDesembolso;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("simulated")
class ServicePasivosSimulado implements ServicePasivos {

    static final String FAILING_ACCOUNT = "9999999999";

    private static final Map<Integer, Cliente> CUSTOMERS = Map.of(
            1, new Cliente(1, "1710034065", "María Fernanda", "Andrade Lema"),
            2, new Cliente(2, "1712345675", "Luis Alberto", "Paredes Mora"),
            3, new Cliente(3, "1709876543", "Ana Lucía", "Gómez Vela"));

    private static final Map<Integer, List<Cuenta>> ACCOUNTS = Map.of(
            1, List.of(new Cuenta("2200000001", "AHORROS"), new Cuenta("3100000001", "CORRIENTE"),
                    new Cuenta(FAILING_ACCOUNT, "AHORROS")),
            2, List.of(new Cuenta("2200000002", "AHORROS")),
            3, List.of(new Cuenta("2200000003", "AHORROS")));

    @Override
    public Optional<Cliente> findCustomer(Integer idCliente) {
        return Optional.ofNullable(CUSTOMERS.get(idCliente));
    }

    @Override
    public Optional<Cliente> findCustomerByNationalId(String cedula) {
        return CUSTOMERS.values().stream().filter(customer -> customer.cedula().equals(cedula)).findFirst();
    }

    @Override
    public List<Cuenta> findActiveAccounts(Integer idCliente) {
        return ACCOUNTS.getOrDefault(idCliente, List.of());
    }

    @Override
    public void creditDisbursement(InstruccionDesembolso instruction) {
        if (FAILING_ACCOUNT.equals(instruction.cuentaDestino())) {
            throw new IllegalStateException("The liabilities core did not respond.");
        }
    }
}
