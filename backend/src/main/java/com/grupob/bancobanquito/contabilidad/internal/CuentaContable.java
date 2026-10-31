package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.contabilidad.CodigoCuentaContable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "cuentasContables")
class CuentaContable {

    @Id
    @Column(name = "idCuentaContable")
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigoCuenta")
    private CodigoCuentaContable codigoCuenta;

    @Column(name = "nombreCuenta")
    private String nombreCuenta;

    CuentaContable() {
    }

    CuentaContable(Integer id) {
        this.id = id;
    }

    Integer getId() {
        return id;
    }

    CodigoCuentaContable getCodigoCuenta() {
        return codigoCuenta;
    }

    String getNombreCuenta() {
        return nombreCuenta;
    }
}
