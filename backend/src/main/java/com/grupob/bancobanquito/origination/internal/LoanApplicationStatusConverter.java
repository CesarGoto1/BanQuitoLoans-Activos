package com.grupob.bancobanquito.origination.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class LoanApplicationStatusConverter implements AttributeConverter<LoanApplicationStatus, String> {

    @Override
    public String convertToDatabaseColumn(LoanApplicationStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case REGISTERED -> "REGISTRADA";
            case APPROVED -> "APROBADA";
            case REJECTED -> "RECHAZADA";
        };
    }

    @Override
    public LoanApplicationStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "REGISTRADA" -> LoanApplicationStatus.REGISTERED;
            case "APROBADA" -> LoanApplicationStatus.APPROVED;
            case "RECHAZADA" -> LoanApplicationStatus.REJECTED;
            default -> throw new IllegalArgumentException("Unknown loan application status: " + value);
        };
    }
}
