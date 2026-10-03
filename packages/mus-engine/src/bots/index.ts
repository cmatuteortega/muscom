/**
 * Bots de Mus. Sólo reciben la vista redactada del jugador (MusView), así que
 * juegan exactamente con la misma información y reglas que un humano.
 *
 *  - facil:   estimación con pocas muestras, mucho ruido y faroles aleatorios.
 *  - normal:  Monte Carlo moderado y umbrales razonables.
 *  - dificil: Monte Carlo condicionado por declaraciones y señas del compañero,
 *             consciente del marcador (órdagos defensivos/ofensivos).
 */
import type { Rng } from '../core/rng.js';
import { Card, effRank, fullDeck } from '../mus/cards.js';
import { handPoints, hasJuego, hasPares, paresOf } from '../mus/evaluate.js';
import { COMPARATORS } from '../mus/engine.js';
import type { Lance, MusAction, MusView, Seat, Sena } from '../mus/types.js';
import { partnerOf, teamOf } from '../mus/types.js';

export type BotLevel = 'facil' | 'normal' | 'dificil';

interface LevelParams {
  samples: number;
  noise: number;
  bluff: number;
  betThreshold: number;
  acceptThreshold: number;
  raiseThreshold: number;
  useSenas: boolean;
  scoreAware: boolean;
}

const PARAMS: Record<BotLevel, LevelParams> = {
  facil: { samples: 40, noise: 0.25, bluff: 0.12, betThreshold: 0.6, acceptThreshold: 0.4, raiseThreshold: 0.92, useSenas: false, scoreAware: false },
  normal: { samples: 160, noise: 0.08, bluff: 0.05, betThreshold: 0.66, acceptThreshold: 0.5, raiseThreshold: 0.84, useSenas: true, scoreAware: false },
  dificil: { samples: 400, noise: 0.03, bluff: 0.07, betThreshold: 0.62, acceptThreshold: 0.47, raiseThreshold: 0.8, useSenas: true, scoreAware: true },
};

// ───────────────────────── señas ─────────────────────────

/** Qué haría saber un bot a su compañero (máx. 2 señas, por prioridad). */
export function chooseSenas(hand: Card[], reyes: 4 | 8): Sena[] {
  const out: Sena[] = [];
  const p = paresOf(hand, reyes);
  const pts = handPoints(hand, reyes);
  const reyesN = hand.filter((c) => effRank(c, reyes) === 12).length;
  const asesN = hand.filter((c) => effRank(c, reyes) === 1).length;
  if (pts === 31) out.push('treintaYUna');
  if (p.cat === 3) out.push('duples');
  else if (p.cat === 2) out.push('medias');
  if (reyesN >= 3) out.push('tresReyes');
  else if (reyesN === 2) out.push('dosReyes');
  if (asesN >= 3) out.push('tresAses');
  else if (asesN === 2) out.push('dosAses');
  if (out.length === 0) out.push('ciego');
  return out.slice(0, 2);
}

function senaConstraint(sena: Sena, reyes: 4 | 8): (h: Card[]) => boolean {
  const count = (h: Card[], r: number) => h.filter((c) => effRank(c, reyes) === r).length;
  switch (sena) {
    case 'dosReyes': return (h) => count(h, 12) >= 2;
    case 'tresReyes': return (h) => count(h, 12) >= 3;
    case 'dosAses': return (h) => count(h, 1) >= 2;
    case 'tresAses': return (h) => count(h, 1) >= 3;
    case 'duples': return (h) => paresOf(h, reyes).cat === 3;
    case 'medias': return (h) => paresOf(h, reyes).cat >= 2;
    case 'treintaYUna': return (h) => handPoints(h, reyes) === 31;
    case 'ciego': return (h) => !hasPares(h, reyes) && !hasJuego(h, reyes);
  }
}

// ───────────────────────── Monte Carlo ─────────────────────────

export interface Estimate {
  grande: number;
  chica: number;
  pares: number;
  juego: number;
  punto: number;
}

/**
 * Probabilidad de que la pareja de `view.seat` gane cada lance, muestreando las
 * cartas desconocidas de forma coherente con lo público (declaraciones) y, si
 * procede, con las señas del compañero.
 */
