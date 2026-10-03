#!/usr/bin/env node
/**
 * Genera vectores de conformidad para otras implementaciones del motor (p.ej. el
 * port Kotlin de la app). Una política aleatoria determinista elige entre las
 * acciones legales canónicas; si otra implementación difiere en reglas, orden de
 * acciones o RNG, diverge y su test falla.
 */
import { nextInt, rngFromSeed } from '../core/rng.js';
import { applyAction, actors, createGame, legalActions } from '../mus/engine.js';
import { encodeAction } from '../mus/format.js';
import { compareChica, compareGrande, compareJuego, comparePares, comparePunto, handPoints, paresOf } from '../mus/evaluate.js';

const rngVectors = ['a', 'mus', 'semilla-123', ''].map((seed) => {
  const s = rngFromSeed(seed);
  return { seed, ints: Array.from({ length: 8 }, () => nextInt(s, 1000)) };
});

const evalVectors: unknown[] = [];
{
  const s = rngFromSeed('eval');
  for (let i = 0; i < 300; i++) {
    const deck = Array.from({ length: 40 }, (_, k) => k);
    for (let k = deck.length - 1; k > 0; k--) {
      const j = nextInt(s, k + 1);
      [deck[k], deck[j]] = [deck[j], deck[k]];
    }
    const a = deck.slice(0, 4);
    const b = deck.slice(4, 8);
    for (const reyes of [8, 4] as const) {
      evalVectors.push({
        a, b, reyes,
        grande: Math.sign(compareGrande(a, b, reyes)),
        chica: Math.sign(compareChica(a, b, reyes)),
        pares: Math.sign(comparePares(a, b, reyes)),
        juego: Math.sign(compareJuego(a, b, reyes)),
        punto: Math.sign(comparePunto(a, b, reyes)),
        paresA: paresOf(a, reyes),
        puntosA: handPoints(a, reyes),
      });
    }
  }
}

const games: unknown[] = [];
for (let g = 0; g < 60; g++) {
  const seed = `conf-${g}`;
  const noOrdago = g % 2 === 0;
  const policy = rngFromSeed(`policy-${g}`);
  let s = createGame({ firstDealer: (g % 4) as 0, reyes: g % 5 === 4 ? 4 : 8 }, seed);
  const actions: string[] = [];
  const handScores: number[][] = [];
  let lastHand = s.handNumber;
  let steps = 0;
  while (s.phase !== 'finPartida' && steps < 3000) {
    const seat = actors(s)[0];
    let legal = legalActions(s, seat);
    if (noOrdago) legal = legal.filter((a) => a.type !== 'ordago');
    const a = legal[nextInt(policy, legal.length)];
    actions.push(`${seat}|${encodeAction(a)}`);
    s = applyAction(s, seat, a);
    if (s.handNumber !== lastHand) {
      lastHand = s.handNumber;
    }
    if (s.phase === 'finMano' || s.phase === 'finPartida') handScores.push([...s.scores]);
    steps++;
  }
  games.push({
    seed,
    policySeed: `policy-${g}`,
    noOrdago,
    reyes: s.config.reyes,
    firstDealer: g % 4,
    actions,
    handScores,
    finalScores: s.scores,
    winner: s.winner,
    hands: s.handNumber,
  });
}

process.stdout.write(JSON.stringify({ version: 1, rng: rngVectors, eval: evalVectors, games }) + '\n');
