package com.grupob.bancobanquito.cobranza;

public interface ServiceRecaudaciones {

    ResultadoRecaudacion procesar(String nombreArchivo, byte[] contenido);
}
