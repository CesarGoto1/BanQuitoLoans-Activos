package com.grupob.bancobanquito.amortization.internal;

interface SimulacionService {
    SimulacionResponse simular(SimulacionRequest solicitud);
    ParametrosSimulacionResponse consultarParametros();
}
