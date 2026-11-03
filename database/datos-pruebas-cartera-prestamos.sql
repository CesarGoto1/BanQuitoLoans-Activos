-- =====================================================================
-- BanQuito · Datos de prueba de cartera (corePrestamos)
-- Ejecutar DESPUÉS de corePrestamos.sql y datos-iniciales-prestamos.sql,
-- y con datos-pruebas-cartera-pasivos.sql en corePasivos.
-- Fecha de negocio esperada: 2026-10-31 (cierre PENDIENTE).
--
-- Las operaciones entran en POR_VENCER con diasMora = 0, como si el
-- sistema aún no las hubiera clasificado. Al correr el cierre del
-- 2026-10-31, las etapas Devengo, Mora y Clasificación calculan el
-- interés del día, la mora y pasan cada operación a su tramo:
--   OP 9001 cliente 20  al día                  -> POR_VENCER  (1 %)
--   OP 9002 cliente 21  al día, 2 cuotas pagadas -> POR_VENCER  (1 %)
--   OP 9003 cliente 22  15 días                 -> VENCIDA     (5 %)
--   OP 9004 cliente 23  45 días, 2 cuotas       -> VENCIDA     (25 %)
--   OP 9005 cliente 24  11 días, pago parcial   -> VENCIDA     (5 %)
--   OP 9006 cliente 25  122 días                -> EN_LITIGIO  (60 %)
--   OP 9007 cliente 26  199 días                -> CASTIGADA   (100 %)
-- Solicitudes sin operación: 9008 REGISTRADA (cliente 27),
-- 9009 APROBADA sin desembolsar (cliente 28), 9010 RECHAZADA (cliente 29).
-- Analista de prueba: cédula 1799999901.
-- =====================================================================

BEGIN;

INSERT INTO "analistas" ("cedulaAnalista", "nombreAnalista", "usuarioCreacion") VALUES
 ('1799999901', 'Analista Demo Uno', 'demo-pruebas'),
 ('1799999902', 'Analista Demo Dos', 'demo-pruebas')
ON CONFLICT ("cedulaAnalista") DO NOTHING;

