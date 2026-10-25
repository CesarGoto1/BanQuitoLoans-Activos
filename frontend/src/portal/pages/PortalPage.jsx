import { useState } from 'react'
import DisbursementStartPage from './DisbursementStartPage.jsx'
import LoanInquiryPage from './LoanInquiryPage.jsx'
import LoginPage from './LoginPage.jsx'
import { clearAnalystSession, getAnalystSession } from '../services/analystSession.js'

const SECTIONS = [
  { id: 'disbursement', label: 'Desembolsar', page: DisbursementStartPage },
  { id: 'inquiry', label: 'Consultar operaciones', page: LoanInquiryPage },
]

export default function PortalPage() {
  const [analyst, setAnalyst] = useState(getAnalystSession)
  const [sectionId, setSectionId] = useState(SECTIONS[0].id)

  function logout() {
    clearAnalystSession()
    setAnalyst(null)
  }

  if (!analyst) {
    return <LoginPage onLogin={setAnalyst} />
  }

  const Page = SECTIONS.find((section) => section.id === sectionId).page

  return (
    <>
      <header className="bq-cabecera">
        <div className="bq-marca">
          <img className="bq-logo" src="/logo-bq.png" alt="BanQuito" />
          <span className="bq-marca-nombre">BanQuito</span>
          <span className="bq-marca-app">Portal del analista</span>
        </div>
        <div className="bq-cabecera-usuario">
          <span>{analyst.name}</span>
          <button type="button" className="bq-btn bq-btn-secundario bq-btn-sm" onClick={logout}>
            Cerrar sesión
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
      <Page />
    </>
  )
}
