export default function BancaWebLayout({ pagina, setPage, children }) {
  const opciones = [
    ['solicitudes', 'Mis solicitudes'],
    ['operaciones', 'Mis operaciones'],
  ]

  return (
    <>
      <header className="bq-cabecera">
        <div className="bq-marca">
          <img className="bq-logo" src="/logo-bq.png" alt="" />
          <span className="bq-marca-nombre">BanQuito</span>
          <span className="bq-marca-app">Banca Web</span>
        </div>
        <div className="bq-cabecera-usuario">
          <span className="bq-fecha-negocio">
            Fecha de negocio <strong>{new Date().toLocaleDateString('es-EC')}</strong>
          </span>
          <span>Cliente · consulta segura</span>
        </div>
      </header>
      <nav className="bq-menu" aria-label="Navegación de Banca Web">
        {opciones.map(([clave, texto]) => (
          <button
            className={`bq-menu-item ${pagina === clave ? 'bq-activo' : ''}`}
            aria-current={pagina === clave ? 'page' : undefined}
            key={clave}
            onClick={() => setPage(clave)}
          >
            {texto}
          </button>
        ))}
      </nav>
      <main>{children}</main>
    </>
  )
}






