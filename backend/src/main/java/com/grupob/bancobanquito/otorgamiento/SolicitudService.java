package com.grupob.bancobanquito.otorgamiento;

import java.util.List;

public interface SolicitudService {

    List<SolicitudBandejaResponse> listarBandejaAnalista(String estado);

    SolicitudDetalleResponse obtenerDetalleParaEvaluacion(Integer idSolicitud);
}
