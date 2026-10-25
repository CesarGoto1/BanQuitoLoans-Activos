import { useState } from 'react'
import ApplicationRequestPage from './ApplicationRequestPage.jsx'
import MyApplicationsPage from './MyApplicationsPage.jsx'
import MyLoansPage from './MyLoansPage.jsx'
import SimulationPage from './SimulationPage.jsx'
import { clearCustomerSession, receiveCustomerFromUrl } from '../services/customerSession.js'

const ONLINE_BANKING_URL = import.meta.env.VITE_URL_PASIVOS ?? 'http://localhost:5174'

const SECTIONS = [
  { id: 'simulation', label: 'Simular crédito', page: SimulationPage },
  { id: 'request', label: 'Solicitar crédito', page: ApplicationRequestPage },
  { id: 'applications', label: 'Mis solicitudes', page: MyApplicationsPage },
  { id: 'loans', label: 'Mis operaciones', page: MyLoansPage },
]

export default function LoanManagementPage() {
  const [customer] = useState(receiveCustomerFromUrl)
  const [sectionId, setSectionId] = useState(SECTIONS[0].id)

  function backToAccount() {
    clearCustomerSession()
    window.location.href = `${ONLINE_BANKING_URL}/banca/mi-cuenta`
  }

  if (!customer) {
    return (
      <main className="bq-pagina">
        <div className="bq-alerta bq-alerta-aviso" role="alert">
          <strong>Ingresa desde la Banca Web</strong>
          <span>Para gestionar tus préstamos, primero inicia sesión en la Banca Web.</span>
        </div>
        <div className="bq-acciones">
          <a className="bq-btn bq-btn-primario" href={ONLINE_BANKING_URL}>
            Ir a la Banca Web
          </a>
        </div>
      </main>
    )
  }

  const Page = SECTIONS.find((section) => section.id === sectionId).page

  return (
    <>
      <header className="bq-cabecera">
        <div className="bq-marca">
          <img className="bq-logo" src="/logo-bq.png" alt="BanQuito" />
          <span className="bq-marca-nombre">BanQuito</span>
          <span className="bq-marca-app">Gestión de préstamos</span>
        </div>
        <div className="bq-cabecera-usuario">
          <span>
            {customer.firstNames} {customer.lastNames}
          </span>
          <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" onClick={backToAccount}>
            Volver a mi cuenta
          </button>
        </div>
      </header>
      <nav className="bq-menu">
        {SECTIONS.map((section) => (
          <button
            key={section.id}
            type="button"
            className="bq-menu-item"
            aria-current={section.id === sectionId ? 'page' : undefined}
            onClick={() => setSectionId(section.id)}
          >
            {section.label}
          </button>
        ))}
      </nav>
      <Page customer={customer} />
    </>
  )
}
