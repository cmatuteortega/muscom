#!/usr/bin/env node
/**
 * Simulador: enfrenta parejas de bots y saca estadísticas.
 *   node dist/cli/simulate.js --games 500 --a dificil --b normal --seed demo
 */
import { createRng } from '../core/rng.js';
import { applyAction, actors, createGame, viewFor } from '../mus/engine.js';
import { teamOf } from '../mus/types.js';
import { BotLevel, chooseSenas, decide } from '../bots/index.js';
import { encodeAction } from '../mus/format.js';
import { cardToString } from '../mus/cards.js';

function arg(name: string, def: string): string {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}

const games = Number(arg('games', '200'));
const levelA = arg('a', 'dificil') as BotLevel; // pareja 0 (asientos 0 y 2)
const levelB = arg('b', 'normal') as BotLevel; // pareja 1 (asientos 1 y 3)
const seedBase = arg('seed', 'sim');
const verbose = process.argv.includes('--verbose');

const stats = { wins: [0, 0], hands: 0, ordagos: 0, mus: 0, actions: 0, maxActions: 0 };
const t0 = Date.now();
for (let g = 0; g < games; g++) {
  const seed = `${seedBase}-${g}`;
  const rng = createRng(`${seed}-bots`);
  let s = createGame({ firstDealer: (g % 4) as 0 }, seed);
  let lastHand = 0;
  let n = 0;
  while (s.phase !== 'finPartida') {
    if (s.handNumber !== lastHand) {
      lastHand = s.handNumber;
      stats.hands++;
      if (verbose) console.log(`\n— Mano ${s.handNumber} — ${s.hands.map((h) => h.map(cardToString).join(' ')).join(' | ')}`);
    }
    if (s.phase === 'grande' && s.betting && s.betting.queue.length === s.betting.participants.length && s.senas.length === 0) {
      for (const seat of [0, 1, 2, 3]) for (const sena of chooseSenas(s.hands[seat], s.config.reyes)) s = applyAction(s, seat, { type: 'sena', sena });
    }
    const seat = actors(s)[0];
    const level = teamOf(seat) === 0 ? levelA : levelB;
    const a = decide(viewFor(s, seat), level, rng);
    if (a.type === 'mus') stats.mus++;
    if (a.type === 'ordago') stats.ordagos++;
    if (verbose) console.log(`  [${s.phase}] J${seat}: ${encodeAction(a)}`);
    s = applyAction(s, seat, a);
    if (++n > 5000) throw new Error(`Partida ${seed} no termina`);
  }
  stats.actions += n;
  stats.maxActions = Math.max(stats.maxActions, n);
  stats.wins[s.winner!]++;
  if (verbose) console.log(`Partida ${g}: gana pareja ${s.winner} ${s.scores.join('-')}`);
}
const dt = (Date.now() - t0) / 1000;
console.log(`Partidas: ${games}  (${levelA} vs ${levelB})  en ${dt.toFixed(1)} s`);
console.log(`Victorias pareja A (${levelA}): ${stats.wins[0]} (${((100 * stats.wins[0]) / games).toFixed(1)}%)`);
console.log(`Victorias pareja B (${levelB}): ${stats.wins[1]} (${((100 * stats.wins[1]) / games).toFixed(1)}%)`);
console.log(`Manos/partida: ${(stats.hands / games).toFixed(1)}  acciones/partida: ${(stats.actions / games).toFixed(0)} (máx ${stats.maxActions})  órdagos: ${stats.ordagos}`);
