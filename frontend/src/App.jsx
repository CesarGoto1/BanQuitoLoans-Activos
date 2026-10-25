import { useState } from 'react';
import MenuPortal from './portal/componentes/MenuPortal.jsx';
import BandejaPagina from './portal/paginas/BandejaPagina.jsx';
import DetalleSolicitudPagina from './portal/paginas/DetalleSolicitudPagina.jsx';

export default function App() {
  const [solicitudSeleccionadaId, setSolicitudSeleccionadaId] = useState(null);

  return (
    <>
      <MenuPortal onIrABandeja={() => setSolicitudSeleccionadaId(null)} />
      <main>
        {solicitudSeleccionadaId ? (
          <DetalleSolicitudPagina
            idSolicitud={solicitudSeleccionadaId}
            onVolver={() => setSolicitudSeleccionadaId(null)}
          />
        ) : (
          <BandejaPagina
            onSeleccionarSolicitud={(id) => setSolicitudSeleccionadaId(id)}
          />
        )}
      </main>
    </>
  );
}