-- OP-20261001-009001 · cliente 20 (1721001400) · Al día, recién desembolsado (sin cuotas vencidas)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9001,20,'FRANCES',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),5000.00,12,'BIENES_DURADEROS',2500.00,900.00,'2026-09-28','2026-09-28 10:00',453.11,0.1550,453.11,'APROBADA','2026-09-30','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9001,20,'1721001400','FRANCES','OP-20261001-009001',5000.00,0.1550,12,3.00,25.00,4975.00,'2200000020','2026-10-01','ENTREGADA','POR_VENCER',5000.00,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),1,'2026-11-01',389.71,60.40,3.00,4610.29,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),2,'2026-12-01',394.41,55.70,3.00,4215.88,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),3,'2027-01-01',399.18,50.93,3.00,3816.70,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),4,'2027-02-01',404.00,46.11,3.00,3412.70,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),5,'2027-03-01',408.88,41.23,3.00,3003.82,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),6,'2027-04-01',413.82,36.29,3.00,2590.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),7,'2027-05-01',418.82,31.29,3.00,2171.18,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),8,'2027-06-01',423.88,26.23,3.00,1747.30,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),9,'2027-07-01',429.00,21.11,3.00,1318.30,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),10,'2027-08-01',434.18,15.93,3.00,884.12,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),11,'2027-09-01',439.43,10.68,3.00,444.69,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9001),12,'2027-10-01',444.69,5.37,3.00,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- OP-20260814-009002 · cliente 21 (1731001473) · Al día con 2 cuotas pagadas
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9002,21,'FRANCES',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),8000.00,24,'EDUCACION',3000.00,1100.00,'2026-08-11','2026-08-11 10:00',390.87,0.1600,392.54,'APROBADA','2026-08-13','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9002,21,'1731001473','FRANCES','OP-20260814-009002',8000.00,0.1600,24,4.80,40.00,7960.00,'2200000021','2026-08-14','ENTREGADA','POR_VENCER',7420.26,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),1,'2026-09-14',288.08,99.56,4.80,7711.92,4.80,99.56,288.08,'PAGADA','2026-09-14','demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),2,'2026-10-14',291.66,95.98,4.80,7420.26,4.80,95.98,291.66,'PAGADA','2026-10-14','demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),3,'2026-11-14',295.29,92.35,4.80,7124.97,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),4,'2026-12-14',298.97,88.67,4.80,6826.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),5,'2027-01-14',302.69,84.95,4.80,6523.31,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),6,'2027-02-14',306.46,81.18,4.80,6216.85,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),7,'2027-03-14',310.27,77.37,4.80,5906.58,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),8,'2027-04-14',314.13,73.51,4.80,5592.45,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),9,'2027-05-14',318.04,69.60,4.80,5274.41,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),10,'2027-06-14',322.00,65.64,4.80,4952.41,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),11,'2027-07-14',326.01,61.63,4.80,4626.40,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),12,'2027-08-14',330.06,57.58,4.80,4296.34,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),13,'2027-09-14',334.17,53.47,4.80,3962.17,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),14,'2027-10-14',338.33,49.31,4.80,3623.84,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),15,'2027-11-14',342.54,45.10,4.80,3281.30,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),16,'2027-12-14',346.80,40.84,4.80,2934.50,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),17,'2028-01-14',351.12,36.52,4.80,2583.38,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),18,'2028-02-14',355.49,32.15,4.80,2227.89,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),19,'2028-03-14',359.91,27.73,4.80,1867.98,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),20,'2028-04-14',364.39,23.25,4.80,1503.59,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),21,'2028-05-14',368.93,18.71,4.80,1134.66,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),22,'2028-06-14',373.52,14.12,4.80,761.14,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),23,'2028-07-14',378.17,9.47,4.80,382.97,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9002),24,'2028-08-14',382.97,4.77,4.80,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- OP-20260916-009003 · cliente 22 (1741001547) · 15 días de mora (tramo 2, VENCIDA 5 %)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9003,22,'ALEMAN',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),6000.00,18,'SALUD',2800.00,1000.00,'2026-09-13','2026-09-13 10:00',409.41,0.1550,409.41,'APROBADA','2026-09-15','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9003,22,'1741001547','ALEMAN','OP-20260916-009003',6000.00,0.1550,18,3.60,30.00,5970.00,'2200000022','2026-09-16','ENTREGADA','POR_VENCER',6000.00,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),1,'2026-10-16',333.33,72.48,3.60,5666.67,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),2,'2026-11-16',333.33,68.46,3.60,5333.34,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),3,'2026-12-16',333.33,64.43,3.60,5000.01,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),4,'2027-01-16',333.33,60.40,3.60,4666.68,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),5,'2027-02-16',333.33,56.38,3.60,4333.35,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),6,'2027-03-16',333.33,52.35,3.60,4000.02,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),7,'2027-04-16',333.33,48.32,3.60,3666.69,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),8,'2027-05-16',333.33,44.30,3.60,3333.36,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),9,'2027-06-16',333.33,40.27,3.60,3000.03,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),10,'2027-07-16',333.33,36.24,3.60,2666.70,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),11,'2027-08-16',333.33,32.22,3.60,2333.37,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),12,'2027-09-16',333.33,28.19,3.60,2000.04,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),13,'2027-10-16',333.33,24.16,3.60,1666.71,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),14,'2027-11-16',333.33,20.14,3.60,1333.38,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),15,'2027-12-16',333.33,16.11,3.60,1000.05,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),16,'2028-01-16',333.33,12.08,3.60,666.72,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),17,'2028-02-16',333.33,8.05,3.60,333.39,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9003),18,'2028-03-16',333.39,4.03,3.60,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- OP-20260816-009004 · cliente 23 (1751001619) · 45 días de mora, 2 cuotas vencidas (tramo 3, VENCIDA 25 %)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9004,23,'FRANCES',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),10000.00,36,'VIAJE',3500.00,1200.00,'2026-08-13','2026-08-13 10:00',350.40,0.1677,355.62,'APROBADA','2026-08-15','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9004,23,'1751001619','FRANCES','OP-20260816-009004',10000.00,0.1677,36,6.00,50.00,9950.00,'2200000023','2026-08-16','ENTREGADA','POR_VENCER',10000.00,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),1,'2026-09-16',219.59,130.03,6.00,9780.41,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),2,'2026-10-16',222.44,127.18,6.00,9557.97,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),3,'2026-11-16',225.33,124.29,6.00,9332.64,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),4,'2026-12-16',228.26,121.36,6.00,9104.38,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),5,'2027-01-16',231.23,118.39,6.00,8873.15,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),6,'2027-02-16',234.24,115.38,6.00,8638.91,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),7,'2027-03-16',237.28,112.34,6.00,8401.63,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),8,'2027-04-16',240.37,109.25,6.00,8161.26,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),9,'2027-05-16',243.50,106.12,6.00,7917.76,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),10,'2027-06-16',246.66,102.96,6.00,7671.10,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),11,'2027-07-16',249.87,99.75,6.00,7421.23,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),12,'2027-08-16',253.12,96.50,6.00,7168.11,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),13,'2027-09-16',256.41,93.21,6.00,6911.70,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),14,'2027-10-16',259.74,89.88,6.00,6651.96,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),15,'2027-11-16',263.12,86.50,6.00,6388.84,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),16,'2027-12-16',266.54,83.08,6.00,6122.30,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),17,'2028-01-16',270.01,79.61,6.00,5852.29,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),18,'2028-02-16',273.52,76.10,6.00,5578.77,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),19,'2028-03-16',277.08,72.54,6.00,5301.69,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),20,'2028-04-16',280.68,68.94,6.00,5021.01,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),21,'2028-05-16',284.33,65.29,6.00,4736.68,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),22,'2028-06-16',288.03,61.59,6.00,4448.65,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),23,'2028-07-16',291.77,57.85,6.00,4156.88,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),24,'2028-08-16',295.57,54.05,6.00,3861.31,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),25,'2028-09-16',299.41,50.21,6.00,3561.90,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),26,'2028-10-16',303.30,46.32,6.00,3258.60,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),27,'2028-11-16',307.25,42.37,6.00,2951.35,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),28,'2028-12-16',311.24,38.38,6.00,2640.11,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),29,'2029-01-16',315.29,34.33,6.00,2324.82,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),30,'2029-02-16',319.39,30.23,6.00,2005.43,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),31,'2029-03-16',323.54,26.08,6.00,1681.89,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),32,'2029-04-16',327.75,21.87,6.00,1354.14,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),33,'2029-05-16',332.01,17.61,6.00,1022.13,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),34,'2029-06-16',336.33,13.29,6.00,685.80,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),35,'2029-07-16',340.70,8.92,6.00,345.10,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9004),36,'2029-08-16',345.10,4.49,6.00,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- OP-20260920-009005 · cliente 24 (1701001685) · 11 días de mora con la cuota 1 pagada en parte
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9005,24,'FRANCES',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),4000.00,12,'OTRO',2200.00,800.00,'2026-09-17','2026-09-17 10:00',362.53,0.1550,362.53,'APROBADA','2026-09-19','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9005,24,'1701001685','FRANCES','OP-20260920-009005',4000.00,0.1550,12,2.40,20.00,3980.00,'2200000024','2026-09-20','ENTREGADA','POR_VENCER',3844.12,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),1,'2026-10-20',311.76,48.32,2.40,3688.24,2.40,48.32,155.88,'PAGO_PARCIAL',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),2,'2026-11-20',315.52,44.56,2.40,3372.72,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),3,'2026-12-20',319.34,40.74,2.40,3053.38,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),4,'2027-01-20',323.19,36.89,2.40,2730.19,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),5,'2027-02-20',327.10,32.98,2.40,2403.09,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),6,'2027-03-20',331.05,29.03,2.40,2072.04,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),7,'2027-04-20',335.05,25.03,2.40,1736.99,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),8,'2027-05-20',339.10,20.98,2.40,1397.89,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),9,'2027-06-20',343.19,16.89,2.40,1054.70,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),10,'2027-07-20',347.34,12.74,2.40,707.36,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),11,'2027-08-20',351.53,8.55,2.40,355.83,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9005),12,'2027-09-20',355.83,4.30,2.40,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- OP-20260601-009006 · cliente 25 (1711001758) · 122 días de mora (tramo 4, EN_LITIGIO 60 %)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9006,25,'ALEMAN',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),12000.00,48,'BIENES_DURADEROS',4500.00,1500.00,'2026-05-29','2026-05-29 10:00',402.17,0.1650,410.90,'APROBADA','2026-05-31','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9006,25,'1711001758','ALEMAN','OP-20260601-009006',12000.00,0.1650,48,7.20,60.00,11940.00,'2200000025','2026-06-01','ENTREGADA','POR_VENCER',12000.00,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),1,'2026-07-01',250.00,153.70,7.20,11750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),2,'2026-08-01',250.00,150.50,7.20,11500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),3,'2026-09-01',250.00,147.29,7.20,11250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),4,'2026-10-01',250.00,144.09,7.20,11000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),5,'2026-11-01',250.00,140.89,7.20,10750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),6,'2026-12-01',250.00,137.69,7.20,10500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),7,'2027-01-01',250.00,134.48,7.20,10250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),8,'2027-02-01',250.00,131.28,7.20,10000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),9,'2027-03-01',250.00,128.08,7.20,9750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),10,'2027-04-01',250.00,124.88,7.20,9500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),11,'2027-05-01',250.00,121.68,7.20,9250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),12,'2027-06-01',250.00,118.47,7.20,9000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),13,'2027-07-01',250.00,115.27,7.20,8750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),14,'2027-08-01',250.00,112.07,7.20,8500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),15,'2027-09-01',250.00,108.87,7.20,8250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),16,'2027-10-01',250.00,105.67,7.20,8000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),17,'2027-11-01',250.00,102.46,7.20,7750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),18,'2027-12-01',250.00,99.26,7.20,7500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),19,'2028-01-01',250.00,96.06,7.20,7250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),20,'2028-02-01',250.00,92.86,7.20,7000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),21,'2028-03-01',250.00,89.66,7.20,6750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),22,'2028-04-01',250.00,86.45,7.20,6500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),23,'2028-05-01',250.00,83.25,7.20,6250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),24,'2028-06-01',250.00,80.05,7.20,6000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),25,'2028-07-01',250.00,76.85,7.20,5750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),26,'2028-08-01',250.00,73.65,7.20,5500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),27,'2028-09-01',250.00,70.44,7.20,5250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),28,'2028-10-01',250.00,67.24,7.20,5000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),29,'2028-11-01',250.00,64.04,7.20,4750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),30,'2028-12-01',250.00,60.84,7.20,4500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),31,'2029-01-01',250.00,57.64,7.20,4250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),32,'2029-02-01',250.00,54.43,7.20,4000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),33,'2029-03-01',250.00,51.23,7.20,3750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),34,'2029-04-01',250.00,48.03,7.20,3500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),35,'2029-05-01',250.00,44.83,7.20,3250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),36,'2029-06-01',250.00,41.63,7.20,3000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),37,'2029-07-01',250.00,38.42,7.20,2750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),38,'2029-08-01',250.00,35.22,7.20,2500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),39,'2029-09-01',250.00,32.02,7.20,2250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),40,'2029-10-01',250.00,28.82,7.20,2000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),41,'2029-11-01',250.00,25.62,7.20,1750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),42,'2029-12-01',250.00,22.41,7.20,1500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),43,'2030-01-01',250.00,19.21,7.20,1250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),44,'2030-02-01',250.00,16.01,7.20,1000.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),45,'2030-03-01',250.00,12.81,7.20,750.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),46,'2030-04-01',250.00,9.61,7.20,500.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),47,'2030-05-01',250.00,6.40,7.20,250.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9006),48,'2030-06-01',250.00,3.20,7.20,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- OP-20260315-009007 · cliente 26 (1721001822) · 199 días de mora (tramo 5, CASTIGADA 100 %)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9007,26,'FRANCES',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),3000.00,12,'OTRO',1800.00,700.00,'2026-03-12','2026-03-12 10:00',271.91,0.1677,273.45,'APROBADA','2026-03-14','demo-pruebas');
INSERT INTO "operaciones" ("idSolicitud","idCliente","cedulaCliente","sistemaAmortizacion","numeroOperacion","montoAprobado","teaAsignada","plazoMeses","seguroMensual","contribucionSolca","montoNeto","cuentaDestino","fechaDesembolso","estadoInstruccionDesembolso","estadoCartera","saldoCapital","diasMora","usuarioCreacion")
VALUES (9007,26,'1721001822','FRANCES','OP-20260315-009007',3000.00,0.1677,12,1.80,15.00,2985.00,'2200000026','2026-03-15','ENTREGADA','POR_VENCER',3000.00,0,'demo-pruebas');
INSERT INTO "cuotas" ("idOperacion","numeroCuota","fechaVencimiento","valorCapital","valorInteres","valorSeguro","saldoCapitalPosterior","seguroPagado","interesPagado","capitalPagado","estadoCuota","fechaPago","usuarioCreacion") VALUES
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),1,'2026-04-15',232.62,39.01,1.80,2767.38,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),2,'2026-05-15',235.64,35.99,1.80,2531.74,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),3,'2026-06-15',238.71,32.92,1.80,2293.03,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),4,'2026-07-15',241.81,29.82,1.80,2051.22,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),5,'2026-08-15',244.96,26.67,1.80,1806.26,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),6,'2026-09-15',248.14,23.49,1.80,1558.12,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),7,'2026-10-15',251.37,20.26,1.80,1306.75,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),8,'2026-11-15',254.64,16.99,1.80,1052.11,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),9,'2026-12-15',257.95,13.68,1.80,794.16,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),10,'2027-01-15',261.30,10.33,1.80,532.86,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),11,'2027-02-15',264.70,6.93,1.80,268.16,0,0,0,'PENDIENTE',NULL,'demo-pruebas'),
 ((SELECT "idOperacion" FROM "operaciones" WHERE "idSolicitud"=9007),12,'2027-03-15',268.16,3.49,1.80,0.00,0,0,0,'PENDIENTE',NULL,'demo-pruebas');

