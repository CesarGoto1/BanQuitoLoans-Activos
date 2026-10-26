package com.grupob.bancobanquito.originacion.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
class ConversorEstadoSolicitud implements AttributeConverter<EstadoSolicitud, String> {

    @Override
    public String convertToDatabaseColumn(EstadoSolicitud status) {
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
    public EstadoSolicitud convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "REGISTRADA" -> EstadoSolicitud.REGISTERED;
            case "APROBADA" -> EstadoSolicitud.APPROVED;
            case "RECHAZADA" -> EstadoSolicitud.REJECTED;
            default -> throw new IllegalArgumentException("Unknown loan application status: " + value);
        };
    }
}
