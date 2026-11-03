package com.grupob.bancobanquito.cobranza.internal;

import com.grupob.bancobanquito.cobranza.ArchivoRecaudacionSolicitado;
import com.grupob.bancobanquito.cobranza.ServiceSolicitudArchivoRecaudacion;
import com.grupob.bancobanquito.pasivos.ArchivoRecaudacionGenerado;
import com.grupob.bancobanquito.pasivos.ServicePasivos;
import com.grupob.bancobanquito.parametros.ServiceFechaNegocio;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
class ServiceSolicitudArchivoRecaudacionImpl implements ServiceSolicitudArchivoRecaudacion {

    private final ServicePasivos servicioPasivos;
    private final ServiceFechaNegocio servicioFechaNegocio;

    ServiceSolicitudArchivoRecaudacionImpl(ServicePasivos servicioPasivos, ServiceFechaNegocio servicioFechaNegocio) {
        this.servicioPasivos = servicioPasivos;
        this.servicioFechaNegocio = servicioFechaNegocio;
    }

    @Override
    public ArchivoRecaudacionSolicitado solicitarArchivoDeLaFechaVigente() {
        LocalDate fechaProceso = servicioFechaNegocio.getCurrentBusinessDate();
        ArchivoRecaudacionGenerado archivo = servicioPasivos.generarArchivoRecaudacion(fechaProceso);
        return new ArchivoRecaudacionSolicitado(fechaProceso, archivo.nombreArchivo(), archivo.cantidadCobros(),
                archivo.montoTotal());
    }
}
