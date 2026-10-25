package com.grupob.bancobanquito.liabilities;

public record Customer(
        Integer customerId,
        String nationalId,
        String firstNames,
        String lastNames) {
}
