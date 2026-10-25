# Backend P1: simulación y amortización en BanQuitoLoans-Activos

## Adaptación realizada

Se trasladó exclusivamente el backend RF-01 y la generación inicial de RF-07 desde BanQuitoLoans. El frontend y los scripts de database no se modificaron. No se crea una solicitud ni se persisten operaciones, cuotas o movimientos al simular.

- Se utiliza el módulo existente `com.grupob.bancobanquito.amortization`; se conservan los nombres españoles de los DTO, métodos de cálculo y campos JSON para mantener el contrato ya trabajado.
- Se consumen `parameters.ParameterService.getValue` y `parameters.BusinessDateService.getCurrentBusinessDate`. No se duplican entidades ni servicios de parametrización.
- Se mantiene la entidad Parameter existente con sus validaciones, precisión decimal y nombres SQL. Se conserva sin modificaciones el código de producción de parameters publicado en main: selecciona el registro más reciente desde la fecha consultada y comprueba su vigencia (desde inclusiva y hasta exclusiva). No rechaza vigencias superpuestas; esa mejora corresponde al responsable del módulo compartido.
- Se conserva ParameterNotFoundException para parámetros ausentes. La API de simulación la convierte en 503 sin exponer detalles internos.
- Springdoc 3.0.3 ya estaba instalado. Solo se añade H2 como dependencia de pruebas. Se mantienen Boot 4.0.8, Java 25, Modulith 2.0.8 y Tomcat.
- Las pruebas existentes de parámetros ahora cargan datos ficticios en H2 y no dependen del contenido de PostgreSQL. Las pruebas de cálculo, servicio, controlador, integración y modularidad se conservan.
- No se añade una pantalla administrativa ni CRUD de parámetros. La generación no requiere optimistic locking porque no modifica entidades.

## Arranque en IntelliJ

1. Abre BanQuitoLoans-Activos e importa `backend/pom.xml` como proyecto Maven. Recarga Maven después de abrirlo.
2. Configura Eclipse Temurin 25 como Project SDK y como JDK del Maven Runner.
3. Detén la ejecución del proyecto anterior si ocupa el puerto 8080. Ejecutar dos aplicaciones en ese puerto produce un conflicto.
4. En la configuración de ejecución de BancoBanQuitoApplication, configura DB_URL, DB_USER y DB_PASSWORD para tu PostgreSQL. No guardes contraseñas en Git ni las envíes por chat. Estas variables no se transfieren automáticamente de la configuración del proyecto anterior.
5. Si corePrestamos ya contiene el esquema y los datos oficiales, reutilízala. No vuelvas a ejecutar a ciegas los scripts de creación o inserción. Esta migración mantiene `ddl-auto=validate`: no crea ni modifica tablas automáticamente.
6. Ejecuta `backend/src/main/java/com/grupob/bancobanquito/BancoBanQuitoApplication.java`.
7. Abre http://localhost:8080/swagger-ui.html. El documento OpenAPI está en http://localhost:8080/v3/api-docs. La ruta raíz / puede devolver 404 porque este proyecto expone una API.

## Prueba manual desde Swagger

1. Abre la sección Simulación y selecciona GET /api/cliente/simulaciones/parametros, Try it out.
2. Introduce X-Identificacion: 1700000000 y X-Rol: CLIENTE. Execute debe responder 200 con los parámetros vigentes.
3. Ejecuta POST /api/cliente/simulaciones con las mismas cabeceras y este cuerpo:

```json
{
  "montoSolicitado": 10000.00,
  "plazoMeses": 12,
  "sistemaAmortizacion": "FRANCES"
}
```

4. Comprueba 200, 12 cuotas, suma de capital 10000.00 y saldo final 0.00. Cambia únicamente sistemaAmortizacion a ALEMAN y repite.

Con PAR-06=0.1550, PAR-07=0.0006, PAR-08=0.0050 y fecha de negocio 2026-10-31, se esperan:

| Dato | Francés | Alemán |
| --- | ---: | ---: |
| Primera cuota | 906.21 | 960.14 |
| Última cuota | 906.23 | 849.44 |
| Total intereses | 802.54 | 785.25 |
| Seguro mensual | 6.00 | 6.00 |
| Total seguros | 72.00 | 72.00 |
| SOLCA | 50.00 | 50.00 |
| Monto neto | 9950.00 | 9950.00 |
| Costo sin capital | 924.54 | 907.25 |

El primer vencimiento es 2026-11-30 y el último 2027-10-31. Si los parámetros o la fecha cambiaron, cambian los resultados. El ejemplo 8.6 utiliza TEA 0.1677, por lo que su cuota francesa de 911.44 no corresponde a la tasa inicial de 0.1550. Las pruebas automatizadas comprueban ese ejemplo sin modificar la base real.

