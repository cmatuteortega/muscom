#!/usr/bin/env node
/** Partida interactiva por terminal: tú (asiento 0) + bot compañero contra 2 bots. */
import * as readline from 'node:readline/promises';
import { createRng } from '../core/rng.js';
import { applyAction, actors, createGame, viewFor } from '../mus/engine.js';
import { cardName, cardToString } from '../mus/cards.js';
import { decide, chooseSenas, BotLevel } from '../bots/index.js';
import { describeAction } from '../mus/format.js';
import { MusAction } from '../mus/types.js';

const level = (process.argv[2] as BotLevel) || 'normal';
const seed = process.argv[3] || `play-${Date.now()}`;
const rl = readline.createInterface({ input: process.stdin, output: process.stdout, terminal: process.stdin.isTTY });
const lines = rl[Symbol.asyncIterator]();
async function ask(q: string): Promise<string> {
  process.stdout.write(q);
  const r = await lines.next();
  if (r.done) {
    console.log('\n(fin de la entrada)');
    process.exit(0);
  }
  return r.value;
}
const rng = createRng(seed + '-bots');
let s = createGame({}, seed);
const names = ['Tú', 'Rival dcha.', 'Compañero', 'Rival izq.'];
console.log(`Semilla: ${seed}  ·  bots: ${level}`);
let senasDone = 0;
while (s.phase !== 'finPartida') {
  if (s.phase === 'grande' && senasDone !== s.handNumber) {
    senasDone = s.handNumber;
    for (const seat of [1, 2, 3]) for (const sena of chooseSenas(s.hands[seat], 8)) s = applyAction(s, seat, { type: 'sena', sena });
    const ps = viewFor(s, 0).senas.filter((x) => x.seat === 2).map((x) => x.sena);
    console.log(`  (tu compañero te hace señas: ${ps.join(', ')})`);
  }
  const seat = actors(s)[0];
  if (seat === 0) {
    const v = viewFor(s, 0);
    if (v.phase === 'finMano') {
      console.log('\nFin de mano:', v.revealed!.map((h, i) => `${names[i]}: ${h.map(cardToString).join(' ')}`).join(' | '));
      console.log('Tantos:', v.summary!.awards.map((a) => `${a.lance}/${a.kind}: ${a.team === 0 ? 'nos' : 'ellos'} +${a.points}`).join(', '));
      console.log(`Marcador: nosotros ${v.scores[0]} – ellos ${v.scores[1]}`);
    }
    console.log(`\n[${v.phase}] Tus cartas: ${v.hand.map((c, i) => `${i + 1}:${cardName(c)}`).join(', ')}`);
    if (v.betting && v.betting.proposed) console.log(`  Envite en juego: ${v.betting.proposed}${v.betting.ordago ? ' (ÓRDAGO)' : ''}`);
    let action: MusAction | null = null;
    if (v.phase === 'descarte') {
      const ans = await ask('Cartas a descartar (p.ej. 1 3): ');
      const idx = ans.trim().split(/\s+/).map((x) => Number(x) - 1);
      action = { type: 'descarte', cards: idx.map((i) => v.hand[i]).filter((c) => c !== undefined) };
    } else {
      v.legal.forEach((a, i) => console.log(`  ${i + 1}) ${describeAction(a)}`));
      const ans = Number(await ask('Elige: '));
      action = v.legal[ans - 1] ?? v.legal[0];
    }
    try {
      s = applyAction(s, 0, action);
    } catch (e) {
      console.log('✗', (e as Error).message);
    }
  } else {
    const a = decide(viewFor(s, seat), level, rng);
    if (a.type !== 'siguienteMano' && a.type !== 'descarte') console.log(`  ${names[seat]}: ${describeAction(a)}`);
    if (a.type === 'descarte') console.log(`  ${names[seat]} descarta ${a.cards.length}`);
    s = applyAction(s, seat, a);
  }
}
console.log(`\n${s.winner === 0 ? '¡Habéis ganado!' : 'Han ganado ellos.'} ${s.scores[0]}–${s.scores[1]}`);
rl.close();
