import { useEffect, useState } from 'react'
import { findCustomerLoans, findLoanInstallments } from '../services/prestamosClienteApi.js'

const dollars = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })
const percentage = new Intl.NumberFormat('es-EC', { style: 'percent', minimumFractionDigits: 2 })
const amortizationMethods = { FRANCES: 'Francés', ALEMAN: 'Alemán' }

const portfolioStatuses = {
  POR_VENCER: { label: 'Por vencer', className: 'bq-estado-info' },
  VENCIDA: { label: 'Vencida', className: 'bq-estado-aviso' },
  EN_LITIGIO: { label: 'En litigio', className: 'bq-estado-peligro' },
  CASTIGADA: { label: 'Castigada', className: 'bq-estado-peligro' },
  CANCELADA: { label: 'Cancelada', className: 'bq-estado-neutro' },
}

const installmentStatuses = {
  PENDIENTE: { label: 'Pendiente', className: 'bq-estado-neutro' },
  PAGO_PARCIAL: { label: 'Pago parcial', className: 'bq-estado-aviso' },
  PAGADA: { label: 'Pagada', className: 'bq-estado-exito' },
}

function formatDate(iso) {
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

export default function MisOperacionesPage({ customer }) {
  const [page, setPage] = useState(0)
  const [loans, setLoans] = useState(null)
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let current = true
    setError('')
    findCustomerLoans(page)
      .then((data) => current && setLoans(data))
      .catch((e) => current && setError(e.message))
    return () => {
      current = false
    }
  }, [customer.cedulaCliente, page])

  if (selected) {
    return <LoanDetail loan={selected} onBack={() => setSelected(null)} />
  }

  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Mis operaciones</h1>
          <p className="bq-pequeno bq-texto-2">Revisa tus créditos, su saldo de capital y el estado de cartera.</p>
        </div>
      </div>

      {error ? (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>No se pudo cargar la información</strong>
          <span>{error}</span>
        </div>
      ) : null}

      {!loans && !error ? <p className="bq-texto-2">Cargando tus operaciones…</p> : null}

      {loans && loans.content.length === 0 ? (
        <div className="bq-alerta bq-alerta-info" role="status">
          <strong>No tienes operaciones</strong>
          <span>Cuando se desembolse un crédito aprobado aparecerá aquí.</span>
        </div>
      ) : null}

      {loans && loans.content.length > 0 ? (
        <>
          <div className="bq-rejilla">
            {loans.content.map((loan) => (
              <article className="bq-tarjeta" key={loan.numeroOperacion}>
                <div className="bq-encabezado-tarjeta">
                  <span className="bq-titulo-tarjeta bq-mono">{loan.numeroOperacion}</span>
                  <PortfolioStatusBadge status={loan.estadoCartera} />
                </div>
                <div className="bq-fila">
                  <div className="bq-campo">
                    <span className="bq-label">Saldo de capital</span>
                    <strong>{dollars.format(loan.saldoCapital)}</strong>
                  </div>
                  <div className="bq-campo">
                    <span className="bq-label">Monto aprobado</span>
                    <span>{dollars.format(loan.montoAprobado)}</span>
                  </div>
                  <div className="bq-campo">
                    <span className="bq-label">Días de mora</span>
                    <span>{loan.diasMora}</span>
                  </div>
                </div>
                <div className="bq-acciones">
                  <button className="bq-btn bq-btn-secundario bq-btn-sm" type="button" onClick={() => setSelected(loan)}>
                    Ver tabla de amortización
                  </button>
                </div>
              </article>
            ))}
          </div>
          <Pagination page={loans.page} onPage={setPage} />
        </>
      ) : null}
    </main>
  )
}

function LoanDetail({ loan, onBack }) {
  const [page, setPage] = useState(0)
  const [installments, setInstallments] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let current = true
    setError('')
    findLoanInstallments(loan.numeroOperacion, page)
      .then((data) => current && setInstallments(data))
      .catch((e) => current && setError(e.message))
    return () => {
      current = false
    }
  }, [loan.numeroOperacion, page])

  const next = installments?.content.find((installment) => installment.estadoCuota !== 'PAGADA')

  return (
    <main className="bq-pagina">
      <div className="bq-acciones">
        <button className="bq-btn bq-btn-texto" type="button" onClick={onBack}>
          ← Volver a mis operaciones
        </button>
      </div>

      <div className="bq-tarjeta">
        <div className="bq-encabezado-tarjeta">
          <span className="bq-titulo-tarjeta">
            Operación <span className="bq-mono">{loan.numeroOperacion}</span>
          </span>
          <PortfolioStatusBadge status={loan.estadoCartera} />
        </div>
        <div className="bq-fila">
          <div className="bq-campo">
            <span className="bq-label">Saldo de capital</span>
            <strong>{dollars.format(loan.saldoCapital)}</strong>
          </div>
          <div className="bq-campo">
            <span className="bq-label">Próximo vencimiento</span>
            <span>{next ? `Cuota ${next.numeroCuota} · ${formatDate(next.fechaVencimiento)}` : '—'}</span>
          </div>
          <div className="bq-campo">
            <span className="bq-label">Valor a pagar</span>
            <strong>{next ? dollars.format(next.valorCuota) : '—'}</strong>
          </div>
          <div className="bq-campo">
            <span className="bq-label">Días de mora</span>
            <span>{loan.diasMora}</span>
          </div>
        </div>
        <div className="bq-fila">
          <div className="bq-campo">
            <span className="bq-label">Monto aprobado</span>
            <span>{dollars.format(loan.montoAprobado)}</span>
          </div>
          <div className="bq-campo">
            <span className="bq-label">Tasa efectiva anual</span>
            <span>{percentage.format(loan.tasaEfectivaAnual)}</span>
          </div>
          <div className="bq-campo">
            <span className="bq-label">Plazo</span>
            <span>{loan.plazoMeses} meses · {amortizationMethods[loan.sistemaAmortizacion]}</span>
          </div>
          <div className="bq-campo">
            <span className="bq-label">Desembolso</span>
            <span>{formatDate(loan.fechaDesembolso)}</span>
          </div>
        </div>
      </div>

      {error ? (
        <div className="bq-alerta bq-alerta-peligro" role="alert">
          <strong>No se pudo cargar la tabla</strong>
          <span>{error}</span>
        </div>
      ) : null}

      {installments ? (
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
                {installments.content.map((installment) => (
                  <tr key={installment.numeroCuota}>
                    <td className="bq-num">{installment.numeroCuota}</td>
                    <td>{formatDate(installment.fechaVencimiento)}</td>
                    <td className="bq-num">{dollars.format(installment.valorCapital)}</td>
                    <td className="bq-num">{dollars.format(installment.valorInteres)}</td>
                    <td className="bq-num">{dollars.format(installment.valorSeguro)}</td>
                    <td className="bq-num">{dollars.format(installment.valorCuota)}</td>
                    <td className="bq-num">{dollars.format(installment.saldoCapitalPendiente)}</td>
                    <td>
                      <span className={`bq-estado ${installmentStatuses[installment.estadoCuota]?.className ?? 'bq-estado-neutro'}`}>
                        {installmentStatuses[installment.estadoCuota]?.label ?? installment.estadoCuota}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={installments.page} onPage={setPage} />
        </div>
      ) : null}
    </main>
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
