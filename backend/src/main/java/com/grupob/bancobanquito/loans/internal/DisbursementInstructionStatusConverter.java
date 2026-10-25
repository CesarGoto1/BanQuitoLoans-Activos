package com.grupob.bancobanquito.loans.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class DisbursementInstructionStatusConverter implements AttributeConverter<DisbursementInstructionStatus, String> {

    @Override
    public String convertToDatabaseColumn(DisbursementInstructionStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PENDING -> "PENDIENTE";
            case DELIVERED -> "ENTREGADA";
        };
    }

    @Override
    public DisbursementInstructionStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "PENDIENTE" -> DisbursementInstructionStatus.PENDING;
            case "ENTREGADA" -> DisbursementInstructionStatus.DELIVERED;
            default -> throw new IllegalArgumentException("Unknown disbursement instruction status: " + value);
        };
    }
}
