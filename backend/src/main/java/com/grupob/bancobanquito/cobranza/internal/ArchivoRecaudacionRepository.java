package com.grupob.bancobanquito.cobranza.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface ArchivoRecaudacionRepository extends JpaRepository<ArchivoRecaudacion, Integer> {

    boolean existsByNombreArchivoAndHashSha256AndEstadoArchivo(String nombre, String hash, String estado);
}