Pruebas negativas: monto 499 o plazo 61 devuelve 409 con los límites iniciales; plazo 12.5, sistema incorrecto o campos adicionales devuelve 400; sin cabeceras devuelve 401; rol ANALISTA devuelve 403. Para provocar un error de configuración utiliza las pruebas aisladas, sin borrar parámetros reales.

## Prueba manual desde PowerShell

Con el backend iniciado:

```powershell
$cabeceras = @{ 'X-Identificacion' = '1700000000'; 'X-Rol' = 'CLIENTE' }
Invoke-RestMethod -Uri 'http://localhost:8080/api/cliente/simulaciones/parametros' -Headers $cabeceras
$cuerpo = @{ montoSolicitado = 10000; plazoMeses = 12; sistemaAmortizacion = 'FRANCES' } | ConvertTo-Json
$resultado = Invoke-RestMethod -Uri 'http://localhost:8080/api/cliente/simulaciones' -Method Post -Headers $cabeceras -ContentType 'application/json' -Body $cuerpo
$resultado | Select-Object primeraCuota, ultimaCuota, montoNeto, contribucionSolca, costoTotalCredito
$resultado.tablaAmortizacion.cuotas | Format-Table
```

## Pruebas automáticas

En IntelliJ, abre Maven > BancoBanQuito > Lifecycle > test, con JDK 25. Si Maven está en PATH, también puedes ejecutar `mvn test` desde backend. Las bases H2 son solo para pruebas y no sustituyen la validación del esquema de PostgreSQL al arrancar.

## Contrato HTTP

Se conserva la ruta del estándar BanQuito /api/cliente/... para no introducir un versionado unilateral. La versión del contrato de este módulo es 1.0; si el equipo adopta /api/v1, se debe cambiar conjuntamente con los consumidores.

Ambos endpoints requieren X-Identificacion (10 dígitos) y X-Rol: CLIENTE. Son cabeceras de autenticación simulada del alcance académico, no credenciales seguras ni prueba de identidad. No se consulta la existencia del cliente en Pasivos porque la simulación no lee datos personales ni persiste resultados. El acceso real y el inicio de sesión compartido corresponden a P4. En esta migración se entregan únicamente los endpoints; el frontend se integra por separado.

### GET /api/cliente/simulaciones/parametros

Devuelve 200 con fechaNegocio, montoMinimo, montoMaximo, plazoMinimo, plazoMaximo, teaReferencial, factorSeguroMensual y factorSolca. Sirve para que el formulario use límites vigentes sin duplicarlos en JavaScript. No altera la configuración.

### POST /api/cliente/simulaciones

Content-Type: application/json

```json
{
  "montoSolicitado": 10000.00,
  "plazoMeses": 12,
  "sistemaAmortizacion": "FRANCES"
}
```

montoSolicitado es obligatorio, positivo, con hasta 8 dígitos enteros y 2 decimales. plazoMeses es un entero positivo. sistemaAmortizacion admite FRANCES o ALEMAN. El servidor valida además los rangos configurados PAR-01 a PAR-04. No se aceptan campos adicionales ni plazos fraccionarios. El cliente no envía tasas, fecha, seguro ni SOLCA.

Responde 200, no 201, porque no crea un recurso persistente. Incluye fechaNegocio, montoSolicitado, plazoMeses, sistemaAmortizacion, teaReferencial, seguroMensual, contribucionSolca, montoNeto, primeraCuota, ultimaCuota, costoTotalCredito y tablaAmortizacion.

tablaAmortizacion contiene tasaMensual, tasaNominalAnual, totalCapital, totalIntereses, totalSeguros, totalCuotas y cuotas. Cada cuota contiene numeroCuota, fechaVencimiento, valorCapital, valorInteres, valorSeguro, totalCuota y saldoCapitalPosterior. Los estados y rubros pagados de las cuotas persistidas son responsabilidad de operaciones: para un desembolso nuevo se inicializan como PENDIENTE y cero.

Todas las tasas y factores JSON son fracciones: 0.1677 representa 16.77 %. Las tasas derivadas conservan precisión para que otros módulos no calculen con una tasa de presentación redondeada. Los importes son números en USD y se redondean a dos decimales. Las fechas son yyyy-MM-dd, sin zona horaria.

Interpretación explícita de costoTotalCredito: intereses + seguros + SOLCA, excluyendo el capital. totalCuotas incluye capital + intereses + seguros; montoNeto es monto solicitado menos SOLCA. Por tanto costoTotalCredito = totalCuotas - montoNeto. Esta definición evita contar SOLCA dos veces y queda pendiente de ratificación del equipo si utiliza otra terminología.

