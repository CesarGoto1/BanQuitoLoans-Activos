package com.grupob.bancobanquito.pasivos;

import java.util.List;
import java.util.Optional;

public interface ServicePasivos {

    Optional<Cliente> findCustomer(Integer idCliente);

    Optional<Cliente> findCustomerByNationalId(String cedula);

    List<Cuenta> findActiveAccounts(Integer idCliente);

    void creditDisbursement(InstruccionDesembolso instruction);
}
