package com.grupob.bancobanquito.origination.internal.repositories;
import com.grupob.bancobanquito.origination.internal.entities.*;
import com.grupob.bancobanquito.origination.internal.repositories.*;
import com.grupob.bancobanquito.origination.internal.services.*;
import com.grupob.bancobanquito.origination.internal.controllers.*;
import com.grupob.bancobanquito.origination.internal.exceptions.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Integer> {

    List<LoanRequest> findByIdClienteOrderByFechaHoraRegistroDesc(Integer idCliente);

    List<LoanRequest> findByIdClienteAndEstadoSolicitudOrderByFechaHoraRegistroDesc(
            Integer idCliente, LoanRequestStatus estadoSolicitud);
}
















