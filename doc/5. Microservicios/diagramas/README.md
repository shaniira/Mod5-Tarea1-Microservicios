# Diagramas

`generar-diagrama-microservicios.js` genera `../c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg`, la arquitectura de microservicios tal como quedó implementada (2026-09-28). Los datos del diagrama (servicios, eventos que publica y consume cada uno, redes, puertos) se tomaron del código y del `docker-compose.yml`, no de la propuesta: si cambian, se edita el script y se regenera.

Desde la raíz del repositorio:

```bash
node "doc/5. Microservicios/diagramas/generar-diagrama-microservicios.js"
# PNG con Chrome sin ventana (Windows, Git Bash):
D="$(cygpath -w "$(pwd)/doc/5. Microservicios")"
URL=$(node -e "console.log(require('url').pathToFileURL(process.argv[1]).href)" "$D\c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.svg")
"/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=1 --window-size=1900,1600 --screenshot="$D\c_DIAGRAMA-ARQUITECTURA-MICROSERVICIOS.png" "$URL"
```

`a_DIAGRAMA-ARQUITECTURA-ACTUAL.svg` (el monolito de partida) se dibujó a mano y es histórico: no se regenera.
