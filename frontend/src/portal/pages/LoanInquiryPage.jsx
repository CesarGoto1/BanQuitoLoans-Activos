import { useState } from 'react'
import { findInstallments, findLoan, findLoansByCustomer } from '../services/loanInquiryApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const number = new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const percentage = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2 })
const amortizationMethods = { FRENCH: 'Francés', GERMAN: 'Alemán' }

const portfolioStatuses = {
  CURRENT: { label: 'Por vencer', className: 'bq-estado-info' },
  PAST_DUE: { label: 'Vencida', className: 'bq-estado-aviso' },
  IN_LITIGATION: { label: 'En litigio', className: 'bq-estado-peligro' },
  WRITTEN_OFF: { label: 'Castigada', className: 'bq-estado-peligro' },
  PAID_OFF: { label: 'Cancelada', className: 'bq-estado-neutro' },
}

const installmentStatuses = {
  PENDING: { label: 'Pendiente', className: 'bq-estado-neutro' },
  PARTIALLY_PAID: { label: 'Pago parcial', className: 'bq-estado-aviso' },
  PAID: { label: 'Pagada', className: 'bq-estado-exito' },
}

const searchModes = {
  LOAN_NUMBER: { label: 'Número de operación', placeholder: 'OP-20261031-000001' },
  NATIONAL_ID: { label: 'Cédula del cliente', placeholder: '1712345678' },
}

