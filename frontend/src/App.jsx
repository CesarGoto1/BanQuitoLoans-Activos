import { useState } from 'react';
import MenuPortal from './portal/componentes/MenuPortal.jsx';
import BandejaPagina from './portal/paginas/BandejaPagina.jsx';
import DetalleSolicitudPagina from './portal/paginas/DetalleSolicitudPagina.jsx';
import AnalystSession from './portal/components/AnalystSession.jsx';
import { getAnalystIdentification, setAnalystIdentification } from './portal/servicios/prestamosApi.js';

export default function App() {
  const [solicitudSeleccionadaId, setSolicitudSeleccionadaId] = useState(null);
  const [analystIdentification, setAnalystIdentity] = useState(getAnalystIdentification);

  if (!analystIdentification) return <AnalystSession onContinue={setAnalystIdentity} />;

  return (
    <>
      <MenuPortal onIrABandeja={() => setSolicitudSeleccionadaId(null)} />
      <div className="bq-acciones">
        <button className="bq-btn bq-btn-texto" type="button" onClick={() => {
          setAnalystIdentification('');
          setAnalystIdentity('');
          setSolicitudSeleccionadaId(null);
        }}>Cambiar analista</button>
      </div>
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
