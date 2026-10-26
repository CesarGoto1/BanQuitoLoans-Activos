package com.grupob.bancobanquito.originacion;

import java.util.List;

public interface ServiceBandejaSolicitud {

    List<SolicitudBandejaResponse> listarBandejaAnalista(String estado);

    SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud);
}
