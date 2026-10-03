# Arquitectura — Mus online

Estado: **propuesta para validar** (fase 0) + prototipo jugable (fase 1 completa y app Android offline).

## 1. Principios

1. **Un único motor de reglas, puro y determinista** (`packages/mus-engine`, TypeScript sin dependencias). El servidor es la única autoridad: el cliente envía *intenciones*, el servidor valida con el motor y devuelve a cada asiento su **vista redactada** (nunca cartas ajenas).
2. **Reproducibilidad**: `estado = f(config, semilla, acciones)`. La semilla sale de un CSPRNG (`crypto.randomBytes(16)`), se guarda con la partida y permite auditar/reproducir cualquier mano (base para detectar colusión).
3. **Juegos como módulos**: `GameModule<Config, State, Action, View>` (`src/core/game.ts`). Tute, Brisca o Truco serán otros módulos con la misma interfaz; servidor, salas, bots y app no saben de Mus más allá de la vista.
4. **Sin dinero real**: los tantos son sólo puntuación. Ni apuestas, ni premios, ni fichas canjeables, ni cajas de botín.

## 2. Vista general

```
                       ┌──────────────────────── Google Play ─────────────────────────┐
                       │  Billing (suscripción Socio, cosméticos)   RTDN → Pub/Sub ───┼──┐
                       └──────────────────────────────────────────────────────────────┘  │
 ┌────────────┐  HTTPS (REST: auth, perfil, ligas, tienda)   ┌───────────────────────┐   │ push
 │ App Android│ ───────────────────────────────────────────▶ │  api (Fastify)        │◀──┘
 │ Compose    │  WSS (protocolo de juego, JSON versionado)   │  ├ auth / perfiles     │
 │ MVVM+Hilt  │ ◀─────────────────────────────────────────▶ │  ├ ligas / temporadas  │── Play Developer API
 └────────────┘                                             │  ├ billing/entitlements│── Stripe (más tarde)
                                                            │  └ ws gateway          │
                                                            └─────────┬─────────────┘
                                                                      │ room actors (en proceso)
                                    ┌─────────────────────────────────┼─────────────────────┐
                                    │ Redis: salas (owner+snapshot), colas MM (ZSET por rating),│
                                    │ presencia, sesión única por cuenta, rate limit, pub/sub   │
                                    └─────────────────────────────────┼─────────────────────┘
                                                                      │
                                    ┌─────────────────────────────────┴─────────────────────┐
                                    │ PostgreSQL (Drizzle): users, identities, sessions,      │
                                    │ matches(seed), match_actions, ratings, leagues, seasons,│
                                    │ entitlements, purchases, reports, blocks, audit         │
                                    └─────────────────────────────────────────────────────────┘
```

### Decisión: Fastify + `ws` (no Colyseus)

| | Fastify + ws | Colyseus |
|---|---|---|
| Modelo de estado | El del motor (JSON puro, inmutable) | Requiere `@colyseus/schema`, duplicaría el estado del motor |
| Vistas por asiento | Trivial: `view(state, seat)` | Posible con `StateView`/filtros, más acoplado |
| Matchmaking por rating, bots, reconexión 60 s | Propio, sobre Redis | Hay que adaptar el suyo igualmente |
| Escalado | Salas particionadas por nodo + Redis | Necesita su *presence/driver* |
| Dependencia | Mínima, estándar | Framework completo |

Como el motor ya resuelve estado, validación y redacción, Colyseus aporta poco y añade un segundo modelo de estado. **Elegimos Fastify + ws**.

### Decisión: Drizzle (no Prisma)
TS-first, SQL explícito, migraciones SQL versionadas y sin motor binario en runtime (imágenes Docker más ligeras, arranque más rápido).

## 3. Servidor (fases 2, 4, 5, 6)

