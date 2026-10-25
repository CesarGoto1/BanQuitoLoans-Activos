import { useState, useEffect } from 'react';
import { obtenerDetalleSolicitud } from '../servicios/prestamosApi.js';

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

  const formatearFecha = (fechaStr) => {
    if (!fechaStr) return '-';
    return fechaStr;
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
                Cliente ID: <span className="bq-mono">{detalle.idCliente}</span> · Fecha de solicitud: {formatearFecha(detalle.fechaSolicitud)}
              </p>
            </div>
            <span className={obtenerClaseEstado(detalle.estadoSolicitud)}>
              {detalle.estadoSolicitud}
            </span>
          </div>

          {/* Tarjeta 1: Condiciones del Crédito */}
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
                    <td>{detalle.sistemaAmortizacion}</td>
                    <td>{detalle.destinoCredito}</td>
                    <td>{detalle.fechaHoraRegistro}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* Tarjeta 2: Capacidad Financiera Declarada y Remanente */}
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

          {/* Tarjeta 3: Evaluación de Capacidad de Pago (RF-04, RN-09, RN-10) */}
          <div className="bq-tarjeta">
            <span className="bq-etiqueta-seccion">Análisis Regulatorio de Capacidad de Pago</span>

            {detalle.capacidadPagoValida ? (
              <div className="bq-alerta bq-alerta-exito">
                <strong>Capacidad de Pago Aprobada</strong>
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
                    <th className="bq-num">Límite Regulatorio (PAR-09)</th>
                    <th className="bq-num">Cuota Máxima Admisible</th>
                    <th>Resultado</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="bq-num">{detalle.porcentajeImpactoCuota} %</td>
                    <td className="bq-num">{detalle.limiteImpactoMaximo} %</td>
                    <td className="bq-num bq-monto">{formatearDinero(detalle.cuotaMaximaAdmisible)}</td>
                    <td>
                      {detalle.capacidadPagoValida ? (
                        <span className="bq-estado bq-estado-exito">Apta para Aprobación</span>
                      ) : (
                        <span className="bq-estado bq-estado-peligro">No Cumple Capacidad</span>
                      )}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* CONTENEDOR INTEGRACIÓN P6 (Micha) - RF-05: Aprobación y Rechazo          */}
          {/* ========================================================================= */}
          <div id="contenedor-resolucion-p6" className="bq-tarjeta">
            <div className="bq-titulo-pagina">
              <div>
                <span className="bq-etiqueta-seccion">Punto de Integración RF-05</span>
                <h3>Resolución del Analista (Aprobación / Rechazo)</h3>
                <p className="bq-pequeno bq-texto-2">
                  Espacio asignado a Micha (P6) para incrustar los formularios de asignación de TEA y motivo de rechazo.
                </p>
              </div>
            </div>
            <div className="bq-alerta bq-alerta-info">
              <strong>Contenedor listo para componentes de P6</strong>
              <p>
                Al integrar RF-05, aquí se presentará el formulario para aprobar la solicitud asignando la TEA definitiva o rechazarla con el motivo correspondiente.
              </p>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