export function estimate(view: MusView, rng: Rng, samples: number, useSenas: boolean): Estimate {
  const reyes = view.config.reyes;
  const me = view.seat;
  const unknown = fullDeck().filter((c) => !view.hand.includes(c));
  const decl = view.declarations;
  const partnerConstraints = useSenas
    ? view.senas.filter((x) => x.seat === partnerOf(me)).map((x) => senaConstraint(x.sena, reyes))
    : [];
  const order = [0, 1, 2, 3].map((i) => ((view.mano + i) % 4) as Seat);
  const wins: Estimate = { grande: 0, chica: 0, pares: 0, juego: 0, punto: 0 };
  const counted: Estimate = { grande: 0, chica: 0, pares: 0, juego: 0, punto: 0 };
  const hands: Card[][] = [[], [], [], []];
  let accepted = 0;
  for (let it = 0; it < samples * 6 && accepted < samples; it++) {
    // Barajado parcial de las desconocidas.
    const pool = unknown.slice();
    for (let i = 0; i < 12; i++) {
      const j = i + rng.int(pool.length - i);
      const t = pool[i]; pool[i] = pool[j]; pool[j] = t;
    }
    let k = 0;
    for (const s of order) hands[s] = s === me ? view.hand : pool.slice(k, (k += 4));
    // Rechazo si contradice lo que ya es público o lo que dijo el compañero.
    const relax = it > samples * 4; // si cuesta mucho, aceptamos sin condicionar
    if (!relax) {
      if (decl.pares && hands.some((h, s) => hasPares(h, reyes) !== decl.pares![s])) continue;
      if (decl.juego && hands.some((h, s) => hasJuego(h, reyes) !== decl.juego![s])) continue;
      if (partnerConstraints.some((f) => !f(hands[partnerOf(me)]))) continue;
    }
    accepted++;
    for (const lance of ['grande', 'chica', 'pares', 'juego', 'punto'] as Lance[]) {
      let seats = order;
      if (lance === 'pares') seats = order.filter((s) => hasPares(hands[s], reyes));
      if (lance === 'juego') seats = order.filter((s) => hasJuego(hands[s], reyes));
      if (lance === 'punto' && order.some((s) => hasJuego(hands[s], reyes))) continue;
      if (seats.length === 0) continue;
      let best = seats[0];
      for (const s of seats.slice(1)) if (COMPARATORS[lance](hands[s], hands[best], reyes) > 0) best = s;
      counted[lance]++;
      if (teamOf(best) === teamOf(me)) wins[lance]++;
    }
  }
  const out = {} as Estimate;
  for (const l of Object.keys(wins) as Lance[]) out[l] = counted[l] ? wins[l] / counted[l] : 0.5;
  return out;
}

// ───────────────────────── decisiones ─────────────────────────

function clamp01(x: number) {
  return Math.max(0, Math.min(1, x));
}

function decideMus(view: MusView, est: Estimate, p: LevelParams, rng: Rng): MusAction {
  const reyes = view.config.reyes;
  const pr = paresOf(view.hand, reyes);
  const pts = handPoints(view.hand, reyes);
  let strong = 0;
  if (pr.cat >= 2) strong += 2;
  else if (pr.cat === 1 && pr.hi >= 10) strong += 1;
  if (pts === 31) strong += 2;
  else if (pts >= 31) strong += 1;
  if (est.grande > 0.75) strong += 1;
  if (est.chica > 0.8) strong += 1;
  if (pts === 30 || pts === 29) strong += 0.5; // buen punto
  let cut = strong >= 2;
  if (p.scoreAware) {
    // Si vamos muy por detrás nos interesa más el mus; si vamos por delante, cortar antes.
    const diff = view.scores[teamOf(view.seat)] - view.scores[1 - teamOf(view.seat)];
    if (diff > 10 && strong >= 1.5) cut = true;
  }
  if (rng.float() < p.noise) cut = !cut;
  return cut ? { type: 'noMus' } : { type: 'mus' };
}