- **Room actor**: cada sala vive en un único proceso (dueño registrado en Redis con TTL). Cola de acciones serializada → `MusGame.apply` → persistencia → difusión de vistas. Snapshot del estado en Redis tras cada acción (recuperación si cae el nodo) y `match_actions` en Postgres (append-only).
- **Protocolo WS** (JSON, `v:1`): cliente → `{t:"act", room, seq, action}`, `{t:"sena", …}`, `{t:"ping"}`; servidor → `{t:"view", room, rev, view}`, `{t:"event", …}`, `{t:"error", code}`. `seq` idempotente; `rev` monotónico para descartar mensajes viejos tras reconectar.
- **Reconexión**: al cerrarse el socket el asiento queda *ausente*; se espera 60 s (configurable) y un bot toma el asiento con el mismo motor. Si el humano vuelve, recupera su asiento al terminar la acción en curso.
- **Bots**: `@muscom/mus-engine/bots` (3 niveles, sólo reciben la vista del asiento: no pueden hacer trampas).
- **Matchmaking**: ZSET por rating Glicko-2; ventana que se amplía con el tiempo (p.ej. ±100 → ±400 en 30 s); relleno con bots tras `MM_BOT_FILL_SECONDS`.
- **Glicko-2**: por jugador; en partidas por parejas se usa el rating/RD medio de la pareja rival como oponente. Partidas con bots no puntúan (o puntúan contra un rating fijo de bot, configurable).
- **Antiabuso**: rate limit (Fastify + Redis) por IP/cuenta y por socket; penalización por abandono (bloqueo temporal del MM creciente); **una sesión de juego por cuenta** (lock en Redis; un segundo login desplaza al primero).
- **Auth**: Google Sign-In (verificación del ID token) y email (contraseña Argon2id + verificación). Access JWT de 10 min, refresh opaco rotatorio (hash en BD, 30 días, detección de reutilización).
- **Ligas**: liga → miembros (invitación por enlace firmado) → temporadas → clasificación (por puntos de partida y desempate por tantos) → historial.
- **Billing** (fase 5): verificación en servidor con Play Developer API (`purchases.subscriptionsv2.get`, `purchases.products.get`), *acknowledge* en servidor, RTDN vía Pub/Sub push autenticado (JWT de Google). Tabla `entitlements(user_id, sku, source[play|stripe], state[active|grace|on_hold|paused|expired|revoked], expires_at, …)`. La app **nunca** concede derechos: sólo muestra lo que diga el servidor. Stripe Checkout + webhooks escribirá en la misma tabla (`source='stripe'`).
- **Observabilidad**: logs JSON con `pino` (requestId, userId, roomId), Sentry (servidor y app), `/healthz` y `/metrics` (Prometheus).
- **Despliegue**: Docker Compose (`api`, `postgres`, `redis`, `caddy` con TLS) para dev/prod con ficheros `compose.override.yml`; GitHub Actions: test → build imagen → push GHCR → deploy por SSH.
- **Carga** (fase 6): k6 / artillery con clientes WS simulados → objetivo 500 salas simultáneas (2.000 sockets) en un nodo, p95 de acción < 100 ms.

## 4. App Android

- Kotlin + Jetpack Compose + Material 3, MVVM, Hilt, Coroutines/Flow, DataStore, Navigation Compose.
- Módulos objetivo (el prototipo es un único módulo `:app` con los mismos paquetes): `:core:model`, `:core:network` (OkHttp WebSocket con backoff exponencial + jitter y reanudación por `rev`), `:core:data`, `:core:ui` (cartas/tapete), `:feature:game`, `:feature:rooms`, `:feature:leagues`, `:feature:shop`, `:feature:profile`.
- Cartas y tapetes **dibujados con Canvas** (diseño propio; ninguna baraja comercial).
- **Motor en la app**: el prototipo incluye un *port* Kotlin del motor (sólo para jugar offline contra bots). Su equivalencia con el motor TS se garantiza con **vectores de conformidad** (`packages/mus-engine/conformance/vectors.json`, 60 partidas completas + 600 comparaciones + RNG). En el producto online la app no aplica reglas: muestra la vista del servidor. Podemos mantener el port para un "modo práctica" offline o eliminarlo.
- Localización `es` (base), `eu`, `gl`, `ca` (borradores: requieren revisión nativa), modo oscuro, alto contraste, escala de texto propia, `contentDescription` en cartas y estados, regiones *live* para anunciar acciones.
- Push FCM (fase 3/4): invitación a mesa, turno de liga, fin de temporada. Analítica sin PII y sólo con consentimiento (registro, primera partida, D1/D7/D30, vista de paywall, conversión).

