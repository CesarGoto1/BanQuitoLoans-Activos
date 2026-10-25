package com.grupob.bancobanquito.loans.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pagos")
class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idOperacion", nullable = false)
    private Operation operation;

    @Column(name = "origenPago", nullable = false)
    private String origin;

    @Column(name = "fechaPago", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "montoRecibido")
    private BigDecimal receivedAmount;

    @Column(name = "excedenteDevuelto", nullable = false)
    private BigDecimal returnedExcess;

    @OneToMany(mappedBy = "payment", fetch = FetchType.LAZY)
    private List<PaymentDetail> details = new ArrayList<>();

    LocalDate getPaymentDate() { return paymentDate; }
    String getOrigin() { return origin; }
    BigDecimal getReceivedAmount() { return receivedAmount; }
    BigDecimal getReturnedExcess() { return returnedExcess; }
    List<PaymentDetail> getDetails() { return details; }
}
