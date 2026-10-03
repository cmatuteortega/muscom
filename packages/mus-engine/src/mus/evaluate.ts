import { Card, effRank, pointValue } from './cards.js';

export type Reyes = 4 | 8;

/** Ranks efectivos de mayor a menor. */
export function grandeKey(hand: Card[], reyes: Reyes): number[] {
  return hand.map((c) => effRank(c, reyes)).sort((a, b) => b - a);
}
/** Ranks efectivos de menor a mayor. */
export function chicaKey(hand: Card[], reyes: Reyes): number[] {
  return hand.map((c) => effRank(c, reyes)).sort((a, b) => a - b);
}

/** >0 si `a` gana a `b` a grande, <0 si pierde, 0 si empate exacto. */
export function compareGrande(a: Card[], b: Card[], reyes: Reyes): number {
  const ka = grandeKey(a, reyes);
  const kb = grandeKey(b, reyes);
  for (let i = 0; i < ka.length; i++) if (ka[i] !== kb[i]) return ka[i] - kb[i];
  return 0;
}
/** >0 si `a` gana a `b` a chica (cartas más bajas). */
export function compareChica(a: Card[], b: Card[], reyes: Reyes): number {
  const ka = chicaKey(a, reyes);
  const kb = chicaKey(b, reyes);
  for (let i = 0; i < ka.length; i++) if (ka[i] !== kb[i]) return kb[i] - ka[i];
  return 0;
}

/** 0 nada, 1 pares (pareja), 2 medias (trío), 3 duples (dos parejas o cuatro iguales). */
export type ParesCategory = 0 | 1 | 2 | 3;
export interface Pares {
  cat: ParesCategory;
  /** Rango de la pareja/trío (la más alta en duples). */
  hi: number;
  /** Rango de la segunda pareja en duples (igual a hi con cuatro iguales). */
  lo: number;
}

export function paresOf(hand: Card[], reyes: Reyes): Pares {
  const counts = new Map<number, number>();
  for (const c of hand) {
    const r = effRank(c, reyes);
    counts.set(r, (counts.get(r) ?? 0) + 1);
  }
  const groups = [...counts.entries()].filter(([, n]) => n >= 2).sort((a, b) => b[0] - a[0]);
  if (groups.length === 0) return { cat: 0, hi: 0, lo: 0 };
  if (groups.length === 2) return { cat: 3, hi: groups[0][0], lo: groups[1][0] };
  const [r, n] = groups[0];
  if (n === 4) return { cat: 3, hi: r, lo: r };
  if (n === 3) return { cat: 2, hi: r, lo: 0 };
  return { cat: 1, hi: r, lo: 0 };
}

export function hasPares(hand: Card[], reyes: Reyes): boolean {
  return paresOf(hand, reyes).cat > 0;
}
/** Tantos que vale la jugada de pares: pares 1, medias 2, duples 3. */
export function paresPoints(p: Pares): number {
  return p.cat;
}
export function compareParesValue(a: Pares, b: Pares): number {
  if (a.cat !== b.cat) return a.cat - b.cat;
  if (a.hi !== b.hi) return a.hi - b.hi;
  return a.lo - b.lo;
}
export function comparePares(a: Card[], b: Card[], reyes: Reyes): number {
  return compareParesValue(paresOf(a, reyes), paresOf(b, reyes));
}

export function handPoints(hand: Card[], reyes: Reyes): number {
  return hand.reduce((s, c) => s + pointValue(c, reyes), 0);
}
export function hasJuego(hand: Card[], reyes: Reyes): boolean {
  return handPoints(hand, reyes) >= 31;
}
/** Orden de juego: 31 > 32 > 40 > 37 > 36 > 35 > 34 > 33. */
export const JUEGO_ORDER = [31, 32, 40, 37, 36, 35, 34, 33] as const;
export function juegoRank(points: number): number {
  const i = (JUEGO_ORDER as readonly number[]).indexOf(points);
  return i < 0 ? 99 : i;
}
/** Tantos de la jugada de juego: 31 vale 3, el resto 2. */
export function juegoPoints(points: number): number {
  return points === 31 ? 3 : 2;
}
export function compareJuego(a: Card[], b: Card[], reyes: Reyes): number {
  return juegoRank(handPoints(b, reyes)) - juegoRank(handPoints(a, reyes));
}
export function comparePunto(a: Card[], b: Card[], reyes: Reyes): number {
  return handPoints(a, reyes) - handPoints(b, reyes);
}

export type Comparator = (a: Card[], b: Card[], reyes: Reyes) => number;

/**
 * Mejor asiento entre `seatsInManoOrder` (ordenados desde la mano).
 * En caso de empate gana el más cercano a la mano.
 */
export function bestSeat(
  hands: Card[][],
  seatsInManoOrder: number[],
  cmp: Comparator,
  reyes: Reyes,
): number {
  let best = seatsInManoOrder[0];
  for (let i = 1; i < seatsInManoOrder.length; i++) {
    const s = seatsInManoOrder[i];
    if (cmp(hands[s], hands[best], reyes) > 0) best = s;
  }
  return best;
}
