package com.grupob.bancobanquito.cobranza;

import java.time.LocalDate;
import java.util.List;

public interface ServicioFotografiaCartera {

    int tomarFotografiaDiaria(LocalDate fechaNegocio);

    List<ResumenFotoCartera> consultarFotosPorFecha(LocalDate fechaCorte);

    List<ResumenFotoCartera> consultarFotosEntreFechas(LocalDate fechaInicio, LocalDate fechaFin);

    List<ResumenTramoCartera> consultarTramos();

    List<ResumenTramoCartera> consultarTramosVigentes(LocalDate fechaCorte);
}