| Código | Situación |
| --- | --- |
| 200 | Consulta o simulación completada |
| 400 | JSON, identificación, campos o formatos inválidos |
| 401 | Falta identificación o rol |
| 403 | Rol distinto de CLIENTE |
| 409 | Monto o plazo fuera de los parámetros vigentes |
| 503 | Configuración ausente, ambigua, incoherente o acceso a datos fallido |
| 500 | Fallo inesperado, sin detalles internos |

```json
{
  "type": "about:blank",
  "title": "Condiciones no permitidas",
  "status": 409,
  "detail": "El plazo debe estar entre 6 y 60 meses.",
  "instance": "/api/cliente/simulaciones"
}
```

Respuestas exitosas con Cache-Control: no-store. No se configura CORS global. El responsable del frontend debe integrar su proxy o acordar los orígenes permitidos. En publicación se requiere HTTPS y configuración de origen acorde al entorno.

## Uso entre módulos

```java
var condiciones = new CondicionesAmortizacion(
        new BigDecimal("10000.00"), 12, new BigDecimal("0.1677"),
        new BigDecimal("6.00"), LocalDate.of(2026, 10, 31), SistemaAmortizacion.FRANCES);
var tabla = amortizacionService.generarTabla(condiciones);
```

Inyectar la interfaz pública AmortizacionService. No importar clases internal ni entidades de otro módulo. El cálculo acepta condiciones explícitas; el consumidor valida las reglas del caso de uso, la TEA máxima y la autorización. SimulacionServiceImpl aplica los parámetros para RF-01. El seguro entregado a generarTabla ya debe estar expresado en centavos.

Se usa BigDecimal con 34 dígitos significativos para derivar la tasa mensual mediante raíz duodécima, sin double, float ni Math.pow. Los importes se redondean HALF_UP. El francés mantiene capital más interés constante salvo el ajuste final. El alemán mantiene capital fijo y ajusta el residual en la última cuota. Cada vencimiento se obtiene desde la fecha original más el número de meses, conservando el día ancla después de febrero.


## Archivos afectados

- src/main/java/com/grupob/bancobanquito/amortization: API pública del generador, DTO y enums.
- amortization/internal: cálculos, servicio, controlador y manejo de errores.
- parameters: se consume la API pública existente; sus archivos de producción permanecen iguales a main. Solo se adapta el entorno de sus pruebas a H2.
- src/test/java: pruebas migradas y datos aislados para parámetros.
- pom.xml: H2 solo para pruebas.
- src/main/resources/application.properties: validación JSON estricta y activación configurable de Swagger.

El contrato público de Java para otros módulos está en amortization.AmortizacionService. No se debe importar amortization.internal. El frontend podrá consumir el contrato HTTP descrito arriba; no necesita reproducir los cálculos.

## Fuentes de la implementación original

- https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/math/BigDecimal.html
- https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html
- https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
- https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot

## Resultado de verificacion de la migracion

Maven test ejecutado con Temurin 25: 37 pruebas, cero fallos, cero errores. Incluye las cinco pruebas existentes de parametros y la verificacion de modularidad del proyecto destino. Swagger y los endpoints se probaron por HTTP con H2. Los archivos del frontend conservaron sus hashes originales. No se ejecutaron scripts SQL ni se modificaron datos de PostgreSQL. Queda la prueba manual contra PostgreSQL al iniciar este proyecto con la configuracion local del usuario.

## Alineación con main después del pull

Se revisaron HEAD, main y origin/main: las tres referencias apuntaban a f5b8147, que ya integra parameters. La simulación reutiliza ParameterService y BusinessDateService mediante inyección por constructor. No accede a repositorios o entidades internos de parameters desde producción.

Se retiraron los cambios propios a ParameterRepository y ParameterServiceImpl para mantener una única implementación compartida. La prueba de integración comprueba ahora la selección que ofrece main. Los ajustes de ParameterServiceTest son exclusivamente de ejecución aislada y preparación de datos ficticios; sus cinco casos originales se conservan.

La estructura sigue el módulo amortization existente: contratos públicos en la raíz e implementación en internal. No se renombraron rutas HTTP ni campos JSON. Las ramas remotas de otros compañeros aún presentan diferencias de nombres y contratos; no se incorporan hasta su integración o acuerdo. En particular, cesar-loans-installment contiene un contrato provisional AmortizationService distinto de AmortizacionService; habrá que acordar su unificación cuando se integre esa rama, sin registrar dos motores de cálculo independientes.
