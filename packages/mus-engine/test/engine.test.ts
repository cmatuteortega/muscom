import { describe, expect, it } from 'vitest';
import { applyAction, actors, createGame, lanceWinner, legalActions, manoOf, validate, viewFor } from '../src/mus/engine.js';
import { parseHand } from '../src/mus/cards.js';
import { IllegalActionError } from '../src/core/game.js';
import type { MusState } from '../src/mus/types.js';
import { allPass, passUntilEnd, play, rigged } from './helpers.js';

// Asiento 0 es mano. Parejas: A = {0,2}, B = {1,3}.
const BASE = [
  '12O 12C 1E 4B', // 0: pareja de reyes, 25
  '11O 7C 6E 5B', //  1: nada, 28
  '1O 2C 4E 5O', //   2: pareja de ases (8 reyes), 11
  '10O 10C 7O 6O', // 3: pareja de sotas, 33 (juego)
];

function allCards(s: MusState): number[] {
  return [...s.hands.flat(), ...s.deck, ...s.discardPile].sort((a, b) => a - b);
}

describe('reparto y mus', () => {
  it('reparte 4 cartas por jugador, sin repetir, de forma determinista', () => {
    const a = createGame({}, 'semilla');
    const b = createGame({}, 'semilla');
    const c = createGame({}, 'otra');
    expect(a.hands).toEqual(b.hands);
    expect(a.hands).not.toEqual(c.hands);
    expect(a.hands.every((h) => h.length === 4)).toBe(true);
    expect(allCards(a)).toEqual(Array.from({ length: 40 }, (_, i) => i));
    expect(a.turn).toBe(0);
    expect(a.phase).toBe('mus');
  });

  it('cuatro mus → descarte → reposición y nueva ronda de mus', () => {
    let s = createGame({}, 'mus');
    s = play(s, [[0, 'mus'], [1, 'mus'], [2, 'mus']]);
    expect(s.phase).toBe('mus');
    s = play(s, [[3, 'mus']]);
    expect(s.phase).toBe('descarte');
    expect(actors(s)).toEqual([0, 1, 2, 3]);
    for (const seat of [2, 0, 3, 1]) s = applyAction(s, seat, { type: 'descarte', cards: s.hands[seat].slice(0, seat + 1) });
    expect(s.phase).toBe('mus');
    expect(s.turn).toBe(0);
    expect(s.musRound).toBe(1);
    expect(s.lastDiscardCounts).toEqual([1, 2, 3, 4]);
    expect(s.hands.every((h) => h.length === 4)).toBe(true);
    expect(allCards(s)).toEqual(Array.from({ length: 40 }, (_, i) => i));
  });

  it('si se acaba el mazo se barajan los descartes', () => {
    let s = createGame({}, 'mazo');
    for (let round = 0; round < 8; round++) {
      s = play(s, [[0, 'mus'], [1, 'mus'], [2, 'mus'], [3, 'mus']]);
      for (const seat of [0, 1, 2, 3]) s = applyAction(s, seat, { type: 'descarte', cards: [...s.hands[seat]] });
      expect(allCards(s)).toEqual(Array.from({ length: 40 }, (_, i) => i));
      expect(new Set(s.hands.flat()).size).toBe(16);
    }
  });

  it('no hay mus abre la grande con la mano', () => {
    let s = createGame({}, 'x');
    s = play(s, [[0, 'mus'], [1, 'noMus']]);
    expect(s.phase).toBe('grande');
    expect(s.turn).toBe(0);
  });

  it('el descarte valida cartas y turno', () => {
    let s = createGame({}, 'v');
    s = play(s, [[0, 'mus'], [1, 'mus'], [2, 'mus'], [3, 'mus']]);
    expect(validate(s, 0, { type: 'descarte', cards: [] })).toBeTruthy();
    expect(validate(s, 0, { type: 'descarte', cards: [s.hands[1][0]] })).toBeTruthy();
    expect(validate(s, 0, { type: 'descarte', cards: [s.hands[0][0], s.hands[0][0]] })).toBeTruthy();
    s = applyAction(s, 0, { type: 'descarte', cards: [s.hands[0][0]] });
    expect(validate(s, 0, { type: 'descarte', cards: [s.hands[0][0]] })).toBe('Ya has descartado');
  });
});

