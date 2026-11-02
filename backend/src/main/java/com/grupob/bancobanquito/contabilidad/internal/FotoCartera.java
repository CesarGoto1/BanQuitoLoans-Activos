package com.grupob.bancobanquito.contabilidad.internal;

import com.grupob.bancobanquito.prestamos.internal.Prestamo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fotosCartera")
@IdClass(FotoCarteraId.class)
class FotoCartera {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idOperacion")
    private Prestamo prestamo;

    @Id
    @Column(name = "fechaFoto")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idTramo")
    private TramoCartera tramo;

    @Column(name = "estadoCartera")
    private String status;

    @Column(name = "diasMora")
    private Integer daysPastDue;

    @Column(name = "saldoCapital")
    private BigDecimal principalBalance;

    @Column(name = "valorProvision")
    private BigDecimal provision;

    @Column(name = "interesDevengadoAcumulado")
    private BigDecimal accruedInterest;

    @Column(name = "interesMoraAcumulado")
    private BigDecimal accruedLateInterest;

    @Column(name = "usuarioCreacion")
    private String createdBy;

    @Column(name = "fechaCreacion")
    private LocalDateTime createdAt;

    static FotoCartera create(Prestamo prestamo, LocalDate date, TramoCartera tramo,
                              BigDecimal provision) {
        FotoCartera foto = new FotoCartera();
        foto.prestamo = prestamo;
        foto.date = date;
        foto.tramo = tramo;
        foto.status = prestamo.getPortfolioStatus().name();
        foto.daysPastDue = prestamo.getDaysPastDue();
        foto.principalBalance = prestamo.getPrincipalBalance();
        foto.provision = provision;
        foto.accruedInterest = BigDecimal.ZERO;
        foto.accruedLateInterest = BigDecimal.ZERO;
        foto.createdBy = "proceso-cierre";
        foto.createdAt = LocalDateTime.now();
        return foto;
    }

    BigDecimal getProvision() {
        return provision;
    }

    String getStatus() {
        return status;
    }
}
