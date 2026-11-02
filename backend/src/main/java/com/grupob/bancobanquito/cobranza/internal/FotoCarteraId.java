package com.grupob.bancobanquito.cobranza.internal;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class FotoCarteraId implements Serializable {

    private Integer idOperacion;
    private LocalDate fechaFoto;

    public FotoCarteraId() {
    }

    public FotoCarteraId(Integer idOperacion, LocalDate fechaFoto) {
        this.idOperacion = idOperacion;
        this.fechaFoto = fechaFoto;
    }

    public Integer getIdOperacion() {
        return idOperacion;
    }

    public void setIdOperacion(Integer idOperacion) {
        this.idOperacion = idOperacion;
    }

    public LocalDate getFechaFoto() {
        return fechaFoto;
    }

    public void setFechaFoto(LocalDate fechaFoto) {
        this.fechaFoto = fechaFoto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FotoCarteraId that = (FotoCarteraId) o;
        return Objects.equals(idOperacion, that.idOperacion) &&
               Objects.equals(fechaFoto, that.fechaFoto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idOperacion, fechaFoto);
    }
}
