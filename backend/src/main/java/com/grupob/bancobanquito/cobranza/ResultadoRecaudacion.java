package com.grupob.bancobanquito.cobranza;

import java.util.List;

public record ResultadoRecaudacion(Integer idArchivo, String nombreArchivo, String estado,
                                   String codigoRechazo, List<ResultadoRegistroRecaudacion> registros) {

    public ResultadoRecaudacion {
        registros = List.copyOf(registros);
    }
}
