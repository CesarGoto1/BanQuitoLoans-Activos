# Implementación y verificación de RF-05

RF-05 está implementado en el backend y conectado al detalle React de Fernando. La decisión se guarda mediante JPA en PostgreSQL, utiliza los parámetros vigentes y la fecha de negocio, recalcula la cuota con la amortización existente y evita decisiones duplicadas. El flujo completo desde el registro RF-02 todavía no puede declararse terminado: la rama de David contiene dependencias sin implementar. La verificación visual en Opera tampoco pudo completarse por una limitación de la herramienta de interacción.

## Material disponible y decisiones de integración

Se revisaron los fuentes, pruebas, configuración Maven/Vite, scripts y estilos del repositorio, junto con estos documentos de `C:/Users/Micha/Downloads`:

- `Especificacion_de_Requisitos-BanQuitoLoans-v2.0.md`.
- `BanQuito-estandares-desarrollo-etapa1.docx`.
- `BanQuito · Características arquitectónicas.pdf`.
- `corePrestamos.sql`, `corePasivos.sql`, `datos-iniciales-prestamos.sql` y `datos-simulados-pasivos.sql`.
- `guia-estilo2.html` y la guía incluida en `frontend/docs/style-guide.html`.

También se consultaron las guías antiguas por persona y por grupos encontradas en Descargas. Describen una versión anterior del modelo; sus tablas y estados adicionales no se trasladaron al esquema actual. El documento de etapa 1 pide identificadores españoles y una reorganización de carpetas, pero esta implementación sigue la instrucción explícita de usar inglés para código nuevo y conservar la estructura existente. Los nombres españoles incorporados desde amortización son contratos y código ya escritos por Christian, conservados sin traducción.

No se encontró un archivo independiente con el diagrama arquitectónico completo indicado en el encargo, ni las transcripciones del ingeniero. Se utilizaron el contexto de requisitos, la arquitectura documentada y los límites reales de los paquetes. Los comentarios del SQL identifican almacenes numerados del diagrama; no se interpretaron como repositorios JPA. No había logs de ejecución ni informes Surefire previos disponibles al comenzar; se generó una línea base antes de editar.

### Módulos y componentes

| Módulo | Situación e interfaces |
| --- | --- |
| `origination` | Propietario de `solicitudes`. `SolicitudService` expone bandeja/detalle; `LoanDecisionService` expone aprobación/rechazo. Entidades, repositorios, controladores y reglas permanecen en `internal`. |
| `parameters` | `ParameterService` consulta valores por código y vigencia; `BusinessDateService` obtiene la fecha abierta de `cierresDia`. Repositorios y entidades internos. |
| `amortization` | Se incorporaron el servicio de Christian, sus cinco contratos y sus pruebas existentes. RF-05 usa exclusivamente su interfaz pública. |
| `access`, `accounting`, `closing`, `collections`, `liabilities`, `loans`, `payments`, `reports` | En la rama de trabajo eran estructuras de módulo sin implementación de negocio. RF-05 no depende de sus repositorios ni crea desembolsos. |

El flujo es React → `LoanDecisionController` → `LoanDecisionServiceImpl` → `LoanApprovalPolicy`, parámetros y amortización → `SolicitudRepository` → PostgreSQL. No se crearon aplicaciones nuevas, microservicios, dependencias Java adicionales ni fórmulas financieras duplicadas.

### Reutilización de Fernando

Se conservaron `Solicitud`, `SolicitudRepository`, `EstadoSolicitud`, los DTO y los dos GET existentes. RF-05 escribe la misma fila que lee `SolicitudServiceImpl`. La bandeja vuelve a consultar solicitudes registradas al regresar desde el detalle. El componente `LoanDecisionPanel` ocupa el contenedor `contenedor-resolucion-p6` de `DetalleSolicitudPagina`; no existe una segunda pantalla de revisión.

