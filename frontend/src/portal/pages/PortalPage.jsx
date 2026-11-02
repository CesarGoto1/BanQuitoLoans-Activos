import { useState } from 'react'
import { MorosidadPagina, ComposicionCarteraPagina, ProvisionesPagina, SaludCarteraPagina, RollRatePagina } from './ReportesCarteraPagina.jsx'
import InicioDesembolsoPage from './InicioDesembolsoPage.jsx'
import ConsultaOperacionPagina from './ConsultaOperacionPagina.jsx'
import CierreDiaPagina from './CierreDiaPagina.jsx'
import AbonoExtraordinarioPagina from './AbonoExtraordinarioPagina.jsx'
import CancelacionAnticipadaPagina from './CancelacionAnticipadaPagina.jsx'
import CierreDiaPagina from './CierreDiaPagina.jsx'
import CuadreContablePagina from './CuadreContablePagina.jsx'
import InicioSesionPage from './InicioSesionPage.jsx'
import SolicitudesPagina from './SolicitudesPagina.jsx'
import { clearAnalystSession, getAnalystSession } from '../services/sesionAnalista.js'

const SECTIONS = [
  { id: 'applications', label: 'Solicitudes', page: SolicitudesPagina },
  { id: 'disbursement', label: 'Desembolsar', page: InicioDesembolsoPage },
  { id: 'inquiry', label: 'Consultar operaciones', page: ConsultaOperacionPagina },
  { id: 'closing', label: 'Cierre de día', page: CierreDiaPagina },]
  { id: 'prepayment', label: 'Abono extraordinario', page: AbonoExtraordinarioPagina },
  { id: 'payoff', label: 'Cancelación anticipada', page: CancelacionAnticipadaPagina },
  { id: 'closing', label: 'Cierre de día', page: CierreDiaPagina },
  { id: 'accounting-balance', label: 'Cuadre contable', page: CuadreContablePagina },
  { id: 'provisions', label: 'Provisiones', page: ProvisionesPagina },
  { id: 'delinquency', label: 'Morosidad', page: MorosidadPagina },
  { id: 'composition', label: 'Composición de cartera', page: ComposicionCarteraPagina },
  { id: 'health', label: 'Salud de cartera', page: SaludCarteraPagina },
  { id: 'roll-rate', label: 'Roll-Rate', page: RollRatePagina },
]

export default function PortalPage() {
  const [analyst, setAnalyst] = useState(getAnalystSession)
  const [sectionId, setSectionId] = useState(SECTIONS[0].id)

  function logout() {
    clearAnalystSession()
    setAnalyst(null)
  }

  if (!analyst) {
    return <InicioSesionPage onLogin={setAnalyst} />
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
          <span>{analyst.nombre}</span>
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
