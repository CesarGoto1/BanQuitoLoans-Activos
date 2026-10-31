package com.grupob.bancobanquito.contabilidad;

import java.util.List;

public interface ServiceContabilidad {

    boolean registrarAsiento(MovimientoContable movimiento);

    int registrarAsientos(List<MovimientoContable> movimientos);
}
