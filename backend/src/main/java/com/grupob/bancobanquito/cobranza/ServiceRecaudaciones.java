package com.grupob.bancobanquito.cobranza;

import java.time.LocalDate;

public interface ServiceRecaudaciones {

    ResultadoRecaudacion procesar(String nombreArchivo, byte[] contenido);

    int procesarCarpeta(LocalDate fechaNegocio);
}
