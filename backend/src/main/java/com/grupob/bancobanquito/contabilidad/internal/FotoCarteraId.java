package com.grupob.bancobanquito.contabilidad.internal;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

class FotoCarteraId implements Serializable {
    private Integer prestamo;
    private LocalDate date;

    FotoCarteraId() {
    }

    FotoCarteraId(Integer prestamo, LocalDate date) {
        this.prestamo = prestamo;
        this.date = date;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FotoCarteraId that)) {
            return false;
        }
        return Objects.equals(prestamo, that.prestamo) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prestamo, date);
    }
}
