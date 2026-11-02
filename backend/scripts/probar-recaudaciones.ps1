param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$MavenCommand = 'mvn.cmd'
)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))

$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('prueba-postgres-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$dataDirectory = Join-Path $testRoot 'data'
$serverLog = Join-Path $testRoot 'postgres.log'
$started = $false
try {
    & "$postgresBin\initdb.exe" -D $dataDirectory -U postgres --auth-local=trust --auth-host=trust --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible inicializar PostgreSQL temporal.' }
    & "$postgresBin\pg_ctl.exe" -D $dataDirectory -l $serverLog -o '-p 55439 -h 127.0.0.1' -w start
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible iniciar PostgreSQL temporal.' }
    $started = $true
    & "$postgresBin\createdb.exe" -h 127.0.0.1 -p 55439 -U postgres banquito_recaudaciones_test
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible crear la base temporal.' }
    & "$postgresBin\psql.exe" -h 127.0.0.1 -p 55439 -U postgres -d banquito_recaudaciones_test -v ON_ERROR_STOP=1 -f "$projectRoot\database\corePrestamos.sql"
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible cargar el esquema existente en la base temporal.' }
    & "$postgresBin\psql.exe" -h 127.0.0.1 -p 55439 -U postgres -d banquito_recaudaciones_test -v ON_ERROR_STOP=1 -f "$projectRoot\backend\src\test\resources\cobranza\preparacion-persistencia.sql"
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible preparar los datos ficticios.' }
    $env:JAVA_HOME=$JdkHome
    $env:RECAUDACIONES_TEST_URL='jdbc:postgresql://127.0.0.1:55439/banquito_recaudaciones_test'
    Push-Location "$projectRoot\backend"
    try {
        & $MavenCommand '-Dtest=LectorArchivoRecaudacionTest,ValidadorRegistroRecaudacionTest,PersistenciaRecaudacionesTest' test
        if ($LASTEXITCODE -ne 0) { throw 'Las pruebas de recaudaciones fallaron.' }
    } finally {
        Pop-Location
    }
} finally {
    if ($started) {
        & "$postgresBin\pg_ctl.exe" -D $dataDirectory -m fast -w stop
    }
    Write-Output "Archivos de diagnóstico: $testRoot"
}
