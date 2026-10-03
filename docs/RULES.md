# Reglas implementadas (motor v0.1)

El Mus tiene muchas variantes locales. Esto es exactamente lo que hace el motor; **las marcadas con ⚠️ son decisiones a confirmar**.

## Baraja y reparto
- Baraja española de 40 cartas. **8 reyes** por defecto: los treses cuentan como reyes y los doses como ases (configurable a 4 reyes).
- 4 jugadores, parejas enfrentadas (asientos 0-2 contra 1-3). Se juega en sentido antihorario; la **mano** es el jugador a la derecha del que reparte. El reparto rota cada mano.
- Partida a **40 tantos** (configurable). No se juega a vacas/juegos: una partida = un juego. ⚠️

## Mus
- Desde la mano, cada jugador dice *mus* o *no hay mus*. Con un solo "no hay mus" se empiezan los lances.
- Si los cuatro piden mus, todos descartan de 1 a 4 cartas (en cualquier orden) y se reponen empezando por la mano. Si se acaba el mazo, se barajan los descartes.

## Lances (en orden)
1. **Grande** — gana la carta más alta, comparando de mayor a menor (R > C > S > 7 > 6 > 5 > 4 > A).
2. **Chica** — gana la más baja, comparando de menor a mayor.
3. **Pares** — duples (dos parejas, **o cuatro iguales**) > medias (trío) > pares (pareja); dentro de cada categoría, por rango. Las cartas sueltas no desempatan.
4. **Juego** — 31 o más: 31 > 32 > 40 > 37 > 36 > 35 > 34 > 33. Figuras (y treses a 8 reyes) valen 10. Si **nadie** tiene juego se juega al **Punto** (gana el más alto, máximo 30).
- Empates exactos: gana el jugador más cercano a la mano.
- Las declaraciones de pares y juego son automáticas y veraces. Si en pares/juego sólo una pareja lo tiene, no hay envite y esa pareja cobra sus jugadas. En pares/juego sólo hablan quienes lo tienen.

## Envites
- Sin apuesta: *paso*, *envido N* (N ≥ 2) u *órdago*. Si todos pasan, el lance queda **en paso**.
- Ante un envite responde la pareja contraria, empezando por el siguiente en el orden: *quiero*, *no quiero*, *N más* (reenvido) u *órdago*. Si el primero no quiere, **habla su compañero**: basta que uno quiera.
- **No quiero**: la pareja que envidó cobra en el acto el **deje**: 1 si era el primer envite, o lo que estaba envidado antes del último reenvido (p.ej. 2, "5 más", no quiero → 2).
- **Órdago querido**: se enseñan las cartas y quien gana ese lance gana la partida. Órdago no querido: deje como un envite.
- Al órdago sólo se puede querer o no querer.

## Conteo (al final de la mano, en este orden; quien llega primero a 40 gana)
| Lance | En paso | Querido | No querido |
|---|---|---|---|
| Grande / Chica | 1 al ganador | envite al ganador | (deje ya cobrado) |
| Pares | jugadas | envite + jugadas | deje + **jugadas al ganador** ⚠️ |
| Juego | jugadas | envite + jugadas | deje + **jugadas al ganador** ⚠️ |
| Punto | 1 | envite + 1 | deje + 1 al ganador ⚠️ |

- Jugadas de pares: pareja 1, medias 2, duples 3 — se suman las de **los dos** jugadores de la pareja ganadora que tengan pares.
- Jugadas de juego: 31 vale 3, el resto 2 — ídem para los dos jugadores.
- Un deje que alcanza 40 termina la partida en el acto.

## Señas
- Seña = mensaje sólo visible para el compañero (dos/tres reyes, dos/tres ases, duples, medias, 31, ciego). Máximo 3 por jugador y mano. No consumen turno.

## No implementado (a decidir)
- ⚠️ Vacas / juegos / amarracos, "órdago a la mano siguiente", envites "a ciegas", mus corrido, "tres cartas" y otras variantes locales.
- ⚠️ Señas libres (expresiones) vs. lista cerrada; para el online proponemos lista cerrada (evita abuso y facilita moderación).
