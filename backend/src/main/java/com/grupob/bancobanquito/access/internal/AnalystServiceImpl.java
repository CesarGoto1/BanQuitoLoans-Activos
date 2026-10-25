package com.grupob.bancobanquito.access.internal;

import com.grupob.bancobanquito.access.AnalystService;
import com.grupob.bancobanquito.access.AnalystSession;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AnalystServiceImpl implements AnalystService {

    private final AnalystRepository analystRepository;

    AnalystServiceImpl(AnalystRepository analystRepository) {
        this.analystRepository = analystRepository;
    }

    @Override
    public AnalystSession login(String nationalId) {
        return analystRepository.findByNationalId(nationalId)
                .map(this::session)
                .orElseThrow(() -> new AccessException(HttpStatus.NOT_FOUND,
                        "No existe un analista con la cédula " + nationalId + "."));
    }

    @Override
    public Optional<AnalystSession> findAnalyst(Integer analystId) {
        return analystRepository.findById(analystId).map(this::session);
    }

    private AnalystSession session(Analyst analyst) {
        return new AnalystSession(analyst.getId(), analyst.getNationalId(), analyst.getName());
    }
}
