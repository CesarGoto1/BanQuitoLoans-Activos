package com.grupob.bancobanquito.loans.internal.repositories;
import com.grupob.bancobanquito.loans.internal.entities.*;
import com.grupob.bancobanquito.loans.internal.repositories.*;
import com.grupob.bancobanquito.loans.internal.services.*;
import com.grupob.bancobanquito.loans.internal.controllers.*;
import com.grupob.bancobanquito.loans.internal.exceptions.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanOperationRepository extends JpaRepository<LoanOperation, Integer> {

    List<LoanOperation> findByCedulaClienteOrderByFechaDesembolsoDescIdOperacionDesc(String cedulaCliente);

    Optional<LoanOperation> findByNumeroOperacionAndCedulaCliente(String numeroOperacion, String cedulaCliente);
}
















