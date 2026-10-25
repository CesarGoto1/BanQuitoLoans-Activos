import { useState, useEffect } from 'react';
import { obtenerBandejaSolicitudes } from '../servicios/prestamosApi.js';

export default function BandejaPagina({ onSeleccionarSolicitud }) {
  const [solicitudes, setSolicitudes] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  const cargarBandeja = async () => {
    setCargando(true);
    setError(null);
    try {
      const datos = await obtenerBandejaSolicitudes('REGISTRADA');
      setSolicitudes(datos);
    } catch (err) {
      setError(err.message || 'Error al recuperar la bandeja de solicitudes');
    } finally {
      setCargando(false);
    }
  };

  useEffect(() => {
    cargarBandeja();
  }, []);

  const formatearDinero = (monto) => {
    if (monto === null || monto === undefined) return '$ 0,00';
    return new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' }).format(monto);
  };

  const formatearFechaHora = (fechaHoraStr) => {
    if (!fechaHoraStr) return '-';
    const fecha = new Date(fechaHoraStr);
    return isNaN(fecha.getTime()) ? fechaHoraStr : fecha.toLocaleString('es-EC');
  };

  return (
    <div className="bq-pagina">
      <div className="bq-titulo-pagina">
        <div>
          <h1>Bandeja de Solicitudes de Crédito</h1>
          <p className="bq-pequeno bq-texto-2">
            Solicitudes pendientes ordenadas por antigüedad (RF-04)
          </p>
        </div>
        <div className="bq-acciones">
          <button
            type="button"
            className="bq-btn bq-btn-secundario bq-btn-sm"
            onClick={cargarBandeja}
            disabled={cargando}
          >
            Actualizar
          </button>
        </div>
      </div>

      {error && (
        <div className="bq-alerta bq-alerta-peligro">
          <strong>Error de comunicación</strong>
          <p>{error}</p>
        </div>
      )}

      {cargando ? (
        <div className="bq-tarjeta">
          <p className="bq-texto-2">Cargando solicitudes pendientes...</p>
        </div>
      ) : solicitudes.length === 0 ? (
        <div className="bq-tarjeta">
          <div className="bq-alerta bq-alerta-info">
            <strong>Bandeja al día</strong>
            <p>No existen solicitudes pendientes de evaluación en estado Registrada.</p>
          </div>
        </div>
      ) : (
        <div className="bq-tabla-contenedor">
          <table className="bq-tabla">
            <thead>
              <tr>
                <th># Solicitud</th>
                <th>Fecha de Registro</th>
                <th>Cód. Cliente</th>
                <th className="bq-num">Monto Solicitado</th>
                <th>Plazo</th>
                <th>Sistema</th>
                <th className="bq-num">Cuota Referencial</th>
                <th>Estado</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              {solicitudes.map((sol) => (
                <tr key={sol.idSolicitud}>
                  <td className="bq-mono">#{sol.idSolicitud}</td>
                  <td>{formatearFechaHora(sol.fechaHoraRegistro)}</td>
                  <td className="bq-mono">{sol.idCliente}</td>
                  <td className="bq-num bq-monto">{formatearDinero(sol.montoSolicitado)}</td>
                  <td>{sol.plazoMeses} meses</td>
                  <td>{sol.sistemaAmortizacion}</td>
                  <td className="bq-num">{formatearDinero(sol.cuotaEvaluacionReferencial)}</td>
                  <td>
                    <span className="bq-estado bq-estado-info">
                      {sol.estadoSolicitud}
                    </span>
                  </td>
                  <td>
                    <button
                      type="button"
                      className="bq-btn bq-btn-primario bq-btn-sm"
                      onClick={() => onSeleccionarSolicitud(sol.idSolicitud)}
                    >
                      Evaluar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