describe('lances en paso y conteo', () => {
  it('todos pasan: grande y chica valen 1, pares y juego sus jugadas', () => {
    let s = rigged(BASE);
    s = allPass(s); // grande
    expect(s.phase).toBe('chica');
    s = allPass(s); // chica
    expect(s.phase).toBe('pares');
    expect(s.betting!.participants).toEqual([0, 2, 3]);
    expect(s.declarations.pares).toEqual([true, false, true, true]);
    s = allPass(s); // pares; juego sólo lo tiene la pareja B → sin apuesta
    expect(s.phase).toBe('finMano');
    expect(s.lances.map((l) => `${l.lance}:${l.outcome}`)).toEqual([
      'grande:paso', 'chica:paso', 'pares:paso', 'juego:sinApuesta',
    ]);
    expect(s.summary!.winners).toEqual({ grande: 0, chica: 2, pares: 0, juego: 3 });
    expect(s.awards).toEqual([
      { lance: 'grande', team: 0, points: 1, kind: 'paso' },
      { lance: 'chica', team: 0, points: 1, kind: 'paso' },
      { lance: 'pares', team: 0, points: 2, kind: 'jugada' },
      { lance: 'juego', team: 1, points: 2, kind: 'jugada' },
    ]);
    expect(s.scores).toEqual([4, 2]);
  });

  it('sin juego se juega al punto (vale 1)', () => {
    let s = rigged(['12O 12C 1E 4B', '11O 7C 6E 5B', '1O 2C 4E 5O', '10O 7O 6O 4O']);
    s = allPass(allPass(s));
    // pares sólo en la pareja A → no se apuesta; nadie tiene juego → punto
    expect(s.phase).toBe('punto');
    s = allPass(s);
    expect(s.phase).toBe('finMano');
    expect(s.summary!.winners.punto).toBe(1);
    expect(s.awards.filter((a) => a.lance === 'punto')).toEqual([{ lance: 'punto', team: 1, points: 1, kind: 'jugada' }]);
    expect(s.awards.filter((a) => a.lance === 'pares')).toEqual([{ lance: 'pares', team: 0, points: 2, kind: 'jugada' }]);
  });

  it('juego: la 31 vale 3, el resto 2, y cuenta el del compañero', () => {
    let s = rigged(['12O 11C 10E 1B', '7C 6E 5B 4O', '12E 11E 10C 6O', '7O 6C 5C 4C']);
    s = passUntilEnd(s);
    expect(s.lances.find((l) => l.lance === 'juego')!.outcome).toBe('sinApuesta');
    expect(s.awards.filter((a) => a.lance === 'juego')).toEqual([{ lance: 'juego', team: 0, points: 5, kind: 'jugada' }]);
  });

  it('juego querido: apuesta + jugadas para el ganador', () => {
    let s = rigged(['12O 11C 10E 1B', '12C 11O 10O 3O', '12E 11E 10C 6O', '7O 6C 5C 4C']);
    s = allPass(allPass(s));
    if (s.phase === 'pares') s = allPass(s);
    expect(s.phase).toBe('juego');
    expect(s.betting!.participants).toEqual([0, 1, 2]);
    s = play(s, [[0, 'envido2'], [1, 'quiero']]);
    expect(s.phase).toBe('finMano');
    expect(s.awards.filter((a) => a.lance === 'juego')).toEqual([
      { lance: 'juego', team: 0, points: 2, kind: 'apuesta' },
      { lance: 'juego', team: 0, points: 5, kind: 'jugada' },
    ]);
  });
});