Se corrigió la comparación de capacidad del detalle: ahora compara importes monetarios, en lugar de porcentajes redondeados. Una cuota de 400,01 con límite de 400,00 no pasa aunque el porcentaje mostrado sea 40,00 %. La pantalla distingue la evaluación referencial de la aprobación definitiva.

### RF-02 y entidad propietaria

La rama local de seguimiento `origin/feature/david-registrosolicitud` contiene `LoanApplicationService`, `LoanApplication`, su repositorio, controlador, DTO y frontend. Su servicio lanza `UnsupportedOperationException` en tres puntos: identificación del cliente, consulta de mora y amortización. Además, expone tipos de `internal` desde su interfaz pública y utiliza `/api/customer/loan-applications`, mientras los estándares documentan `/api/cliente/solicitudes`.

No se incorporaron esos métodos incompletos ni la segunda entidad JPA. `Solicitud` y `SolicitudRepository` siguen siendo los únicos propietarios de la tabla en esta rama. La integración pendiente de David debe adaptar su servicio de registro a ellos, resolver cliente por la API pública de `liabilities`, consultar mora por la API pública de `loans` y utilizar `AmortizacionService`. Debe conservar o adaptar conscientemente sus consumidores HTTP. No se afirma haber ejecutado el endpoint RF-02: las pruebas de RF-05 crean solicitudes válidas mediante JPA como preparación de pruebas.

### Amortización reutilizada

Origen: `origin/feature/ChristianV-AmortizacionSimulacion`, commit `2a6303c21be8b932d9cb2812f1ed10c21d3134fe`, disponible localmente. No se hizo merge masivo de la rama ni se importó su pantalla de simulación.

Se llama a `AmortizacionService.generarTabla(CondicionesAmortizacion)`, con monto y plazo originales, TEA definitiva, seguro mensual calculado con PAR-07 y fecha de negocio. Se toma la mayor `totalCuota` de la tabla; esto incluye el máximo alemán y cualquier ajuste de redondeo final. La tabla se calcula en memoria para evaluar; no se persisten cuotas ni operaciones. SOLCA no participa en la cuota mensual.

## Esquema PostgreSQL

Los scripts oficiales de Descargas y los de `database` solo difieren en comentarios y espacios; sus definiciones y datos iniciales coinciden. No se modificaron.

| Aspecto | Implementación |
| --- | --- |
| PK `idSolicitud` | `Integer`, identidad generada por PostgreSQL. |
| Montos | `numeric(10,2)` / `BigDecimal`, precisión explícita en `Solicitud`. |
| `teaAsignada` | `numeric(5,4)`; validación de máximo cuatro decimales, positiva y no superior a PAR-05. |
| Estados | Se mantienen `REGISTRADA`, `APROBADA`, `RECHAZADA`. |
| Aprobación | TEA y cuota definitiva no nulas; motivo nulo; analista y fecha de resolución presentes. |
| Rechazo | Motivo obligatorio de hasta 200 caracteres; TEA y cuota definitiva nulas; analista y fecha registrados. |
| Auditoría | Se preservan usuario/fecha de creación; se actualizan usuario/fecha de modificación conjuntamente. El usuario procede de la cabecera validada. |
| Analista | Búsqueda JPA por `cedulaAnalista`, columna única de `analistas`; se persiste el ID obtenido. No se crea un analista automáticamente. |
| Fecha | Resolución obtenida de `BusinessDateService`; la marca temporal de auditoría utiliza el reloj, separada de la fecha contable. |
| Concurrencia | Sin columna de versión. `PESSIMISTIC_WRITE` con espera cero sobre la solicitud, dentro de la transacción. |
| Índices | Se reutilizan la PK y los índices existentes de bandeja y cliente. |

Los datos iniciales establecen PAR-01 = 500, PAR-02 = 30000, PAR-03 = 6, PAR-04 = 60, PAR-05 = 0,1677, PAR-06 = 0,1550, PAR-07 = 0,0006, PAR-08 = 0,0050 y PAR-09 = 0,4000, vigentes desde el 31/10/2026. RF-05 consulta PAR-05, PAR-07 y PAR-09 para la misma fecha de negocio. Los límites no están fijados en el servicio. La fecha final de vigencia se trata como exclusiva, conforme al servicio existente.

