import GestionPrestamosPage from './banca-web/pages/GestionPrestamosPage.jsx'
import PortalPage from './portal/pages/PortalPage.jsx'

export default function App() {
  if (window.location.pathname.startsWith('/banca-web')) {
    return <GestionPrestamosPage />
  }
  return <PortalPage />
}