/** Descarte: conserva parejas y reyes (y ases si van a chica). */
export function chooseDiscard(hand: Card[], reyes: 4 | 8, rng: Rng, level: BotLevel): Card[] {
  if (level === 'facil' && rng.float() < 0.3) {
    const n = 1 + rng.int(4);
    return hand.slice().sort(() => rng.float() - 0.5).slice(0, n);
  }
  const counts = new Map<number, number>();
  for (const c of hand) counts.set(effRank(c, reyes), (counts.get(effRank(c, reyes)) ?? 0) + 1);
  const ases = hand.filter((c) => effRank(c, reyes) === 1).length;
  const reyesN = hand.filter((c) => effRank(c, reyes) === 12).length;
  const goChica = ases >= 2 && reyesN < 2;
  const keep = (c: Card) => {
    const r = effRank(c, reyes);
    if ((counts.get(r) ?? 0) >= 2) return true;
    if (goChica) return r === 1;
    return r === 12;
  };
  let discard = hand.filter((c) => !keep(c));
  if (discard.length === 0) {
    // Hay que tirar al menos una: la de menor valor de grande que no forme pareja.
    const sorted = hand
      .slice()
      .sort((a, b) => (counts.get(effRank(a, reyes))! - counts.get(effRank(b, reyes))!) || effRank(a, reyes) - effRank(b, reyes));
    discard = [sorted[0]];
  }
  return discard;
}

function lanceStrength(view: MusView, est: Estimate, lance: Lance): number {
  return est[lance];
}

function decideBet(view: MusView, est: Estimate, p: LevelParams, rng: Rng): MusAction {
  const b = view.betting!;
  const myTeam = teamOf(view.seat);
  const myScore = view.scores[myTeam];
  const theirScore = view.scores[1 - myTeam];
  const target = view.config.target;
  let pw = clamp01(lanceStrength(view, est, b.lance) + (rng.float() * 2 - 1) * p.noise);
  // Ser mano da ventaja en empates; ya está incluido en la estimación.
  const legal = view.legal;
  const can = (t: MusAction['type']) => legal.some((a) => a.type === t);

  // El partido se acaba: si el rival está a punto de ganar, el órdago es la última bala.
  const desperate = p.scoreAware && theirScore >= target - 5 && myScore < target - 10;

  if (b.ordago) {
    let accept = pw > 0.72;
    if (p.scoreAware) {
      if (desperate && pw > 0.4) accept = true;
      if (myScore >= target - 3 && pw < 0.8) accept = false;
    }
    return accept ? { type: 'quiero' } : { type: 'noQuiero' };
  }

  if (b.proposed === 0) {
    const late = Math.max(myScore, theirScore) >= target - 12;
    if (p.scoreAware && can('ordago') && ((late && pw > 0.95) || (desperate && pw > 0.62))) return { type: 'ordago' };
    if (!p.scoreAware && pw > 0.98 && rng.float() < 0.15) return { type: 'ordago' };
    if (pw > p.betThreshold) {
      const amount = pw > p.raiseThreshold && rng.float() < 0.4 ? 5 : 2;
      return { type: 'envido', amount };
    }
    if (rng.float() < p.bluff) return { type: 'envido', amount: 2 };
    return { type: 'paso' };
  }

  // Responder a un envite: más exigencia cuanto más hay en juego.
  const stake = b.proposed;
  const need = p.acceptThreshold + Math.min(0.25, (stake - 2) * 0.02);
  if (pw > p.raiseThreshold && stake < 15) {
    if (p.scoreAware && pw > 0.95 && stake >= 8) return { type: 'ordago' };
    return { type: 'envido', amount: 2 };
  }
  if (pw > need) return { type: 'quiero' };
  if (rng.float() < p.bluff / 2 && stake <= 4) return { type: 'envido', amount: 2 };
  return { type: 'noQuiero' };
}

/**
 * Decide la próxima acción del bot a partir de su vista. Devuelve siempre una acción legal.
 */
export function decide(view: MusView, level: BotLevel, rng: Rng): MusAction {
  const p = PARAMS[level];
  if (view.legal.length === 0) throw new Error('El bot no tiene acciones legales');
  switch (view.phase) {
    case 'finMano':
      return { type: 'siguienteMano' };
    case 'descarte':
      return { type: 'descarte', cards: chooseDiscard(view.hand, view.config.reyes, rng, level) };
    case 'mus':
      return decideMus(view, estimate(view, rng, Math.min(p.samples, 120), false), p, rng);
    default: {
      const est = estimate(view, rng, p.samples, p.useSenas);
      return decideBet(view, est, p, rng);
    }
  }
}

export const BOT_LEVELS: readonly BotLevel[] = ['facil', 'normal', 'dificil'];