-- Solicitud REGISTRADA, aparece en la bandeja del analista (cliente 27)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","estadoSolicitud","usuarioCreacion")
VALUES (9008,27,'FRANCES',7000.00,24,'EDUCACION',2900.00,1000.00,'2026-10-30','2026-10-30 09:30',341.94,'REGISTRADA','demo-pruebas');

-- Solicitud APROBADA sin desembolsar, aparece en desembolsos pendientes (cliente 28)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","teaAsignada","cuotaEvaluacionDefinitiva","estadoSolicitud","fechaResolucion","usuarioCreacion")
VALUES (9009,28,'ALEMAN',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),5000.00,12,'SALUD',2400.00,900.00,'2026-10-29','2026-10-29 15:10',480.07,0.1500,478.24,'APROBADA','2026-10-30','demo-pruebas');

-- Solicitud RECHAZADA por el analista (cliente 29)
INSERT INTO "solicitudes" ("idSolicitud","idCliente","sistemaAmortizacion","idAnalista","montoSolicitado","plazoMeses","destinoCredito","ingresosMensuales","gastosMensuales","fechaSolicitud","fechaHoraRegistro","cuotaEvaluacionReferencial","estadoSolicitud","fechaResolucion","motivoRechazo","usuarioCreacion")
VALUES (9010,29,'FRANCES',(SELECT "idAnalista" FROM "analistas" WHERE "cedulaAnalista"='1799999901'),20000.00,48,'VIAJE',3000.00,1500.00,'2026-10-28','2026-10-28 11:45',563.53,'RECHAZADA','2026-10-29','Historial crediticio insuficiente para el monto solicitado.','demo-pruebas');

COMMIT;
