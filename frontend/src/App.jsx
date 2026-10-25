import LoanManagementPage from './online-banking/pages/LoanManagementPage.jsx'
import PortalPage from './portal/pages/PortalPage.jsx'

export default function App() {
  if (window.location.pathname.startsWith('/banca-web')) {
    return <LoanManagementPage />
  }
  return <PortalPage />
}
