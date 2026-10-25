INSERT INTO "parametros" ("codigoParametro", "nombreParametro", "valorParametro", "fechaVigenciaDesde", "usuarioCreacion") VALUES
 ('PAR-01', 'Monto mínimo del crédito',            500.0000, '2026-10-31', 'sistema'),
 ('PAR-02', 'Monto máximo del crédito',          30000.0000, '2026-10-31', 'sistema'),
 ('PAR-03', 'Plazo mínimo en meses',                 6.0000, '2026-10-31', 'sistema'),
 ('PAR-04', 'Plazo máximo en meses',                60.0000, '2026-10-31', 'sistema'),
 ('PAR-05', 'TEA máxima consumo',                    0.1677, '2026-10-31', 'sistema'),
 ('PAR-06', 'TEA de referencia',                     0.1550, '2026-10-31', 'sistema'),
 ('PAR-07', 'Seguro de desgravamen mensual',         0.0006, '2026-10-31', 'sistema'),
 ('PAR-08', 'Contribución SOLCA',                    0.0050, '2026-10-31', 'sistema'),
 ('PAR-09', 'Cuota máxima sobre ingreso remanente',  0.4000, '2026-10-31', 'sistema'),
 ('PAR-10', 'Factor de mora',                        1.1000, '2026-10-31', 'sistema'),
 ('PAR-11', 'Base de días del año',                360.0000, '2026-10-31', 'sistema'),
 ('PAR-12', 'Abono extraordinario mínimo',          50.0000, '2026-10-31', 'sistema');

INSERT INTO "tramosCartera" ("numeroTramo", "diasMoraDesde", "diasMoraHasta", "estadoCartera", "devengaIngresos",
                             "tratamientoMora", "porcentajeProvision", "fechaVigenciaDesde", "usuarioCreacion") VALUES
 (1,   0,    0, 'POR_VENCER', true,  'NO_APLICA',          0.0100, '2026-10-31', 'sistema'),
 (2,   1,   30, 'VENCIDA',    true,  'SE_CALCULA',         0.0500, '2026-10-31', 'sistema'),
 (3,  31,   90, 'VENCIDA',    true,  'SE_CALCULA',         0.2500, '2026-10-31', 'sistema'),
 (4,  91,  180, 'EN_LITIGIO', false, 'CUENTAS_DE_ORDEN',   0.6000, '2026-10-31', 'sistema'),
 (5, 181, NULL, 'CASTIGADA',  false, 'DEJA_DE_ACUMULARSE', 1.0000, '2026-10-31', 'sistema');

INSERT INTO "cierresDia" ("fechaNegocio", "estadoCierre", "usuarioCreacion") VALUES
 ('2026-10-31', 'PENDIENTE', 'sistema');

INSERT INTO "codigosValidacion" ("codigoValidacion", "nivelValidacion", "descripcionValidacion", "usuarioCreacion") VALUES
 ('ARC-001', 'ARCHIVO',  'El nombre del archivo no cumple el patrón establecido', 'sistema'),
 ('ARC-002', 'ARCHIVO',  'Falta la cabecera o el pie, o la estructura es inválida', 'sistema'),
 ('ARC-003', 'ARCHIVO',  'El total de registros no coincide entre cabecera, pie y detalle', 'sistema'),
 ('ARC-004', 'ARCHIVO',  'El monto total no coincide entre cabecera, pie y la suma del detalle', 'sistema'),
 ('ARC-005', 'ARCHIVO',  'El hash del pie no coincide con el calculado', 'sistema'),
 ('ARC-006', 'ARCHIVO',  'Ya se procesó con éxito un archivo con el mismo nombre y hash', 'sistema'),
 ('ARC-007', 'ARCHIVO',  'La fecha de proceso no corresponde a la fecha de negocio vigente', 'sistema'),
 ('REG-001', 'REGISTRO', 'Formato inválido: campo faltante, tipo o longitud incorrectos', 'sistema'),
 ('REG-002', 'REGISTRO', 'El UUID de la transacción ya fue procesado', 'sistema'),
 ('REG-003', 'REGISTRO', 'La operación no existe', 'sistema'),
 ('REG-004', 'REGISTRO', 'La identificación no corresponde al titular de la operación', 'sistema'),
 ('REG-005', 'REGISTRO', 'Monto inválido: menor o igual a cero, o con más de dos decimales', 'sistema'),
 ('REG-006', 'REGISTRO', 'La operación está cancelada', 'sistema'),
 ('REG-007', 'REGISTRO', 'Sobrepago: la parte del valor que excede el total adeudado', 'sistema');

INSERT INTO "cuentasContables" ("codigoCuenta", "nombreCuenta", "tipoCuenta", "naturalezaCuenta", "usuarioCreacion") VALUES
 ('CARTERA_POR_VENCER',        'Cartera por vencer',                 'ACTIVO',  'DEUDORA',   'sistema'),
 ('CARTERA_VENCIDA',           'Cartera vencida',                    'ACTIVO',  'DEUDORA',   'sistema'),
 ('CARTERA_EN_LITIGIO',        'Cartera en litigio',                 'ACTIVO',  'DEUDORA',   'sistema'),
 ('INTERESES_POR_COBRAR',      'Intereses por cobrar',               'ACTIVO',  'DEUDORA',   'sistema'),
 ('INTERESES_MORA_POR_COBRAR', 'Intereses de mora por cobrar',       'ACTIVO',  'DEUDORA',   'sistema'),
 ('PROVISIONES_CARTERA',       'Provisiones de cartera',             'ACTIVO',  'ACREEDORA', 'sistema'),
 ('CUENTA_CLIENTE',            'Cuenta del cliente',                 'PASIVO',  'ACREEDORA', 'sistema'),
 ('SOLCA_POR_PAGAR',           'Contribución SOLCA por pagar',       'PASIVO',  'ACREEDORA', 'sistema'),
 ('SEGUROS_POR_PAGAR',         'Seguro de desgravamen por pagar',    'PASIVO',  'ACREEDORA', 'sistema'),
 ('RECAUDACIONES_POR_APLICAR', 'Recaudaciones por aplicar',          'PASIVO',  'ACREEDORA', 'sistema'),
 ('INGRESOS_INTERESES',        'Ingresos por intereses',             'INGRESO', 'ACREEDORA', 'sistema'),
 ('INGRESOS_MORA',             'Ingresos por intereses de mora',     'INGRESO', 'ACREEDORA', 'sistema'),
 ('INGRESOS_RECUPERACIONES',   'Ingresos por recuperaciones',        'INGRESO', 'ACREEDORA', 'sistema'),
 ('GASTO_PROVISIONES',         'Gasto de provisiones',               'GASTO',   'DEUDORA',   'sistema'),
 ('INTERESES_EN_SUSPENSO',     'Intereses en suspenso',              'ORDEN',   'DEUDORA',   'sistema'),
 ('CARTERA_CASTIGADA',         'Cartera castigada',                  'ORDEN',   'DEUDORA',   'sistema'),
 ('CONTRA_CUENTAS_ORDEN',      'Contrapartida de cuentas de orden',  'ORDEN',   'ACREEDORA', 'sistema');
