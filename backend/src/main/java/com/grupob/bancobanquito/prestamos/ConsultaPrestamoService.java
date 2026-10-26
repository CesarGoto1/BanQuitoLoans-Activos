package com.grupob.bancobanquito.prestamos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultaPrestamoService {

    ResumenPrestamo buscarPorNumeroOperacion(String numeroOperacion);

    Page<ResumenPrestamo> buscarPorCliente(String cedulaCliente, EstadoCartera estadoCartera, Pageable paginacion);

    Page<ResumenCuota> buscarTablaAmortizacion(String numeroOperacion, Pageable paginacion);
}
