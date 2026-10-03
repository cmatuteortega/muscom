import { describe, expect, it } from 'vitest';
import { parseHand, effRank, pointValue, parseCard, cardToString, fullDeck, makeCard } from '../src/mus/cards.js';
import {
  bestSeat, compareChica, compareGrande, compareJuego, comparePares, comparePunto,
  handPoints, hasJuego, juegoPoints, juegoRank, paresOf,
} from '../src/mus/evaluate.js';

const H = parseHand;

describe('cartas', () => {
  it('baraja de 40 cartas únicas con notación reversible', () => {
    const d = fullDeck();
    expect(d).toHaveLength(40);
    for (const c of d) expect(parseCard(cardToString(c))).toBe(c);
  });
  it('a 8 reyes el 3 es rey y el 2 es as', () => {
    expect(effRank(parseCard('3O'), 8)).toBe(12);
    expect(effRank(parseCard('2B'), 8)).toBe(1);
    expect(effRank(parseCard('3O'), 4)).toBe(3);
    expect(effRank(parseCard('2B'), 4)).toBe(2);
  });
  it('valores de punto', () => {
    expect(pointValue(parseCard('12O'), 8)).toBe(10);
    expect(pointValue(parseCard('11O'), 8)).toBe(10);
    expect(pointValue(parseCard('10O'), 8)).toBe(10);
    expect(pointValue(parseCard('3O'), 8)).toBe(10);
    expect(pointValue(parseCard('3O'), 4)).toBe(3);
    expect(pointValue(parseCard('2O'), 8)).toBe(1);
    expect(pointValue(parseCard('7O'), 8)).toBe(7);
  });
  it('rechaza cartas inválidas', () => {
    expect(() => parseCard('8O')).toThrow();
    expect(() => parseCard('1X')).toThrow();
    expect(() => makeCard(9, 0)).toThrow();
  });
});

describe('grande y chica', () => {
  it('gana quien tiene la carta más alta, comparando en orden', () => {
    expect(compareGrande(H('12O 1C 4E 5B'), H('11O 11C 11E 11B'), 8)).toBeGreaterThan(0);
    expect(compareGrande(H('12O 12C 4E 5B'), H('12E 11C 11E 7B'), 8)).toBeGreaterThan(0);
    expect(compareGrande(H('12O 11C 7E 5B'), H('12E 11E 7O 6B'), 8)).toBeLessThan(0);
  });
  it('el 3 cuenta como rey a 8 reyes pero no a 4', () => {
    expect(compareGrande(H('3O 1C 1E 1B'), H('11O 11C 11E 11B'), 8)).toBeGreaterThan(0);
    expect(compareGrande(H('3O 1C 1E 1B'), H('11O 11C 11E 11B'), 4)).toBeLessThan(0);
  });
  it('rey y tres son iguales a grande (empate exacto)', () => {
    expect(compareGrande(H('3O 12C 4E 5B'), H('12O 12E 4C 5O'), 8)).toBe(0);
  });
  it('chica: gana la carta más baja', () => {
    expect(compareChica(H('1O 4C 12E 12B'), H('4O 4E 5C 6B'), 8)).toBeGreaterThan(0);
    expect(compareChica(H('1O 2C 7E 12B'), H('1C 1E 7O 12O'), 8)).toBe(0); // 2 = as
    expect(compareChica(H('1O 2C 7E 12B'), H('1C 1E 7O 12O'), 4)).toBeLessThan(0);
    expect(compareChica(H('1O 1C 4E 5B'), H('1E 1B 4O 6B'), 8)).toBeGreaterThan(0);
  });
});

