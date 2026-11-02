package com.grupob.bancobanquito.cobranza.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RegistroRecaudacionRepository extends JpaRepository<RegistroRecaudacion, Integer> {

    boolean existsByUuidTransaccionAndEstadoRegistro(UUID uuid, String estado);
}
