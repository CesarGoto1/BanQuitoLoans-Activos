package com.grupob.bancobanquito.acceso.internal;

import com.grupob.bancobanquito.acceso.ServiceAnalista;
import com.grupob.bancobanquito.acceso.SesionAnalista;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ServiceAnalistaImpl implements ServiceAnalista {

    @Override
    public Optional<SesionAnalista> findByNationalId(String cedula) {
        return analystRepository.findByCedula(cedula).map(this::session);
    }

    private final AnalistaRepository analystRepository;

    ServiceAnalistaImpl(AnalistaRepository analystRepository) {
        this.analystRepository = analystRepository;
    }

    @Override
    public SesionAnalista login(String cedula) {
        return analystRepository.findByCedula(cedula)
                .map(this::session)
                .orElseThrow(() -> new ExcepcionAcceso(HttpStatus.NOT_FOUND,
                        "No existe un analista con la cédula " + cedula + "."));
    }

    @Override
    public Optional<SesionAnalista> findAnalyst(Integer idAnalista) {
        return analystRepository.findById(idAnalista).map(this::session);
    }

    private SesionAnalista session(Analista analyst) {
        return new SesionAnalista(analyst.getId(), analyst.getCedula(), analyst.getNombre());
    }
}
