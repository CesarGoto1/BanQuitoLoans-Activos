package com.grupob.bancobanquito.prestamos;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultaPrestamoService {

    List<DatosOperacionCartera> buscarDatosPorIds(Collection<Integer> identificadores);

    ResumenPrestamo buscarPorNumeroOperacion(String numeroOperacion);

    Page<ResumenPrestamo> buscarPorCliente(String cedulaCliente, EstadoCartera estadoCartera, Pageable paginacion);

    Page<ResumenCuota> buscarTablaAmortizacion(String numeroOperacion, Pageable paginacion);

    HistorialOperacion buscarHistorial(String numeroOperacion);
}
