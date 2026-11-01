package com.grupob.bancobanquito.pagos.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DetallePagoRepository extends JpaRepository<DetallePago, Integer> {

    List<DetallePago> findByIdPagoOrderByIdCuotaAsc(Integer idPago);
}
