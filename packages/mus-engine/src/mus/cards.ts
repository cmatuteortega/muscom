/**
 * Baraja española de 40 cartas. Una carta es un entero 0..39:
 *   palo  = floor(c / 10)  -> 0 oros, 1 copas, 2 espadas, 3 bastos
 *   rango = RANKS[c % 10]  -> 1,2,3,4,5,6,7,10 (sota),11 (caballo),12 (rey)
 */
export type Card = number;
export type Suit = 0 | 1 | 2 | 3;

export const SUITS = ['oros', 'copas', 'espadas', 'bastos'] as const;
export const SUIT_LETTERS = ['O', 'C', 'E', 'B'] as const;
export const RANKS = [1, 2, 3, 4, 5, 6, 7, 10, 11, 12] as const;

export function suitOf(c: Card): Suit {
  return Math.floor(c / 10) as Suit;
}
export function rankOf(c: Card): number {
  return RANKS[c % 10];
}
export function makeCard(rank: number, suit: Suit): Card {
  const idx = RANKS.indexOf(rank as (typeof RANKS)[number]);
  if (idx < 0) throw new Error(`Rango inválido: ${rank}`);
  return suit * 10 + idx;
}
export function fullDeck(): Card[] {
  return Array.from({ length: 40 }, (_, i) => i);
}

/**
 * Rango efectivo: a 8 reyes los treses cuentan como reyes y los doses como ases.
 * Orden de grande: 12 > 11 > 10 > 7 > 6 > 5 > 4 > 1 (y 3, 2 sólo a 4 reyes).
 */
export function effRank(c: Card, reyes: 4 | 8): number {
  const r = rankOf(c);
  if (reyes === 8) {
    if (r === 3) return 12;
    if (r === 2) return 1;
  }
  return r;
}

/** Valor para juego/punto: figuras (y treses a 8 reyes) valen 10. */
export function pointValue(c: Card, reyes: 4 | 8): number {
  const r = effRank(c, reyes);
  return r >= 10 ? 10 : r;
}

/** Notación compacta "12O", "1E", "3B"… */
export function cardToString(c: Card): string {
  return `${rankOf(c)}${SUIT_LETTERS[suitOf(c)]}`;
}
export function parseCard(s: string): Card {
  const m = /^(\d{1,2})([OCEB])$/.exec(s.trim().toUpperCase());
  if (!m) throw new Error(`Carta inválida: ${s}`);
  return makeCard(Number(m[1]), SUIT_LETTERS.indexOf(m[2] as 'O') as Suit);
}
export function parseHand(s: string): Card[] {
  return s.trim().split(/\s+/).map(parseCard);
}

const RANK_NAMES: Record<number, string> = {
  1: 'As', 2: 'Dos', 3: 'Tres', 4: 'Cuatro', 5: 'Cinco', 6: 'Seis', 7: 'Siete',
  10: 'Sota', 11: 'Caballo', 12: 'Rey',
};
export function cardName(c: Card): string {
  return `${RANK_NAMES[rankOf(c)]} de ${SUITS[suitOf(c)]}`;
}
