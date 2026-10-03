import { applyAction, createGame } from '../src/mus/engine.js';
import { parseHand } from '../src/mus/cards.js';
import type { MusAction, MusConfig, MusState } from '../src/mus/types.js';

/** Partida con manos amañadas (asiento 0 es mano) y el mus ya cortado: empieza la grande. */
export function rigged(hands: string[], cfg: Partial<MusConfig> = {}, scores: [number, number] = [0, 0]): MusState {
  const s = createGame({ firstDealer: 3, ...cfg }, 'rigged');
  s.hands = hands.map(parseHand);
  s.scores = [...scores];
  return applyAction(s, 0, { type: 'noMus' });
}

export type Step = [number, MusAction] | [number, string];

const short: Record<string, MusAction> = {
  paso: { type: 'paso' },
  quiero: { type: 'quiero' },
  noQuiero: { type: 'noQuiero' },
  ordago: { type: 'ordago' },
  mus: { type: 'mus' },
  noMus: { type: 'noMus' },
};

export function play(s: MusState, steps: Step[]): MusState {
  for (const [seat, a] of steps) {
    let action: MusAction;
    if (typeof a === 'string') {
      const m = /^envido(\d+)$/.exec(a);
      action = m ? { type: 'envido', amount: Number(m[1]) } : short[a];
      if (!action) throw new Error(`acción ${a}`);
    } else action = a;
    s = applyAction(s, seat, action);
  }
  return s;
}

/** Los cuatro pasan en el lance actual. */
export function allPass(s: MusState): MusState {
  while (s.betting && s.betting.proposed === 0 && !s.betting.ordago) {
    const lance = s.phase;
    s = applyAction(s, s.turn!, { type: 'paso' });
    if (s.phase !== lance) break;
  }
  return s;
}

export function passUntilEnd(s: MusState): MusState {
  while (s.betting) s = allPass(s);
  return s;
}