describe('envites', () => {
  it('envido no querido: deje de 1 inmediato', () => {
    let s = rigged(BASE);
    s = play(s, [[0, 'envido2']]);
    expect(s.betting!.queue).toEqual([1, 3]);
    s = play(s, [[1, 'noQuiero']]);
    expect(s.turn).toBe(3); // el compañero aún puede querer
    s = play(s, [[3, 'noQuiero']]);
    expect(s.phase).toBe('chica');
    expect(s.scores).toEqual([1, 0]);
    expect(s.lances[0]).toEqual({ lance: 'grande', outcome: 'noQuerido', amount: 1, team: 0, ordago: false });
  });

  it('reenvido no querido: se cobra lo envidado antes', () => {
    let s = rigged(BASE);
    s = play(s, [[0, 'envido2'], [1, 'envido5']]);
    expect(s.betting!.proposed).toBe(7);
    expect(s.betting!.queue).toEqual([2, 0]);
    s = play(s, [[2, 'noQuiero'], [0, 'noQuiero']]);
    expect(s.scores).toEqual([0, 2]);
  });

  it('el compañero puede querer aunque el primero no quiera', () => {
    let s = rigged(BASE);
    s = play(s, [[0, 'envido2'], [1, 'noQuiero'], [3, 'quiero']]);
    expect(s.lances[0]).toMatchObject({ outcome: 'querido', amount: 2 });
    s = passUntilEnd(s);
    expect(s.awards[0]).toEqual({ lance: 'grande', team: 0, points: 2, kind: 'apuesta' });
  });

  it('pares no queridos: deje para quien envida, jugadas para quien gana', () => {
    let s = allPass(allPass(rigged(BASE)));
    s = play(s, [[0, 'paso'], [2, 'paso'], [3, 'envido2']]);
    expect(s.betting!.queue).toEqual([0, 2]);
    s = play(s, [[0, 'noQuiero'], [2, 'noQuiero']]);
    expect(s.phase).toBe('finMano');
    expect(s.awards.filter((a) => a.lance === 'pares')).toEqual([
      { lance: 'pares', team: 1, points: 1, kind: 'deje' },
      { lance: 'pares', team: 0, points: 2, kind: 'jugada' },
    ]);
  });

  it('quien no tiene pares no habla en pares', () => {
    const s = allPass(allPass(rigged(BASE)));
    expect(s.turn).toBe(0);
    expect(() => applyAction(s, 1, { type: 'paso' })).toThrow(IllegalActionError);
    const s2 = applyAction(s, 0, { type: 'paso' });
    expect(s2.turn).toBe(2);
  });
});

describe('órdago', () => {
  it('órdago querido: el ganador del lance gana la partida', () => {
    const s = play(rigged(BASE, {}, [12, 30]), [[0, 'ordago'], [1, 'quiero']]);
    expect(s.phase).toBe('finPartida');
    expect(s.winner).toBe(0);
    expect(s.scores).toEqual([40, 30]);
    expect(s.summary!.gameOver).toBe(true);
    expect(s.summary!.winners).toEqual({ grande: 0 });
  });
  it('órdago querido a chica lo gana quien tiene la chica', () => {
    let s = allPass(rigged(['12O 12C 11E 4B', '1O 1C 4E 5B', '7O 7C 6E 6B', '10O 10C 7E 6O']));
    s = play(s, [[0, 'ordago'], [1, 'quiero']]);
    expect(s.winner).toBe(1);
  });
  it('órdago no querido: deje de 1 y sigue la mano', () => {
    const s = play(rigged(BASE), [[0, 'ordago'], [1, 'noQuiero'], [3, 'noQuiero']]);
    expect(s.phase).toBe('chica');
    expect(s.scores).toEqual([1, 0]);
    expect(s.lances[0]).toMatchObject({ outcome: 'noQuerido', amount: 1, ordago: true });
  });
  it('órdago sobre envido no querido: se cobra el envido', () => {
    const s = play(rigged(BASE), [[0, 'envido2'], [1, 'ordago'], [2, 'noQuiero'], [0, 'noQuiero']]);
    expect(s.scores).toEqual([0, 2]);
  });
  it('al órdago no se puede reenvidar', () => {
    const s = play(rigged(BASE), [[0, 'ordago']]);
    expect(legalActions(s, 1)).toEqual([{ type: 'quiero' }, { type: 'noQuiero' }]);
    expect(() => applyAction(s, 1, { type: 'envido', amount: 2 })).toThrow(IllegalActionError);
    expect(() => applyAction(s, 1, { type: 'ordago' })).toThrow(IllegalActionError);
  });
});

describe('fin de partida', () => {
  it('se cobra en orden: quien llega antes a 40 gana aunque el otro sumara más', () => {
    const s = passUntilEnd(rigged(BASE, {}, [38, 38]));
    expect(s.phase).toBe('finPartida');
    expect(s.winner).toBe(0);
    expect(s.scores).toEqual([40, 38]); // el juego de B no llega a cobrarse
  });
  it('un deje que llega a 40 acaba la partida al instante', () => {
    const s = play(rigged(BASE, {}, [39, 0]), [[0, 'envido2'], [1, 'noQuiero'], [3, 'noQuiero']]);
    expect(s.phase).toBe('finPartida');
    expect(s.winner).toBe(0);
    expect(actors(s)).toEqual([]);
    expect(() => applyAction(s, 0, { type: 'siguienteMano' })).toThrow();
  });
  it('partida a otro número de tantos', () => {
    const s = passUntilEnd(rigged(BASE, { target: 3 }));
    expect(s.winner).toBe(0);
    expect(s.scores).toEqual([3, 0]);
  });
});

