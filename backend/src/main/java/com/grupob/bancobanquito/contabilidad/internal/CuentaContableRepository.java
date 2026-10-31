package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CuentaContableRepository extends JpaRepository<CuentaContable, Integer> {

    Optional<CuentaContable> findByCodigoCuenta(CodigoCuentaContable codigoCuenta);
}
