package com.grupob.bancobanquito.loans.internal;

import com.grupob.bancobanquito.loans.PortfolioStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class PortfolioStatusConverter implements AttributeConverter<PortfolioStatus, String> {

    @Override
    public String convertToDatabaseColumn(PortfolioStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CURRENT -> "POR_VENCER";
            case PAST_DUE -> "VENCIDA";
            case IN_LITIGATION -> "EN_LITIGIO";
            case WRITTEN_OFF -> "CASTIGADA";
            case PAID_OFF -> "CANCELADA";
        };
    }

    @Override
    public PortfolioStatus convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "POR_VENCER" -> PortfolioStatus.CURRENT;
            case "VENCIDA" -> PortfolioStatus.PAST_DUE;
            case "EN_LITIGIO" -> PortfolioStatus.IN_LITIGATION;
            case "CASTIGADA" -> PortfolioStatus.WRITTEN_OFF;
            case "CANCELADA" -> PortfolioStatus.PAID_OFF;
            default -> throw new IllegalArgumentException("Unknown portfolio status: " + value);
        };
    }
}