## 5. Árbol del monorepo

```
muscom/
├── package.json                 # npm workspaces
├── docs/
│   ├── ARCHITECTURE.md          # este documento
│   └── RULES.md                 # reglas implementadas y variantes a decidir
├── packages/
│   ├── mus-engine/              # ✅ fase 1
│   │   ├── src/core/            # GameModule, RNG (cyrb128+sfc32)
│   │   ├── src/mus/             # cartas, evaluación, motor, tipos, formato
│   │   ├── src/bots/            # bots fácil/normal/difícil (Monte Carlo sobre la vista)
│   │   ├── src/cli/             # simulate, play (terminal), conformance
│   │   ├── test/                # vitest
│   │   └── conformance/vectors.json
│   ├── protocol/                # (fase 2) tipos de mensajes WS + validación (zod)
│   └── glicko2/                 # (fase 4) implementación Glicko-2 + tests
├── server/                      # (fase 2)
│   ├── src/
│   │   ├── app.ts               # Fastify, plugins, rutas
│   │   ├── auth/                # google, email, jwt, refresh
│   │   ├── ws/                  # gateway, sesiones, rate limit
│   │   ├── rooms/               # room actor, reconexión, bots
│   │   ├── matchmaking/         # colas Redis, ampliación de rango, relleno con bots
│   │   ├── leagues/             # ligas, temporadas, clasificación
│   │   ├── billing/             # Play Developer API, RTDN, entitlements, stripe/
│   │   ├── moderation/          # reportes, bloqueos (y chat si lo hay)
│   │   └── db/                  # esquema Drizzle + migraciones
│   ├── test/                    # unit + integración (testcontainers)
│   ├── load/                    # k6 (fase 6)
│   └── Dockerfile
├── android/                     # ✅ prototipo jugable offline
│   ├── app/src/main/java/com/muscom/app/
│   │   ├── engine/              # port Kotlin del motor (conformance-tested)
│   │   ├── data/                # DataStore: ajustes, estadísticas
│   │   ├── feedback/            # sonido y háptica
│   │   └── ui/                  # theme, cards (Canvas), game, home, onboarding, login, profile, settings
│   ├── app/src/test/            # tests motor, conformidad TS↔Kotlin, capturas Paparazzi
│   └── tools/gen_strings.py     # textos es/eu/gl/ca desde una tabla
├── deploy/                      # (fase 2) docker-compose.yml, Caddyfile, .env.example
└── .github/workflows/
    ├── engine.yml               # typecheck, tests, simulador, vectores al día
    └── android.yml              # tests + APK + prerelease "proto-latest"
```

## 6. Fases

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Motor de reglas + tests + simulador CLI con bots | ✅ hecho |
| — | Prototipo Android jugable offline + APK por GitHub Actions | ✅ hecho (adelantado para probar) |
| 2 | Servidor: auth, salas, partida completa en red contra bots y entre humanos | pendiente de OK |
| 3 | App Android online de punta a punta (login, salas por enlace, WS) | — |
| 4 | Matchmaking, Glicko-2, reconexión y ligas | — |
| 5 | Billing, entitlements, RTDN, restaurar compras, Stripe preparado | — |
| 6 | Hardening: carga 500 salas, seguridad, checklist Google Play (Data Safety, IARC, privacidad, borrado de cuenta web+app) | — |
