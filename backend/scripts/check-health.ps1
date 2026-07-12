# ============================================================
# Script de verificacion de salud - Clinica Stella Maris
# Consulta /actuator/health y reporta el estado del sistema
# Requerido por rubrica: Mantenimiento (scripts)
# ============================================================

$HEALTH_URL = "http://localhost:8080/actuator/health"

Write-Host "Verificando estado del sistema..." -ForegroundColor Cyan

try {
    $respuesta = Invoke-RestMethod -Uri $HEALTH_URL -Method Get -TimeoutSec 5

    if ($respuesta.status -eq "UP") {
        Write-Host "[OK] Sistema operativo. Estado: $($respuesta.status)" -ForegroundColor Green
    } else {
        Write-Host "[ALERTA] Sistema con problemas. Estado: $($respuesta.status)" -ForegroundColor Red
    }
} catch {
    Write-Host "[ERROR] No se pudo contactar al backend. Verifique que este corriendo en el puerto 8080." -ForegroundColor Red
    Write-Host "Detalle: $($_.Exception.Message)" -ForegroundColor Yellow
    exit 1
}
