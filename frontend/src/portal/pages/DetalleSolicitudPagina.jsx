import { useState, useEffect } from 'react';
import { obtenerDetalleSolicitud } from '../services/prestamosApi.js';
import PanelDecisionPrestamo from '../components/PanelDecisionPrestamo.jsx';

const SISTEMAS = { FRANCES: 'Francés', ALEMAN: 'Alemán' };

const DESTINOS = {
  BIENES_DURADEROS: 'Bienes duraderos',
  SALUD: 'Salud',
  EDUCACION: 'Educación',
  VIAJE: 'Viaje',
  OTRO: 'Otro',
};

const ESTADOS = { REGISTRADA: 'Registrada', APROBADA: 'Aprobada', RECHAZADA: 'Rechazada' };

function formatearFechaHora(valor) {
  if (!valor) return '';
  const fecha = new Date(valor);
  return isNaN(fecha.getTime()) ? valor : fecha.toLocaleString('es-EC', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

export default function DetalleSolicitudPagina({ idSolicitud, onVolver }) {
  const [detalle, setDetalle] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function cargarDetalle() {
      setCargando(true);
      setError(null);
      try {
        const datos = await obtenerDetalleSolicitud(idSolicitud);
        setDetalle(datos);
      } catch (err) {
        setError(err.message || 'Error al cargar el detalle de la solicitud');
      } finally {
        setCargando(false);
      }
    }
    if (idSolicitud) {
      cargarDetalle();
    }
  }, [idSolicitud]);

  const formatearDinero = (monto) => {
    if (monto === null || monto === undefined) return '$ 0,00';
    return new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' }).format(monto);
  };

  const formatearPorcentaje = (valor) => {
    if (valor === null || valor === undefined) return '-';
    return `${new Intl.NumberFormat('es-EC', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(valor)} %`;
  };

  const formatearFecha = (fechaStr) => {
    if (!fechaStr) return '-';
    const [anio, mes, dia] = String(fechaStr).slice(0, 10).split('-');
    return dia && mes && anio ? `${dia}/${mes}/${anio}` : fechaStr;
  };

  const obtenerClaseEstado = (estado) => {
    switch (estado) {
      case 'APROBADA':
        return 'bq-estado bq-estado-exito';
      case 'RECHAZADA':
        return 'bq-estado bq-estado-peligro';
      case 'REGISTRADA':
      default:
        return 'bq-estado bq-estado-info';
    }
  };

  return (
    <div className="bq-pagina">
      <div className="bq-acciones">
        <button
          type="button"
          className="bq-btn bq-btn-texto"
          onClick={onVolver}
        >
          ← Volver a la bandeja
        </button>
      </div>

      {error && (
        <div className="bq-alerta bq-alerta-peligro">
          <strong>Error de consulta</strong>
          <p>{error}</p>
        </div>
      )}

      {cargando ? (
        <div className="bq-tarjeta">
          <p className="bq-texto-2">Cargando evaluación de la solicitud #{idSolicitud}...</p>
        </div>
      ) : detalle ? (
        <>
          <div className="bq-titulo-pagina">
            <div>
              <span className="bq-etiqueta-seccion">Evaluación de Solicitud</span>
              <h1>Solicitud #{detalle.idSolicitud}</h1>
              <p className="bq-pequeno bq-texto-2">
                Cliente: {detalle.nombreCliente ?? 'No disponible'} · Cédula: <span className="bq-mono">{detalle.cedulaCliente ?? '—'}</span> · Fecha de solicitud: {formatearFecha(detalle.fechaSolicitud)}
              </p>
            </div>
            <span className={obtenerClaseEstado(detalle.estadoSolicitud)}>
              {ESTADOS[detalle.estadoSolicitud] ?? detalle.estadoSolicitud}
            </span>
          </div>

          <div className="bq-tarjeta">
            <div className="bq-titulo-pagina">
              <div>
                <span className="bq-etiqueta-seccion">Condiciones del Crédito</span>
                <h2 className="bq-monto">{formatearDinero(detalle.montoSolicitado)}</h2>
                <p className="bq-pequeno bq-texto-2">Monto Solicitado</p>
              </div>
              <div>
                <span className="bq-etiqueta-seccion">Cuota Referencial Proyectada</span>
                <h3 className="bq-monto">{formatearDinero(detalle.cuotaEvaluacionReferencial)}</h3>
              </div>
            </div>
            <div className="bq-tabla-contenedor">
              <table className="bq-tabla">
                <thead>
                  <tr>
                    <th>Plazo</th>
                    <th>Sistema de Amortización</th>
                    <th>Destino del Crédito</th>
                    <th>Fecha / Hora Registro</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>{detalle.plazoMeses} meses</td>
                    <td>{SISTEMAS[detalle.sistemaAmortizacion] ?? detalle.sistemaAmortizacion}</td>
                    <td>{DESTINOS[detalle.destinoCredito] ?? detalle.destinoCredito}</td>
                    <td>{formatearFechaHora(detalle.fechaHoraRegistro)}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div className="bq-tarjeta">
            <span className="bq-etiqueta-seccion">Situación Financiera Declarada</span>
            <div className="bq-tabla-contenedor">
              <table className="bq-tabla">
                <thead>
                  <tr>
                    <th className="bq-num">Ingresos Mensuales</th>
                    <th className="bq-num">Gastos Mensuales</th>
                    <th className="bq-num">Ingreso Remanente Mensual</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="bq-num bq-monto">{formatearDinero(detalle.ingresosMensuales)}</td>
                    <td className="bq-num bq-monto">{formatearDinero(detalle.gastosMensuales)}</td>
                    <td className="bq-num bq-monto">{formatearDinero(detalle.ingresoRemanente)}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div className="bq-tarjeta">
            <span className="bq-etiqueta-seccion">Análisis referencial de capacidad de pago</span>

            {detalle.capacidadPagoValida ? (
              <div className="bq-alerta bq-alerta-exito">
                <strong>Evaluación referencial favorable</strong>
                <p>{detalle.mensajeCapacidadPago}</p>
              </div>
            ) : (
              <div className="bq-alerta bq-alerta-peligro">
                <strong>Riesgo Financiero: Excede Límite Regulatorio</strong>
                <p>{detalle.mensajeCapacidadPago}</p>
              </div>
            )}

            <div className="bq-tabla-contenedor">
              <table className="bq-tabla">
                <thead>
                  <tr>
                    <th className="bq-num">Impacto sobre Remanente</th>
                    <th className="bq-num">Límite máximo permitido</th>
                    <th className="bq-num">Cuota Máxima Admisible</th>
                    <th>Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="bq-num">{formatearPorcentaje(detalle.porcentajeImpactoCuota)}</td>
                    <td className="bq-num">{formatearPorcentaje(detalle.limiteImpactoMaximo)}</td>
                    <td className="bq-num bq-monto">{formatearDinero(detalle.cuotaMaximaAdmisible)}</td>
                    <td>
                      {detalle.capacidadPagoValida ? (
                        <span className="bq-estado bq-estado-exito">Cumple capacidad de pago</span>
                      ) : (
                        <span className="bq-estado bq-estado-peligro">No cumple capacidad de pago</span>
                      )}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <PanelDecisionPrestamo key={detalle.idSolicitud} application={detalle}
            onResolved={(result) => setDetalle((current) => ({ ...current, ...result }))} />
        </>
      ) : null}
    </div>
  );
}
