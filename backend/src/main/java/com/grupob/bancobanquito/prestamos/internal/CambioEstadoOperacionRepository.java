package com.grupob.bancobanquito.prestamos.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface CambioEstadoOperacionRepository extends JpaRepository<CambioEstadoOperacion, Integer> {
}
