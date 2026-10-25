package com.grupob.bancobanquito.access;

import java.util.Optional;

public interface AnalystService {

    AnalystSession login(String nationalId);

    Optional<AnalystSession> findAnalyst(Integer analystId);

    Optional<AnalystSession> findByNationalId(String nationalId);
}
