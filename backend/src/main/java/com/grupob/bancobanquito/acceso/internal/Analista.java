package com.grupob.bancobanquito.acceso.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "analistas")
public class Analista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAnalista")
    private Integer id;

    @Column(name = "cedulaAnalista")
    @NotNull
    @Size(max = 10)
    private String cedula;

    @Column(name = "nombreAnalista")
    @NotNull
    @Size(max = 100)
    private String nombre;

    @Column(name = "usuarioCreacion")
    @NotNull
    @Size(max = 50)
    private String createdBy;

    @Column(name = "fechaCreacion")
    @NotNull
    private LocalDateTime createdAt;

    @Column(name = "usuarioModificacion")
    @Size(max = 50)
    private String lastModifiedBy;

    @Column(name = "fechaModificacion")
    private LocalDateTime lastModifiedAt;

    public Analista() {
    }

    public Analista(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
