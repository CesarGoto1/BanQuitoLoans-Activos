package com.grupob.bancobanquito.cierre;

import java.time.LocalDateTime;

public record EjecucionEtapaCierre(
        int numeroEtapa,
        PasoCierre etapa,
        int numeroIntento,
        String estadoEtapa,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        Long duracionMilisegundos,
        int registrosProcesados,
        String mensajeError) {
}
