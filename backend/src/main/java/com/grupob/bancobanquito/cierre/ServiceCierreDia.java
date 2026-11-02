package com.grupob.bancobanquito.cierre;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServiceCierreDia {

    ResultadoCierreDia ejecutarCierre(LocalDate fechaNegocio, String usuario);

    ResultadoCierreDia consultarCierre(LocalDate fechaNegocio);

    Page<ResultadoCierreDia> listarCierres(Pageable paginacion);
}
