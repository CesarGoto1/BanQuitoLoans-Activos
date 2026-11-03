import GestionPrestamosPage from './banca-web/pages/GestionPrestamosPage.jsx'
import PortalPage from './portal/pages/PortalPage.jsx'
import InicioPage from './sitio/pages/InicioPage.jsx'

export default function App() {
  const ruta = window.location.pathname
  if (ruta.startsWith('/banca-web')) {
    return <GestionPrestamosPage />
  }
  if (ruta.startsWith('/portal')) {
    return <PortalPage />
  }
  return <InicioPage />
}
