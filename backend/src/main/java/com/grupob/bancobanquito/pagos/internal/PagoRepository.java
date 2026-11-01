package com.grupob.bancobanquito.pagos.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PagoRepository extends JpaRepository<Pago, Integer> {

    boolean existsByIdRegistro(Integer idRegistro);

    Optional<Pago> findByIdRegistro(Integer idRegistro);
}
