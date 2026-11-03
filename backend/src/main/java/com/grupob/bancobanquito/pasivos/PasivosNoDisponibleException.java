package com.grupob.bancobanquito.pasivos;

public class PasivosNoDisponibleException extends RuntimeException {

    public PasivosNoDisponibleException(Throwable cause) {
        super("El servicio no está disponible en este momento; intenta de nuevo en unos minutos.", cause);
    }
}
