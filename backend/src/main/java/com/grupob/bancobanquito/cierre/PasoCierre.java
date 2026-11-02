package com.grupob.bancobanquito.cierre;

public enum PasoCierre {
    RECAUDACIONES(1),
    DEVENGO(2),
    MORA(3),
    CLASIFICACION(4),
    PROVISIONES(5),
    FOTOGRAFIA(6);

    private final int numeroEtapa;

    PasoCierre(int numeroEtapa) {
        this.numeroEtapa = numeroEtapa;
    }

    public int numeroEtapa() {
        return numeroEtapa;
    }
}