describe('pares', () => {
  it('categorías', () => {
    expect(paresOf(H('1O 4C 6E 7B'), 8).cat).toBe(0);
    expect(paresOf(H('7O 7C 6E 5B'), 8)).toEqual({ cat: 1, hi: 7, lo: 0 });
    expect(paresOf(H('7O 7C 7E 5B'), 8)).toEqual({ cat: 2, hi: 7, lo: 0 });
    expect(paresOf(H('7O 7C 5E 5B'), 8)).toEqual({ cat: 3, hi: 7, lo: 5 });
    expect(paresOf(H('12O 3C 12E 3B'), 8)).toEqual({ cat: 3, hi: 12, lo: 12 }); // cuatro reyes = duples
    expect(paresOf(H('12O 3C 4E 5B'), 8)).toEqual({ cat: 1, hi: 12, lo: 0 }); // rey+tres = pareja de reyes
    expect(paresOf(H('12O 3C 4E 5B'), 4).cat).toBe(0);
    expect(paresOf(H('1O 2C 2E 5B'), 8)).toEqual({ cat: 2, hi: 1, lo: 0 }); // medias de ases
  });
  it('orden: duples > medias > pares; y dentro, por rango', () => {
    expect(comparePares(H('4O 4C 5E 5B'), H('12O 12C 12E 1B'), 8)).toBeGreaterThan(0);
    expect(comparePares(H('4O 4C 4E 5B'), H('12O 12C 1E 6B'), 8)).toBeGreaterThan(0);
    expect(comparePares(H('12O 12C 1E 1B'), H('11O 11C 10E 10B'), 8)).toBeGreaterThan(0);
    expect(comparePares(H('12O 12C 5E 5B'), H('12E 12B 4E 4B'), 8)).toBeGreaterThan(0);
    expect(comparePares(H('12O 12C 12E 12B'), H('12O 12C 11E 11B'), 8)).toBeGreaterThan(0);
    expect(comparePares(H('6O 6C 1E 4B'), H('6E 6B 7E 5B'), 8)).toBe(0); // las sueltas no cuentan
  });
});

describe('juego y punto', () => {
  it('suma de puntos y juego desde 31', () => {
    expect(handPoints(H('12O 11C 10E 1B'), 8)).toBe(31);
    expect(hasJuego(H('12O 11C 10E 1B'), 8)).toBe(true);
    expect(handPoints(H('12O 11C 7E 2B'), 8)).toBe(28);
    expect(hasJuego(H('12O 11C 7E 2B'), 8)).toBe(false);
    expect(handPoints(H('3O 3C 3E 3B'), 8)).toBe(40);
    expect(handPoints(H('3O 3C 3E 3B'), 4)).toBe(12);
  });
  it('orden de juego 31 > 32 > 40 > 37 > 36 > 35 > 34 > 33', () => {
    const order = [31, 32, 40, 37, 36, 35, 34, 33];
    for (let i = 0; i + 1 < order.length; i++) expect(juegoRank(order[i])).toBeLessThan(juegoRank(order[i + 1]));
    expect(compareJuego(H('12O 11C 10E 1B'), H('12E 12B 6O 6B'), 8)).toBeGreaterThan(0); // 31 > 32
    expect(compareJuego(H('12O 11C 10E 1B'), H('12E 12B 10O 2O'), 8)).toBe(0); // 31 = 31 (el 2 vale 1)
    expect(compareJuego(H('12O 11C 10E 2B'), H('12E 12B 10O 10B'), 8)).toBeGreaterThan(0); // 31 > 40
    expect(compareJuego(H('12O 11C 10E 10B'), H('12E 12B 7O 6B'), 8)).toBeGreaterThan(0); // 40 > 33
    expect(compareJuego(H('12O 11C 6E 6B'), H('12E 12B 7O 6O'), 8)).toBeGreaterThan(0); // 32 > 33
  });
  it('tantos de juego', () => {
    expect(juegoPoints(31)).toBe(3);
    expect(juegoPoints(32)).toBe(2);
    expect(juegoPoints(40)).toBe(2);
  });
  it('punto: más alto gana', () => {
    expect(comparePunto(H('12O 11C 7E 2B'), H('12E 12B 4O 1B'), 8)).toBeGreaterThan(0); // 28 > 25
    expect(comparePunto(H('12O 11C 6E 4B'), H('12E 12B 7O 3B'), 4)).toBe(0);
  });
});

describe('desempate por mano', () => {
  it('en empate exacto gana el más cercano a la mano', () => {
    const hands = [H('12O 11O 4O 5O'), H('12C 11C 4C 5C'), H('12E 11E 4E 5E'), H('12B 11B 4B 5B')];
    expect(bestSeat(hands, [2, 3, 0, 1], compareGrande, 8)).toBe(2);
    expect(bestSeat(hands, [1, 2, 3, 0], compareChica, 8)).toBe(1);
  });
});
