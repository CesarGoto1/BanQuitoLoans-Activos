package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrestamoRepository extends JpaRepository<Prestamo, Integer> {

    boolean existsByIdSolicitud(Integer idSolicitud);

    Optional<Prestamo> findByIdSolicitud(Integer idSolicitud);

    Optional<Prestamo> findByNumeroOperacion(String numeroOperacion);

    List<Prestamo> findByPortfolioStatusNot(EstadoCartera portfolioStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.numeroOperacion = :numeroOperacion")
    Optional<Prestamo> findParaActualizar(@Param("numeroOperacion") String numeroOperacion);

    Page<Prestamo> findByCedulaCliente(String cedulaCliente, Pageable paginacion);

    Page<Prestamo> findByCedulaClienteAndPortfolioStatus(String cedulaCliente, EstadoCartera portfolioStatus,
                                                          Pageable paginacion);
}
