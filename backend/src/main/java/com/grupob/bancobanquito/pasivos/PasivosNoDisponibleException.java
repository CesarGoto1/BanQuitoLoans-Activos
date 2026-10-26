package com.grupob.bancobanquito.pasivos;

public class PasivosNoDisponibleException extends RuntimeException {

    public PasivosNoDisponibleException(Throwable cause) {
        super("El Core de Pasivos no está disponible; intenta de nuevo en unos minutos.", cause);
    }
}
