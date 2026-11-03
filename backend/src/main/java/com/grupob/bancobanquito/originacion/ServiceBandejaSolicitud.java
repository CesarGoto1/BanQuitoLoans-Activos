package com.grupob.bancobanquito.originacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceBandejaSolicitud {

    Page<SolicitudBandejaResponse> listarBandejaAnalista(String estado, Pageable paginacion);

    SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud);
}
