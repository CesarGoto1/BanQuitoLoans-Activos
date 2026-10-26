package com.grupob.bancobanquito.acceso.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface AnalistaRepository extends JpaRepository<Analista, Integer> {

    Optional<Analista> findByCedula(String cedula);
}