`corePasivos` conserva clientes, cuentas y movimientos en otra base. RF-05 no consulta sus tablas, no introduce FK entre bases y no necesita disponibilidad del Core de Pasivos para resolver una solicitud ya registrada.

La auditoría implementada utiliza las columnas reales de `solicitudes`. El esquema actual no contiene una bitácora general de solo adición con valores anteriores/nuevos; no se inventó una tabla para declarar cumplido el alcance global de QA-08. Esa capacidad transversal sigue siendo una decisión pendiente del equipo.

## Contratos HTTP

| Método y ruta | Resultado |
| --- | --- |
| `GET /api/analista/solicitudes?estado=REGISTRADA` | Bandeja existente. |
| `GET /api/analista/solicitudes/{idSolicitud}` | Detalle existente. |
| `POST /api/analista/solicitudes/{idSolicitud}/aprobacion` | Resuelve con `{"teaAsignada":0.1650}`. |
| `POST /api/analista/solicitudes/{idSolicitud}/rechazo` | Resuelve con `{"motivoRechazo":"Motivo de la decisión"}`. |

Los POST requieren `X-Rol: ANALISTA` y `X-Identificacion` con 10 dígitos correspondientes a un analista existente. El cuerpo no decide el analista. Se devuelve un DTO con `idSolicitud`, `estadoSolicitud`, `teaAsignada`, `cuotaEvaluacionDefinitiva`, `fechaResolucion`, `motivoRechazo` e `idAnalista`, nunca una entidad JPA.

Los códigos son 200 para resolución, 400 para formato inválido, 403 para rol incorrecto o analista inexistente, 404 para solicitud inexistente, 409 para estado resuelto, regla financiera o conflicto de bloqueo, y 500 para fallos inesperados. Los errores son `ProblemDetail` en español. Los logs nuevos utilizan mensajes técnicos en inglés y no registran identificaciones completas ni motivos libres.

## Frontend

`AnalystSession` permite indicar la identificación usada por el acceso simulado y la conserva solo en la sesión del navegador. Esto reemplaza la identificación fija anterior; la validación de existencia se realiza en el backend al resolver.

`LoanDecisionPanel` incluye TEA en fracción decimal, motivo de hasta 200 caracteres, confirmación de rechazo, bloqueo de ambos formularios durante el envío, guardia inmediata contra doble envío, errores accesibles y confirmación de éxito posterior a la respuesta HTTP. Al resolver actualiza el detalle; al volver, la bandeja consulta nuevamente. Ante un 409 o una respuesta incierta intenta refrescar el estado; si no puede, exige actualizar antes de permitir otro envío.

El cliente HTTP dejó de devolver solicitudes ficticias cuando falla la conexión. Se conserva `VITE_API_PRESTAMOS` para instalaciones que ya lo utilizan y se soporta `VITE_API=/api` con el proxy Vite existente. El flujo verificado por build utiliza dicho proxy. La hoja de estilos actual azul/amarillo y las clases `bq-*` se mantienen sin añadir otra paleta ni estilos inline. Los importes se presentan con `Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' })`.

## Archivos

Nuevos en `backend/src/main/java/com/grupob/bancobanquito/origination`:

- `LoanDecisionService.java`, `LoanDecisionResult.java`.
- `internal/LoanDecisionServiceImpl.java`, `internal/LoanDecisionController.java`.
- `internal/Analyst.java`, `internal/AnalystRepository.java`.

Incorporados sin reescribir desde la rama de Christian:

- `amortization/AmortizacionService.java`, `CondicionesAmortizacion.java`, `CuotaAmortizacion.java`, `SistemaAmortizacion.java`, `TablaAmortizacion.java`.
- `amortization/internal/AmortizacionServiceImpl.java`.
- `backend/src/test/java/com/grupob/bancobanquito/amortization/internal/AmortizacionServiceTest.java`.

