package com.grupob.bancobanquito.originacion.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface PrestamoClienteRepository extends JpaRepository<PrestamoCliente, Integer> {

    boolean existsByIdClienteAndDaysPastDueGreaterThan(Integer idCliente, Integer daysPastDue);
}
