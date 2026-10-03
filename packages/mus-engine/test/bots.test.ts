import { describe, expect, it } from 'vitest';
import { createRng } from '../src/core/rng.js';
import { applyAction, actors, createGame, viewFor } from '../src/mus/engine.js';
import { BOT_LEVELS, BotLevel, chooseDiscard, chooseSenas, decide, estimate } from '../src/bots/index.js';
import { parseHand } from '../src/mus/cards.js';
import type { MusState } from '../src/mus/types.js';
import { teamOf } from '../src/mus/types.js';
import { rigged } from './helpers.js';

function runGame(seed: string, a: BotLevel, b: BotLevel, reyes: 4 | 8 = 8): MusState {
  const rng = createRng(seed + 'b');
  let s = createGame({ reyes }, seed);
  let n = 0;
  while (s.phase !== 'finPartida') {
    if (s.phase === 'grande' && s.senas.length === 0)
      for (const seat of [0, 1, 2, 3]) for (const sena of chooseSenas(s.hands[seat], reyes)) s = applyAction(s, seat, { type: 'sena', sena });
    const seat = actors(s)[0];
    s = applyAction(s, seat, decide(viewFor(s, seat), teamOf(seat) === 0 ? a : b, rng));
    if (++n > 3000) throw new Error('no termina');
  }
  return s;
}

describe('bots', () => {
  it.each(BOT_LEVELS.map((l) => [l]))('nivel %s juega partidas completas legales', (level) => {
    for (let i = 0; i < 4; i++) {
      const s = runGame(`bot-${level}-${i}`, level as BotLevel, 'facil', i % 2 ? 4 : 8);
      expect(s.winner).not.toBeNull();
      expect(Math.max(...s.scores)).toBe(40);
    }
  });

  it('una partida se reproduce exactamente desde semilla + historial', () => {
    const s = runGame('replay', 'normal', 'dificil');
    let r = createGame({}, s.seed);
    for (const e of s.history) r = applyAction(r, e.seat, e.action);
    expect(r).toEqual(s);
  });

  it('el descarte guarda parejas y reyes', () => {
    const rng = createRng('d');
    expect(chooseDiscard(parseHand('12O 3C 5E 6B'), 8, rng, 'normal').sort()).toEqual(parseHand('5E 6B').sort());
    expect(chooseDiscard(parseHand('7O 7C 5E 12B'), 8, rng, 'normal')).toEqual(parseHand('5E'));
    expect(chooseDiscard(parseHand('12O 12C 12E 12B'), 8, rng, 'normal')).toHaveLength(1);
  });

  it('las señas reflejan la mano', () => {
    expect(chooseSenas(parseHand('12O 11C 10E 1B'), 8)).toContain('treintaYUna');
    expect(chooseSenas(parseHand('12O 3C 7E 7B'), 8)).toEqual(['duples', 'dosReyes']);
    expect(chooseSenas(parseHand('4O 5C 6E 7B'), 8)).toEqual(['ciego']);
  });

  it('la estimación favorece a manos fuertes', () => {
    const s = rigged(['12O 12C 3E 3B', '4O 5C 6E 7B', '4C 5E 6B 7O', '1O 1C 2E 4B']);
    const est = estimate(viewFor(s, 0), createRng('e'), 300, false);
    expect(est.grande).toBeGreaterThan(0.9);
    expect(est.pares).toBeGreaterThan(0.9);
  });

  it('el bot difícil gana más que el fácil', () => {
    let wins = 0;
    const N = 24;
    for (let i = 0; i < N; i++) if (runGame(`vs-${i}`, 'dificil', 'facil').winner === 0) wins++;
    expect(wins / N).toBeGreaterThan(0.5);
  }, 60_000);
});