Modificados en backend:

- `Solicitud.java`: mapeo SQL explícito.
- `SolicitudRepository.java`: consulta bloqueante de resolución.
- `SolicitudServiceImpl.java`: comparación monetaria de capacidad.
- `SolicitudExceptionHandler.java`: validación, conflictos y fallos inesperados.
- `SolicitudServiceCapacidadPagoTest.java`: regresión del centavo sobre el límite.

Nuevas pruebas:

- `LoanApprovalBoundaryTest.java`, `LoanDecisionServiceTest.java`, `LoanDecisionControllerTest.java`, `LoanDecisionPostgresTest.java`.
- `backend/src/test/resources/rf05-analysts.sql`, exclusivamente para preparar analistas ficticios de prueba.
- `frontend/src/portal/servicios/prestamosApi.test.js`.

Frontend nuevo/modificado:

- Nuevos `portal/components/AnalystSession.jsx` y `LoanDecisionPanel.jsx`.
- Modificados `App.jsx`, `portal/paginas/BandejaPagina.jsx`, `DetalleSolicitudPagina.jsx` y `portal/servicios/prestamosApi.js`.
- `package.json` agrega el comando de pruebas con el ejecutor nativo de Node; `package-lock.json` fija las versiones resueltas. No se agregaron dependencias de pruebas React.

El cambio previo del usuario en `LoanApprovalPolicy.java` se conservó. Sus ocho pruebas originales no fueron reemplazadas. No se crearon commits ni se publicó la rama.

## Verificación y limitaciones

La línea base ejecutó 19 pruebas sin fallos antes de implementar. La verificación final mediante Maven `verify` pasó 88 pruebas, con cero fallos, errores u omisiones, incluyendo nueve casos de integración PostgreSQL, modularidad, amortización, reglas y controladores. También generó el JAR ejecutable. Los resultados se encuentran en `backend/target/surefire-reports` y `backend/target/rf05-verification.log`. Las seis pruebas frontend pasaron y el build final de Vite terminó correctamente.

Las pruebas PostgreSQL usan una instancia PostgreSQL 18 independiente en `127.0.0.1:55435`, base `rf05_test`, inicializada con los dos scripts oficiales. Hibernate arranca con `ddl-auto=validate`. Se comprueban persistencia tras finalizar transacciones, seguro mensual, cuota alemana máxima, conservación de auditoría, rollback después de `flush`, compatibilidad bandeja/detalle, ausencia de desembolso, decisiones concurrentes y aprobación mediante HTTP real con conflicto al reintentar. Los datos de preparación son ficticios, pero las operaciones y verificaciones de persistencia se ejecutan realmente en PostgreSQL.

La instancia habitual de PostgreSQL en 5432 estaba activa, pero rechazó las credenciales predeterminadas del proyecto. No se modificó su configuración ni sus datos. Las pruebas de parámetros existentes también necesitan una base con los datos iniciales y la fecha 31/10/2026.

Al terminar se detuvieron el backend, Vite y el clúster temporal iniciados para esta verificación. Los datos y logs temporales permanecen en `backend/target/rf05-postgres` y `backend/target`. Para reutilizar ese clúster local, desde `backend` puede ejecutarse:

```powershell
& 'C:/Program Files/PostgreSQL/18/bin/pg_ctl.exe' -D 'target/rf05-postgres' -l 'target/rf05-postgres.log' -o '-p 55435 -h 127.0.0.1' -w start
```

Las seis pruebas frontend validan contratos, cabeceras, conflictos, fallos de red, respuestas incompletas y consultas sin caché. Vite genera el build de producción. No equivalen a pruebas de interacción React en navegador. La herramienta de UI se detuvo al no poder determinar la URL de Opera con suficiente confianza; no se verificaron visualmente accesibilidad, responsive ni consola del navegador. No se midieron percentiles de rendimiento, disponibilidad, RPO o RTO.