describe('desempates y rotación', () => {
  it('a igualdad gana la mano', () => {
    const hands = ['12O 11O 4O 5O', '12C 11C 4C 5C', '7E 6E 6B 7B', '7O 6O 4E 5E'].map(parseHand);
    const s = createGame({ firstDealer: 3 }, 't');
    s.hands = hands;
    expect(lanceWinner(s, 'grande')).toBe(0);
    s.dealer = 0; // mano = 1
    expect(lanceWinner(s, 'grande')).toBe(1);
  });
  it('siguiente mano: el repartidor pasa a la derecha y cambia la mano', () => {
    let s = passUntilEnd(rigged(BASE));
    expect(s.phase).toBe('finMano');
    expect(actors(s)).toEqual([0, 1, 2, 3]);
    s = applyAction(s, 2, { type: 'siguienteMano' });
    expect(s.handNumber).toBe(2);
    expect(s.dealer).toBe(0);
    expect(manoOf(s.dealer)).toBe(1);
    expect(s.turn).toBe(1);
    expect(s.scores).toEqual([4, 2]);
    expect(s.awards).toEqual([]);
  });
});

describe('acciones ilegales', () => {
  const s = rigged(BASE);
  it.each([
    ['fuera de turno', 1, { type: 'paso' }],
    ['quiero sin apuesta', 0, { type: 'quiero' }],
    ['envido de 1', 0, { type: 'envido', amount: 1 }],
    ['envido no entero', 0, { type: 'envido', amount: 2.5 }],
    ['mus en lance', 0, { type: 'mus' }],
    ['siguiente mano antes de tiempo', 0, { type: 'siguienteMano' }],
    ['descarte en lance', 0, { type: 'descarte', cards: [1] }],
    ['acción desconocida', 0, { type: 'trampa' }],
    ['seña desconocida', 0, { type: 'sena', sena: 'chuleta' }],
    ['asiento inválido', 7, { type: 'paso' }],
  ])('%s', (_name, seat, action) => {
    expect(() => applyAction(s, seat as number, action as never)).toThrow(IllegalActionError);
  });
  it('paso con apuesta pendiente', () => {
    const t = play(s, [[0, 'envido2']]);
    expect(() => applyAction(t, 1, { type: 'paso' })).toThrow(IllegalActionError);
  });
  it('applyAction no muta el estado original', () => {
    const before = JSON.stringify(s);
    applyAction(s, 0, { type: 'envido', amount: 2 });
    expect(JSON.stringify(s)).toBe(before);
  });
});

describe('vista y señas', () => {
  it('la vista no contiene cartas ajenas hasta el final de la mano', () => {
    const s = createGame({}, 'vista');
    const v = viewFor(s, 1);
    expect(v.hand).toEqual(s.hands[1]);
    expect(v.revealed).toBeNull();
    const json = JSON.stringify(v);
    expect(json).not.toContain('"hands"');
    expect(json).not.toContain('"deck"');
    expect(json).not.toContain('"rng"');
    expect(json).not.toContain('"seed"');
  });
  it('las señas sólo las ve el compañero', () => {
    let s = createGame({}, 'senas');
    s = applyAction(s, 1, { type: 'sena', sena: 'duples' });
    s = applyAction(s, 2, { type: 'sena', sena: 'treintaYUna' });
    expect(viewFor(s, 0).senas).toEqual([{ seat: 2, sena: 'treintaYUna' }]);
    expect(viewFor(s, 3).senas).toEqual([{ seat: 1, sena: 'duples' }]);
    expect(viewFor(s, 0).log.some((e) => e.action.type === 'sena' && e.seat === 1)).toBe(false);
    expect(s.turn).toBe(0); // las señas no consumen turno
  });
  it('límite de señas por mano', () => {
    let s = createGame({ maxSenasPerHand: 2 }, 'flood');
    s = play(s, [[0, { type: 'sena', sena: 'ciego' }], [0, { type: 'sena', sena: 'ciego' }]]);
    expect(() => applyAction(s, 0, { type: 'sena', sena: 'ciego' })).toThrow(IllegalActionError);
  });
  it('la vista incluye las acciones legales del jugador', () => {
    const s = rigged(BASE);
    expect(viewFor(s, 0).legal.map((a) => a.type)).toEqual(['paso', 'envido', 'envido', 'ordago']);
    expect(viewFor(s, 1).legal).toEqual([]);
  });
});
