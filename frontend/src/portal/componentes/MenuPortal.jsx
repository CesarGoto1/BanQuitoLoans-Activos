export default function MenuPortal({ onIrABandeja }) {
  return (
    <header className="bq-cabecera">
      <div className="bq-fila">
        <span className="bq-marca">
          BanQuito · Portal de Gestión
        </span>
        <span className="bq-estado bq-estado-info">Analista</span>
      </div>
      <div className="bq-acciones">
        <button
          type="button"
          className="bq-btn bq-btn-secundario bq-btn-sm"
          onClick={onIrABandeja}
        >
          Bandeja de Solicitudes
        </button>
      </div>
    </header>
  );
}