Los avisos de Mockito por carga dinámica del agente ya estaban en la línea base. Los errores SQLState 55P03 durante las carreras son el resultado esperado del bloqueo sin espera. Los fallos 500 inducidos por las pruebas comprueban que no se expongan detalles internos.

## Ejecutar desde IntelliJ y demostrar

1. Abrir el repositorio e importar `backend/pom.xml` como Maven; seleccionar JDK 25. Se conservan Spring Boot 4.0.8 y Spring Modulith 2.0.8.
2. Preparar una base PostgreSQL vacía de desarrollo y ejecutar, en orden, `database/corePrestamos.sql` y `database/datos-iniciales-prestamos.sql`. No repetirlos sobre una base ya inicializada.
3. En la configuración de `BancoBanQuitoApplication`, definir `DB_URL`, `DB_USER` y `DB_PASSWORD` con los valores reales de esa base. Ejecutar la clase principal; el puerto predeterminado es 8080.
4. Desde `frontend`, ejecutar `npm ci` y `npm run dev`. Abrir `http://localhost:5173`. El proxy `/api` comunica con 8080; `.env.example` ya define `VITE_API=/api`.
5. Ingresar la identificación de un analista existente. Abrir una solicitud registrada desde la bandeja. Introducir `0.1677` para 16,77 % y aprobar, o escribir un motivo y confirmar el rechazo. Revisar el estado y la cuota/motivo; volver a la bandeja para comprobar que desapareció de pendientes.

Para una demostración aislada, el fixture `rf05-analysts.sql` crea los analistas ficticios `1799999901` y `1799999902`. Debe cargarse únicamente en la base de pruebas. Se pueden preparar allí solicitudes con este SQL, sin atribuir su creación a RF-02:

```sql
INSERT INTO "solicitudes" (
    "idCliente", "sistemaAmortizacion", "montoSolicitado", "plazoMeses", "destinoCredito",
    "ingresosMensuales", "gastosMensuales", "fechaSolicitud", "fechaHoraRegistro",
    "cuotaEvaluacionReferencial", "estadoSolicitud", "usuarioCreacion"
) VALUES
    (101, 'FRANCES', 10000, 12, 'EDUCACION', 3500, 1200, '2026-10-31', CURRENT_TIMESTAMP, 900, 'REGISTRADA', 'rf05-demo'),
    (102, 'ALEMAN', 10000, 12, 'EDUCACION', 3500, 1200, '2026-10-31', CURRENT_TIMESTAMP, 960, 'REGISTRADA', 'rf05-demo');
```

Con TEA 0,1677, la francesa debe aprobar con cuota 911,44; la alemana debe impedir la aprobación por superar 920,00 y permitir su rechazo con motivo. No se debe registrar ninguna operación desembolsada.

### Comandos de pruebas

Desde `backend`, con `mvn` disponible y una base de pruebas separada cargada con los scripts:

```powershell
$env:DB_URL='jdbc:postgresql://127.0.0.1:55435/rf05_test'
$env:DB_USER='postgres'
$env:RF05_POSTGRES_TESTS='true'
mvn verify
```

Definir `DB_PASSWORD` según la configuración de esa instancia. En esta sesión se usó autenticación local `trust` exclusivamente en el clúster temporal de pruebas; no se cambió la autenticación de la base habitual. Si Maven no está en PATH, IntelliJ dispone de `C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/plugins/maven-plugin/lib/maven3/bin/mvn.cmd`.

Sin PostgreSQL, ejecutar solo la suite independiente de persistencia:

```powershell
mvn test '-Dtest=ModularityTest,LoanApprovalPolicyTest,LoanApprovalBoundaryTest,LoanDecisionServiceTest,LoanDecisionControllerTest,SolicitudAnalistaControllerTest,SolicitudServiceCapacidadPagoTest,AmortizacionServiceTest'
```

`LoanDecisionPostgresTest` se habilita únicamente con `RF05_POSTGRES_TESTS=true`. No confundir una ejecución con esas pruebas omitidas con una verificación de persistencia.

Desde `frontend`:

```powershell
npm test
npm run build
```