function formatDate(iso) {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

function sum(installments, field) {
  return installments.reduce((total, installment) => total + Number(installment[field]), 0)
}

export default function LoanInquiryPage() {
  const [searchMode, setSearchMode] = useState('LOAN_NUMBER')
  const [searchText, setSearchText] = useState('')
  const [portfolioStatus, setPortfolioStatus] = useState('')
  const [error, setError] = useState('')
  const [searching, setSearching] = useState(false)
  const [customerLoans, setCustomerLoans] = useState(null)
  const [loan, setLoan] = useState(null)
  const [installments, setInstallments] = useState(null)

  async function run(action) {
    setSearching(true)
    setError('')
    try {
      await action()
    } catch (e) {
      setError(e.message)
    } finally {
      setSearching(false)
    }
  }

  async function openLoan(loanNumber) {
    const [foundLoan, schedule] = await Promise.all([findLoan(loanNumber), findInstallments(loanNumber, 0)])
    setLoan(foundLoan)
    setInstallments(schedule)
  }

  async function searchCustomer(page) {
    setCustomerLoans(await findLoansByCustomer(searchText.trim(), portfolioStatus, page))
  }

  function search(event) {
    event.preventDefault()
    setLoan(null)
    setInstallments(null)
    setCustomerLoans(null)
    if (searchMode === 'NATIONAL_ID' && !/^\d{10}$/.test(searchText.trim())) {
      setError('La cédula debe tener 10 dígitos.')
      return
    }
    run(() => (searchMode === 'LOAN_NUMBER' ? openLoan(searchText.trim()) : searchCustomer(0)))
  }

  function changeSearchMode(mode) {
    setSearchMode(mode)
    setSearchText('')
    setError('')
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Consulta de operación</h1>
          <p className="bq-pequeno bq-texto-2">Busca un préstamo por su número de operación o por la cédula del cliente.</p>
        </div>
      </div>

      <form className="bq-tarjeta bq-formulario" onSubmit={search}>
        <div className="bq-segmentado" role="group" aria-label="Buscar por">
          {Object.entries(searchModes).map(([mode, { label }]) => (
            <button key={mode} type="button" aria-pressed={searchMode === mode} onClick={() => changeSearchMode(mode)}>
              {label}
            </button>
          ))}
        </div>

        <div className="bq-fila">
          <div className="bq-campo">
            <label className="bq-label" htmlFor="searchText">
              {searchModes[searchMode].label} <span className="bq-obligatorio">*</span>
            </label>
            <input
              className="bq-input bq-buscador bq-mono"
              id="searchText"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              placeholder={searchModes[searchMode].placeholder}
              inputMode={searchMode === 'NATIONAL_ID' ? 'numeric' : undefined}
              required
            />
          </div>

          {searchMode === 'NATIONAL_ID' ? (
            <div className="bq-campo">
              <label className="bq-label" htmlFor="portfolioStatus">Estado de cartera</label>
              <select
                className="bq-select"
                id="portfolioStatus"
                value={portfolioStatus}
                onChange={(e) => setPortfolioStatus(e.target.value)}
              >
                <option value="">Todos</option>
                {Object.entries(portfolioStatuses).map(([status, { label }]) => (
                  <option key={status} value={status}>{label}</option>
                ))}
              </select>
            </div>
          ) : null}
        </div>

        {error ? (
          <div className="bq-alerta bq-alerta-peligro" role="alert">
            <strong>No se pudo consultar</strong>
            <p>{error}</p>
          </div>
        ) : null}

        <div className="bq-acciones">
          <button className="bq-btn bq-btn-primario" type="submit" disabled={searching || searchText.trim() === ''}>
            {searching ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
      </form>

      {customerLoans && !loan ? (
        <CustomerLoans
          loans={customerLoans}
          onOpen={(loanNumber) => run(() => openLoan(loanNumber))}
          onPage={(page) => run(() => searchCustomer(page))}
        />
      ) : null}

      {loan ? (
        <>
          {customerLoans ? (
            <div className="bq-acciones">
              <button className="bq-btn bq-btn-texto" type="button" onClick={() => setLoan(null)}>
                ← Volver a los préstamos del cliente
              </button>
            </div>
          ) : null}
          <LoanDetail loan={loan} />
          <Schedule
            installments={installments}
            onPage={(page) => run(async () => setInstallments(await findInstallments(loan.loanNumber, page)))}
          />
        </>
      ) : null}
    </main>
  )
}

function CustomerLoans({ loans, onOpen, onPage }) {
  if (loans.content.length === 0) {
    return (
      <div className="bq-alerta bq-alerta-info" role="status">
        <strong>Sin préstamos</strong>
        <p>El cliente no tiene préstamos con ese criterio.</p>
      </div>
    )
  }

  return (
    <div className="bq-tarjeta">
      <span className="bq-etiqueta-seccion">Préstamos del cliente</span>
      <div className="bq-tabla-contenedor">
        <table className="bq-tabla">
          <thead>
            <tr>
              <th>Operación</th>
              <th>Desembolso</th>
              <th className="bq-num">Monto aprobado</th>
              <th className="bq-num">Saldo de capital</th>
              <th className="bq-num">Días de mora</th>
              <th>Estado</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {loans.content.map((loan) => (
              <tr key={loan.loanNumber}>
                <td className="bq-mono">{loan.loanNumber}</td>
                <td>{formatDate(loan.disbursementDate)}</td>
                <td className="bq-num">{number.format(loan.approvedAmount)}</td>
                <td className="bq-num">{number.format(loan.principalBalance)}</td>
                <td className="bq-num">{loan.daysPastDue}</td>
                <td><PortfolioStatusBadge status={loan.portfolioStatus} /></td>
                <td>
                  <button className="bq-btn bq-btn-secundario bq-btn-sm" type="button" onClick={() => onOpen(loan.loanNumber)}>
                    Ver detalle
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={loans.page} onPage={onPage} />
    </div>
  )
}

function LoanDetail({ loan }) {
  return (
    <div className="bq-tarjeta">
      <div className="bq-encabezado-tarjeta">
        <span className="bq-titulo-tarjeta">
          Operación <span className="bq-mono">{loan.loanNumber}</span>
        </span>
        <PortfolioStatusBadge status={loan.portfolioStatus} />
      </div>

      <span className="bq-etiqueta-seccion">Situación actual</span>
      <div className="bq-fila">
        <Field label="Saldo de capital" value={dollars.format(loan.principalBalance)} amount highlighted />
        <Field label="Días de mora" value={loan.daysPastDue} />
        <Field label="Cédula del cliente" value={loan.customerNationalId} mono />
      </div>

      <span className="bq-etiqueta-seccion">Condiciones del préstamo</span>
      <div className="bq-fila">
        <Field label="Monto aprobado" value={dollars.format(loan.approvedAmount)} amount />
        <Field label="Tasa efectiva anual (TEA)" value={percentage.format(loan.effectiveAnnualRate)} />
        <Field label="Plazo" value={`${loan.termMonths} meses`} />
        <Field label="Sistema de amortización" value={amortizationMethods[loan.amortizationMethod]} />
        <Field label="Fecha de desembolso" value={formatDate(loan.disbursementDate)} />
        <Field label="Solicitud" value={loan.applicationId} />
      </div>
    </div>
  )
}

function Schedule({ installments, onPage }) {
  const rows = installments.content
  const complete = installments.page.totalPages <= 1

  return (
    <div className="bq-tarjeta">
      <span className="bq-etiqueta-seccion">Tabla de amortización vigente</span>
      <div className="bq-tabla-contenedor">
        <table className="bq-tabla">
          <thead>
            <tr>
              <th className="bq-num">N.º</th>
              <th>Vencimiento</th>
              <th className="bq-num">Capital</th>
              <th className="bq-num">Interés</th>
              <th className="bq-num">Seguro</th>
              <th className="bq-num">Cuota total</th>
              <th className="bq-num">Saldo</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((installment) => (
              <tr key={installment.installmentNumber}>
                <td className="bq-num">{installment.installmentNumber}</td>
                <td>{formatDate(installment.dueDate)}</td>
                <td className="bq-num">{number.format(installment.principalAmount)}</td>
                <td className="bq-num">{number.format(installment.interestAmount)}</td>
                <td className="bq-num">{number.format(installment.insuranceAmount)}</td>
                <td className="bq-num">{number.format(installment.totalAmount)}</td>
                <td className="bq-num">{number.format(installment.remainingPrincipalBalance)}</td>
                <td>
                  <span className={`bq-estado ${installmentStatuses[installment.status]?.className ?? 'bq-estado-neutro'}`}>
                    {installmentStatuses[installment.status]?.label ?? installment.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
          {complete ? (
            <tfoot>
              <tr>
                <td></td>
                <td>Total</td>
                <td className="bq-num">{number.format(sum(rows, 'principalAmount'))}</td>
                <td className="bq-num">{number.format(sum(rows, 'interestAmount'))}</td>
                <td className="bq-num">{number.format(sum(rows, 'insuranceAmount'))}</td>
                <td className="bq-num">{number.format(sum(rows, 'totalAmount'))}</td>
                <td></td>
                <td></td>
              </tr>
            </tfoot>
          ) : null}
        </table>
      </div>
      <Pagination page={installments.page} onPage={onPage} />
    </div>
  )
}

function Pagination({ page, onPage }) {
  if (page.totalPages <= 1) {
    return null
  }

  return (
    <div className="bq-acciones">
      <span className="bq-pequeno bq-texto-2">
        Página {page.number + 1} de {page.totalPages} · {page.totalElements} registros
      </span>
      <button className="bq-btn bq-btn-secundario bq-btn-sm" type="button" disabled={page.number === 0} onClick={() => onPage(page.number - 1)}>
        Anterior
      </button>
      <button
        className="bq-btn bq-btn-secundario bq-btn-sm"
        type="button"
        disabled={page.number + 1 >= page.totalPages}
        onClick={() => onPage(page.number + 1)}
      >
        Siguiente
      </button>
    </div>
  )
}

function PortfolioStatusBadge({ status }) {
  const { label, className } = portfolioStatuses[status] ?? { label: status, className: 'bq-estado-neutro' }
  return <span className={`bq-estado ${className}`}>{label}</span>
}

function Field({ label, value, amount = false, highlighted = false, mono = false }) {
  const className = [amount ? 'bq-monto' : null, mono ? 'bq-mono' : null].filter(Boolean).join(' ')
  return (
    <div className="bq-campo">
      <span className="bq-label">{label}</span>
      <span className={className || undefined} style={highlighted ? { fontWeight: 700 } : undefined}>
        {value}
      </span>
    </div>
  )
}
