@echo off
echo Iniciando servidor de prueba...
echo.
cd /d "F:\Projects\pixcodev\Minecraft\InfinityPixel\Custom mods\forge-infinixmc-protectionblocks\run"

echo Verificando archivos necesarios...
if not exist "server.jar" (
    echo Error: server.jar no encontrado
    pause
    exit /b 1
)

if not exist "mods" mkdir mods
copy "..\build\libs\protectionblocks-1.0.0.jar" "mods\" /Y > nul

echo Iniciando servidor Minecraft con el mod...
java -Xmx2G -Xms1G -jar server.jar nogui

echo.
echo Servidor terminado.
pause