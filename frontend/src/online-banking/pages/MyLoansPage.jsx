export default function MyLoansPage({ customer }) {
  return (
    <main className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Mis operaciones</h1>
          <p className="bq-pequeno bq-texto-2">Consulta tus créditos vigentes y su tabla de amortización.</p>
        </div>
      </div>
      <div className="bq-alerta bq-alerta-info" role="status">
        <strong>En construcción</strong>
        <span>Cliente {customer.customerId} · {customer.nationalId}</span>
      </div>
    </main>
  )
}
