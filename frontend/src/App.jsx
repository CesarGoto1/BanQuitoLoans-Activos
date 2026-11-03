import GestionPrestamosPage from './banca-web/pages/GestionPrestamosPage.jsx'
import PortalPage from './portal/pages/PortalPage.jsx'
import InicioPage from './sitio/pages/InicioPage.jsx'

export default function App() {
  const ruta = window.location.pathname
  if (ruta.startsWith('/banca-web')) {
    document.title = 'BanQuito · Gestión de préstamos'
    return <GestionPrestamosPage />
  }
  if (ruta.startsWith('/portal')) {
    document.title = 'BanQuito · Portal del analista'
    return <PortalPage />
  }
  document.title = 'BanQuito · Tu crédito, cuota a cuota'
  return <InicioPage />
}
