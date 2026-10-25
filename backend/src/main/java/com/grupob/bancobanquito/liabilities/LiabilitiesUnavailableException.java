package com.grupob.bancobanquito.liabilities;

public class LiabilitiesUnavailableException extends RuntimeException {

    public LiabilitiesUnavailableException(Throwable cause) {
        super("El Core de Pasivos no está disponible; intenta de nuevo en unos minutos.", cause);
    }
}
