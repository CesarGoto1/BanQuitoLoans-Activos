INSERT INTO "analistas" ("idAnalista", "cedulaAnalista", "nombreAnalista", "usuarioCreacion")
VALUES (9001, '1700000001', 'Analista de pruebas', 'pruebas')
ON CONFLICT ("idAnalista") DO NOTHING;

INSERT INTO "solicitudes" ("idSolicitud", "idCliente", "sistemaAmortizacion", "idAnalista", "montoSolicitado",
    "plazoMeses", "destinoCredito", "ingresosMensuales", "gastosMensuales", "fechaSolicitud", "fechaHoraRegistro",
    "cuotaEvaluacionReferencial", "teaAsignada", "cuotaEvaluacionDefinitiva", "estadoSolicitud", "fechaResolucion",
    "usuarioCreacion")
VALUES
 (9101, 101, 'FRANCES', 9001, 10000.00, 12, 'SALUD', 3500.00, 1200.00,
    DATE '2026-10-31', TIMESTAMP '2026-10-31 09:00', 911.44, 0.1677, 911.44, 'APROBADA', DATE '2026-10-31', 'pruebas'),
 (9102, 102, 'FRANCES', 9001, 300.00, 1, 'OTRO', 2000.00, 500.00,
    DATE '2026-04-30', TIMESTAMP '2026-04-30 09:00', 312.00, 0.1677, 312.00, 'APROBADA', DATE '2026-04-30', 'pruebas'),
 (9103, 103, 'FRANCES', 9001, 500.00, 6, 'OTRO', 2000.00, 500.00,
    DATE '2026-04-30', TIMESTAMP '2026-04-30 09:00', 90.00, 0.1677, 90.00, 'APROBADA', DATE '2026-04-30', 'pruebas'),
 (9104, 104, 'FRANCES', 9001, 200.00, 2, 'OTRO', 2000.00, 500.00,
    DATE '2026-10-31', TIMESTAMP '2026-10-31 09:00', 105.00, 0.1677, 105.00, 'APROBADA', DATE '2026-10-31', 'pruebas')
ON CONFLICT ("idSolicitud") DO NOTHING;

INSERT INTO "operaciones" ("idOperacion", "idSolicitud", "idCliente", "cedulaCliente", "sistemaAmortizacion",
    "numeroOperacion", "montoAprobado", "teaAsignada", "plazoMeses", "seguroMensual", "contribucionSolca", "montoNeto",
    "cuentaDestino", "fechaDesembolso", "estadoInstruccionDesembolso", "estadoCartera", "saldoCapital", "diasMora",
    "usuarioCreacion")
VALUES
 (9101, 9101, 101, '1710034065', 'FRANCES', 'OP-20261031-009101', 10000.00, 0.1677, 12, 6.00, 50.00, 9950.00,
    '2200000001', DATE '2026-10-31', 'ENTREGADA', 'VENCIDA', 10000.00, 10, 'pruebas'),
 (9102, 9102, 102, '0912345675', 'FRANCES', 'OP-20260430-009102', 300.00, 0.1677, 1, 2.00, 1.50, 298.50,
    '2200000002', DATE '2026-04-30', 'ENTREGADA', 'CASTIGADA', 300.00, 193, 'pruebas'),
 (9103, 9103, 103, '1102233445', 'FRANCES', 'OP-20260430-009103', 500.00, 0.1677, 6, 0.30, 2.50, 497.50,
    '2200000003', DATE '2026-04-30', 'ENTREGADA', 'CANCELADA', 0.00, 0, 'pruebas'),
 (9104, 9104, 104, '1102233446', 'FRANCES', 'OP-20261031-009104', 200.00, 0.1677, 2, 1.00, 1.00, 199.00,
    '2200000004', DATE '2026-10-31', 'ENTREGADA', 'POR_VENCER', 200.00, 0, 'pruebas')
ON CONFLICT ("idOperacion") DO NOTHING;

INSERT INTO "cuotas" ("idOperacion", "numeroCuota", "fechaVencimiento", "valorCapital", "valorInteres", "valorSeguro",
    "saldoCapitalPosterior", "moraAcumulada", "usuarioCreacion")
VALUES
 (9101, 1, DATE '2026-11-30', 775.41, 130.03, 6.00, 9224.59, 3.3300, 'pruebas'),
 (9101, 2, DATE '2026-12-31', 785.49, 119.95, 6.00, 8439.10, 0, 'pruebas'),
 (9101, 3, DATE '2027-01-31', 795.70, 109.74, 6.00, 7643.40, 0, 'pruebas'),
 (9101, 4, DATE '2027-02-28', 806.05, 99.39, 6.00, 6837.35, 0, 'pruebas'),
 (9101, 5, DATE '2027-03-31', 816.53, 88.91, 6.00, 6020.82, 0, 'pruebas'),
 (9101, 6, DATE '2027-04-30', 827.15, 78.29, 6.00, 5193.67, 0, 'pruebas'),
 (9101, 7, DATE '2027-05-31', 837.90, 67.54, 6.00, 4355.77, 0, 'pruebas'),
 (9101, 8, DATE '2027-06-30', 848.80, 56.64, 6.00, 3506.97, 0, 'pruebas'),
 (9101, 9, DATE '2027-07-31', 859.84, 45.60, 6.00, 2647.13, 0, 'pruebas'),
 (9101, 10, DATE '2027-08-31', 871.02, 34.42, 6.00, 1776.11, 0, 'pruebas'),
 (9101, 11, DATE '2027-09-30', 882.34, 23.10, 6.00, 893.77, 0, 'pruebas'),
 (9101, 12, DATE '2027-10-31', 893.77, 11.62, 6.00, 0.00, 0, 'pruebas'),
 (9102, 1, DATE '2026-05-31', 300.00, 10.00, 2.00, 0.00, 25.0000, 'pruebas'),
 (9104, 1, DATE '2026-11-30', 100.00, 5.00, 1.00, 100.00, 0, 'pruebas'),
 (9104, 2, DATE '2026-12-31', 100.00, 2.00, 1.00, 0.00, 0, 'pruebas')
ON CONFLICT ("idOperacion", "numeroCuota") DO NOTHING;

INSERT INTO "archivosRecaudacion" ("idArchivo", "fechaCarga", "nombreArchivo", "hashSha256", "fechaProcesoArchivo",
    "fechaHoraGeneracion", "totalRegistros", "montoTotalArchivo", "estadoArchivo", "usuarioCreacion")
VALUES (9101, DATE '2026-10-31', 'REC_20261210_001', '0000000000000000000000000000000000000000000000000000000000000000', DATE '2026-12-10',
    TIMESTAMP '2026-12-10 18:00', 10, 5000.00, 'PROCESADO', 'pruebas')
ON CONFLICT ("idArchivo") DO NOTHING;

INSERT INTO "registrosRecaudacion" ("idRegistro", "idArchivo", "numeroSecuencial", "uuidTransaccion", "estadoRegistro",
    "usuarioCreacion")
SELECT 9100 + n, 9101, n, ('00000000-0000-4000-8000-' || lpad((9100 + n)::text, 12, '0'))::uuid, 'APLICADO', 'pruebas'
FROM generate_series(1, 10) AS n
ON CONFLICT ("idRegistro") DO NOTHING;
