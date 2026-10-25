INSERT INTO "analistas" ("idAnalista", "cedulaAnalista", "nombreAnalista", "usuarioCreacion")
VALUES (9001, '1700000001', 'Analista de pruebas', 'pruebas')
ON CONFLICT ("idAnalista") DO NOTHING;

INSERT INTO "solicitudes" ("idSolicitud", "idCliente", "sistemaAmortizacion", "idAnalista", "montoSolicitado",
    "plazoMeses", "destinoCredito", "ingresosMensuales", "gastosMensuales", "fechaSolicitud", "fechaHoraRegistro",
    "cuotaEvaluacionReferencial", "teaAsignada", "cuotaEvaluacionDefinitiva", "estadoSolicitud", "fechaResolucion",
    "usuarioCreacion")
VALUES
 (9001, 1, 'FRANCES', 9001, 10000.00, 2, 'SALUD', 3000.00, 1000.00,
    DATE '2026-10-31', TIMESTAMP '2026-10-31 09:00', 5000.00, 0.1550, 5000.00, 'APROBADA', DATE '2026-10-31', 'pruebas'),
 (9002, 2, 'ALEMAN', 9001, 5000.00, 12, 'EDUCACION', 2000.00, 800.00,
    DATE '2026-10-31', TIMESTAMP '2026-10-31 09:10', 480.00, 0.1550, 480.00, 'APROBADA', DATE '2026-10-31', 'pruebas'),
 (9003, 3, 'FRANCES', 9001, 30000.00, 60, 'BIENES_DURADEROS', 6000.00, 1500.00,
    DATE '2026-10-31', TIMESTAMP '2026-10-31 09:20', 730.00, 0.1550, 730.00, 'APROBADA', DATE '2026-10-31', 'pruebas')
ON CONFLICT ("idSolicitud") DO NOTHING;
