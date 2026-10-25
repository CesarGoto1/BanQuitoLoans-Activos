package com.grupob.bancobanquito.origination;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanDecisionResult(
        @JsonProperty("idSolicitud") Integer applicationId,
        @JsonProperty("estadoSolicitud") String status,
        @JsonProperty("teaAsignada") BigDecimal annualEffectiveRate,
        @JsonProperty("cuotaEvaluacionDefinitiva") BigDecimal finalInstallment,
        @JsonProperty("fechaResolucion") LocalDate resolutionDate,
        @JsonProperty("motivoRechazo") String rejectionReason,
        @JsonProperty("idAnalista") Integer analystId) {
}
