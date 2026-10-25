package com.grupob.bancobanquito.loans.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface LoanQueryRepository extends JpaRepository<Operation, Integer> {

    List<Operation> findByClientIdOrderByDisbursementDateDescOperationNumberAsc(Integer clientId);
}
