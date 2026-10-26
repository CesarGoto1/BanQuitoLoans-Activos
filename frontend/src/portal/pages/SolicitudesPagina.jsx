import { useState } from 'react'
import BandejaPagina from './BandejaPagina.jsx'
import DetalleSolicitudPagina from './DetalleSolicitudPagina.jsx'

export default function SolicitudesPagina() {
    const [idSolicitud, setIdSolicitud] = useState(null)

    if (idSolicitud) {
        return <DetalleSolicitudPagina idSolicitud={idSolicitud} onVolver={() => setIdSolicitud(null)} />
    }
    return <BandejaPagina onSeleccionarSolicitud={setIdSolicitud} />
}