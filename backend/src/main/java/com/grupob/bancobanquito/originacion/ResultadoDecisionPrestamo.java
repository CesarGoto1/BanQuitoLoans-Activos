package com.grupob.bancobanquito.originacion;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultadoDecisionPrestamo(
        @JsonProperty("idSolicitud") Integer idSolicitud,
        @JsonProperty("estadoSolicitud") String status,
        @JsonProperty("teaAsignada") BigDecimal teaAsignada,
        @JsonProperty("cuotaEvaluacionDefinitiva") BigDecimal finalInstallment,
        @JsonProperty("fechaResolucion") LocalDate fechaResolucion,
        @JsonProperty("motivoRechazo") String motivoRechazo,
        @JsonProperty("idAnalista") Integer idAnalista) {
}
