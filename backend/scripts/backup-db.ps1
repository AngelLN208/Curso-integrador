# ============================================================
# Script de Backup - Clinica Stella Maris
# Respalda la base de datos PostgreSQL y aplica retencion de 30 dias
# Requerido por rubrica: Mantenimiento (backups)
# ============================================================

$PG_DUMP     = "C:\Program Files\PostgreSQL\18\bin\pg_dump.exe"
$DB_NAME     = "clinica_stella_maris"
$DB_USER     = "postgres"
$DB_HOST     = "localhost"
$DB_PORT     = "5432"
$BACKUP_DIR  = "$PSScriptRoot\..\backups"
$RETENTION_DAYS = 30

$env:PGPASSWORD = "2020"

if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
}

$fecha = Get-Date -Format "yyyy-MM-dd_HHmmss"
$archivoBackup = "$BACKUP_DIR\clinica_stella_maris_$fecha.sql"
$logFile = "$BACKUP_DIR\backup.log"

function Escribir-Log {
    param([string]$mensaje)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $linea = "$timestamp - $mensaje"
    Add-Content -Path $logFile -Value $linea
    Write-Host $linea
}

Escribir-Log "Iniciando backup de la base de datos $DB_NAME..."

try {
    & $PG_DUMP -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -F p -f $archivoBackup

    if ($LASTEXITCODE -eq 0) {
        $tamano = (Get-Item $archivoBackup).Length / 1KB
        Escribir-Log ("Backup completado exitosamente: {0} ({1:N1} KB)" -f $archivoBackup, $tamano)
    } else {
        Escribir-Log "ERROR: pg_dump finalizo con codigo de salida $LASTEXITCODE"
        exit 1
    }
} catch {
    Escribir-Log "ERROR: Excepcion durante el backup - $($_.Exception.Message)"
    exit 1
} finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

$limite = (Get-Date).AddDays(-$RETENTION_DAYS)
$backupsAntiguos = Get-ChildItem -Path $BACKUP_DIR -Filter "clinica_stella_maris_*.sql" |
    Where-Object { $_.LastWriteTime -lt $limite }

if ($backupsAntiguos.Count -gt 0) {
    foreach ($backup in $backupsAntiguos) {
        Remove-Item $backup.FullName -Force
        Escribir-Log "Backup antiguo eliminado (retencion $RETENTION_DAYS dias): $($backup.Name)"
    }
} else {
    Escribir-Log "No hay backups antiguos para eliminar."
}

Escribir-Log "Proceso de backup finalizado."
