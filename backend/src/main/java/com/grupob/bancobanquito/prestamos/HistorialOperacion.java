package com.grupob.bancobanquito.prestamos;

import java.util.List;

public record HistorialOperacion(
        String numeroOperacion,
        List<PagoAplicado> pagos,
        List<CargoMora> cargosMora,
        List<MovimientoEstado> movimientosEstado) {
}
