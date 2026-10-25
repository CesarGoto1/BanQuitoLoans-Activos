package com.grupob.bancobanquito.origination.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface CustomerLoanRepository extends JpaRepository<CustomerLoan, Integer> {

    boolean existsByCustomerIdAndDaysPastDueGreaterThan(Integer customerId, Integer daysPastDue);
}
