export default function ApplicationRequestPage({ customer }) {
  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Solicitar crédito</h1>
          <p className="bq-pequeno bq-texto-2">Registra una nueva solicitud de crédito.</p>
        </div>
      </div>
      <div className="bq-alerta bq-alerta-info" role="status">
        <strong>En construcción</strong>
        <span>Cliente {customer.customerId} · {customer.nationalId}</span>
      </div>
    </main>
  )
}
