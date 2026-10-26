package com.grupob.bancobanquito.prestamos.internal;

import com.grupob.bancobanquito.prestamos.EstadoCartera;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PrestamoRepository extends JpaRepository<Prestamo, Integer> {

    boolean existsByIdSolicitud(Integer idSolicitud);

    Optional<Prestamo> findByIdSolicitud(Integer idSolicitud);

    Optional<Prestamo> findByNumeroOperacion(String numeroOperacion);

    Page<Prestamo> findByCedulaCliente(String cedulaCliente, Pageable paginacion);

    Page<Prestamo> findByCedulaClienteAndPortfolioStatus(String cedulaCliente, EstadoCartera portfolioStatus,
                                                          Pageable paginacion);
}
