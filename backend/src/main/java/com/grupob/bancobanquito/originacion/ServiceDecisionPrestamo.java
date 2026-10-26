package com.grupob.bancobanquito.originacion;

import java.math.BigDecimal;

public interface ServiceDecisionPrestamo {

    ResultadoDecisionPrestamo approveApplication(Integer idSolicitud, String identification, BigDecimal teaAsignada);

    ResultadoDecisionPrestamo rejectApplication(Integer idSolicitud, String identification, String motivoRechazo);
}
