# Mus online (monorepo)

Mus por parejas a 8 reyes, partida a 40, con motor de reglas propio, servidor autoritativo (fase 2) y app Android nativa.

- **Arquitectura y fases:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Reglas exactas implementadas (y variantes a decidir):** [docs/RULES.md](docs/RULES.md)

## Probar el prototipo en Android
Cada push que toca `android/` ejecuta el workflow **android-apk**, que:
1. Pasa los tests del motor Kotlin, incluida la conformidad con el motor TypeScript.
2. Compila la APK de depuración.
3. La publica como artefacto y en el prerelease **`proto-latest`** (pestaña *Releases* del repo), descargable directamente desde el móvil.

Instalación: descarga `mus-proto-N.apk` en el móvil (Android 8+), permite "instalar apps de orígenes desconocidos" y ábrela. Todas las APK de prueba se firman con la misma clave de depuración, así que se actualizan sin desinstalar.

## Motor de reglas (`packages/mus-engine`)
```bash
npm ci
npm test -w @muscom/mus-engine                 # 68 tests
npm run simulate -- --games 500 --a dificil --b normal
npm run play -w @muscom/mus-engine -- normal    # partida por terminal
npm run conformance -w @muscom/mus-engine       # regenera vectores para el port Kotlin
```

## App Android (`android/`)
```bash
cd android
./gradlew :app:testDebugUnitTest      # motor + conformidad + capturas Paparazzi
./gradlew :app:assembleDebug          # APK en app/build/outputs/apk/debug/
./gradlew :app:recordPaparazziDebug   # regenera capturas en app/src/test/snapshots/
python3 tools/gen_strings.py          # regenera textos es/eu/gl/ca
```
