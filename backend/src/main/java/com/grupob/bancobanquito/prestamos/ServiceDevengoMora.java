package com.grupob.bancobanquito.prestamos;

import java.time.LocalDate;

public interface ServiceDevengoMora {

    int executeAccrual(LocalDate businessDate);

    int executeAccrualAdjustments(LocalDate businessDate);

    int executeDefaultInterest(LocalDate businessDate);
}
