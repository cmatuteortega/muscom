import { GameModule, IllegalActionError } from '../core/game.js';
import { rngFromSeed, shuffleInPlace } from '../core/rng.js';
import { Card, fullDeck } from './cards.js';
import {
  Comparator,
  bestSeat,
  compareChica,
  compareGrande,
  compareJuego,
  comparePares,
  comparePunto,
  handPoints,
  hasJuego,
  hasPares,
  juegoPoints,
  paresOf,
  paresPoints,
} from './evaluate.js';
import {
  Award,
  AwardKind,
  Betting,
  DEFAULT_CONFIG,
  HandSummary,
  Lance,
  LanceRecord,
  LogEntry,
  MusAction,
  MusConfig,
  MusState,
  MusView,
  Phase,
  SENAS,
  Seat,
  Team,
  partnerOf,
  teamOf,
} from './types.js';

const MAX_ENVIDO = 99;
const BETTING_PHASES: readonly Phase[] = ['grande', 'chica', 'pares', 'juego', 'punto'];
const ACTIVE_PHASES: readonly Phase[] = ['mus', 'descarte', ...BETTING_PHASES];

export const COMPARATORS: Record<Lance, Comparator> = {
  grande: compareGrande,
  chica: compareChica,
  pares: comparePares,
  juego: compareJuego,
  punto: comparePunto,
};

// ───────────────────────── utilidades ─────────────────────────

export function manoOf(dealer: Seat): Seat {
  return ((dealer + 1) % 4) as Seat;
}

/** Asientos en orden de juego empezando por la mano. */
export function manoOrder(state: Pick<MusState, 'dealer'>): Seat[] {
  const m = manoOf(state.dealer);
  return [0, 1, 2, 3].map((i) => ((m + i) % 4) as Seat);
}

function cloneState(s: MusState): MusState {
  return {
    config: s.config,
    seed: s.seed,
    rng: [...s.rng] as MusState['rng'],
    scores: [s.scores[0], s.scores[1]],
    dealer: s.dealer,
    handNumber: s.handNumber,
    deck: [...s.deck],
    discardPile: [...s.discardPile],
    hands: s.hands.map((h) => [...h]),
    phase: s.phase,
    turn: s.turn,
    musRound: s.musRound,
    pendingDiscard: [...s.pendingDiscard],
    lastDiscardCounts: [...s.lastDiscardCounts],
    betting: s.betting
      ? { ...s.betting, participants: [...s.betting.participants], queue: [...s.betting.queue] }
      : null,
    lances: s.lances.map((l) => ({ ...l })),
    declarations: {
      pares: s.declarations.pares ? [...s.declarations.pares] : null,
      juego: s.declarations.juego ? [...s.declarations.juego] : null,
    },
    senas: [...s.senas],
    awards: [...s.awards],
    summary: s.summary,
    winner: s.winner,
    history: [...s.history],
  };
}

/** Rivales de `seat` entre los participantes, en orden de juego a partir de él. */
function opponentsAfter(participants: Seat[], seat: Seat): Seat[] {
  const out: Seat[] = [];
  for (let i = 1; i <= 4; i++) {
    const s = ((seat + i) % 4) as Seat;
    if (participants.includes(s) && teamOf(s) !== teamOf(seat)) out.push(s);
  }
  return out;
}

// ───────────────────────── creación y reparto ─────────────────────────

export function createGame(config: Partial<MusConfig> = {}, seed: string): MusState {
  const cfg: MusConfig = { ...DEFAULT_CONFIG, ...config };
  if (cfg.reyes !== 4 && cfg.reyes !== 8) throw new Error('reyes debe ser 4 u 8');
  if (!(cfg.target > 0)) throw new Error('target inválido');
  const s: MusState = {
    config: cfg,
    seed,
    rng: rngFromSeed(seed),
    scores: [0, 0],
    dealer: cfg.firstDealer,
    handNumber: 0,
    deck: [],
    discardPile: [],
    hands: [[], [], [], []],
    phase: 'mus',
    turn: null,
    musRound: 0,
    pendingDiscard: [],
    lastDiscardCounts: [0, 0, 0, 0],
    betting: null,
    lances: [],
    declarations: { pares: null, juego: null },
    senas: [],
    awards: [],
    summary: null,
    winner: null,
    history: [],
  };
  startHand(s, cfg.firstDealer);
  return s;
}

function startHand(s: MusState, dealer: Seat): void {
  s.dealer = dealer;
  s.handNumber += 1;
  s.deck = shuffleInPlace(fullDeck(), s.rng);
  s.discardPile = [];
  s.hands = [[], [], [], []];
  const order = manoOrder(s);
  for (let r = 0; r < 4; r++) for (const seat of order) s.hands[seat].push(s.deck.shift()!);
  s.phase = 'mus';
  s.turn = order[0];
  s.musRound = 0;
  s.pendingDiscard = [];
  s.lastDiscardCounts = [0, 0, 0, 0];
  s.betting = null;
  s.lances = [];
  s.declarations = { pares: null, juego: null };
  s.senas = [];
  s.awards = [];
  s.summary = null;
}

function drawCard(s: MusState): Card {
  if (s.deck.length === 0) {
    // Se acaba el mazo: se barajan los descartes y se sigue repartiendo.
    s.deck = shuffleInPlace(s.discardPile, s.rng);
    s.discardPile = [];
  }
  const c = s.deck.shift();
  if (c === undefined) throw new Error('Mazo agotado');
  return c;
}

// ───────────────────────── lances ─────────────────────────

function startBetting(s: MusState, lance: Lance, participants: Seat[]): void {
  s.phase = lance;
  s.betting = {
    lance,
    participants,
    queue: [...participants],
    proposed: 0,
    deje: 0,
    bettingTeam: null,
    ordago: false,
  };
  s.turn = participants[0];
}

function record(s: MusState, rec: Omit<LanceRecord, 'ordago' | 'team' | 'amount'> & Partial<LanceRecord>): void {
  s.lances.push({ amount: 0, team: null, ordago: false, ...rec });
}

/** Paga tantos; devuelve true si con ello termina la partida. */
function pay(s: MusState, lance: Lance, team: Team, points: number, kind: AwardKind): boolean {
  if (points <= 0) return false;
  const capped = Math.min(points, s.config.target - s.scores[team]);
  s.scores[team] += capped;
  s.awards.push({ lance, team, points: capped, kind });
  if (s.scores[team] >= s.config.target) {
    s.winner = team;
    return true;
  }
  return false;
}

function advanceAfter(s: MusState, lance: Lance): void {
  s.betting = null;
  const order = manoOrder(s);
  if (lance === 'grande') return startBetting(s, 'chica', order);
  if (lance === 'chica') {
    const decl = s.hands.map((h) => hasPares(h, s.config.reyes));
    s.declarations.pares = decl;
    const holders = order.filter((x) => decl[x]);
    const teams = new Set(holders.map(teamOf));
    if (teams.size === 2) return startBetting(s, 'pares', holders);
    record(s, { lance: 'pares', outcome: holders.length ? 'sinApuesta' : 'noHay' });
    return enterJuego(s);
  }
  if (lance === 'pares') return enterJuego(s);
  return conteo(s);
}

function enterJuego(s: MusState): void {
  const order = manoOrder(s);
  const decl = s.hands.map((h) => hasJuego(h, s.config.reyes));
  s.declarations.juego = decl;
  const holders = order.filter((x) => decl[x]);
  if (holders.length === 0) return startBetting(s, 'punto', order);
  const teams = new Set(holders.map(teamOf));
  if (teams.size === 2) return startBetting(s, 'juego', holders);
  record(s, { lance: 'juego', outcome: 'sinApuesta' });
  conteo(s);
}

/** Asiento ganador de un lance (desempate por mano). */
export function lanceWinner(s: Pick<MusState, 'hands' | 'dealer' | 'config'>, lance: Lance): Seat {
  const order = manoOrder(s);
  const reyes = s.config.reyes;
  let seats = order;
  if (lance === 'pares') seats = order.filter((x) => hasPares(s.hands[x], reyes));
  if (lance === 'juego') seats = order.filter((x) => hasJuego(s.hands[x], reyes));
  return bestSeat(s.hands, seats, COMPARATORS[lance], reyes) as Seat;
}

function finishGame(s: MusState, winners: HandSummary['winners']): void {
  s.betting = null;
  s.turn = null;
  s.phase = 'finPartida';
  s.summary = { hands: s.hands.map((h) => [...h]), winners, awards: [...s.awards], gameOver: true };
}

function resolveOrdago(s: MusState, lance: Lance): void {
  const w = lanceWinner(s, lance);
  const team = teamOf(w);
  record(s, { lance, outcome: 'querido', amount: 0, ordago: true });
  pay(s, lance, team, s.config.target - s.scores[team], 'ordago');
  s.winner = team;
  finishGame(s, { [lance]: w });
}

function conteo(s: MusState): void {
  s.betting = null;
  s.turn = null;
  const reyes = s.config.reyes;
  const winners: HandSummary['winners'] = {};
  let over = s.winner !== null;
  for (const rec of s.lances) {
    if (rec.outcome === 'noHay') continue;
    const w = lanceWinner(s, rec.lance);
    winners[rec.lance] = w;
    const team = teamOf(w);
    if (over) continue;
    const payments: [number, AwardKind][] = [];
    if (rec.outcome === 'querido') payments.push([rec.amount, 'apuesta']);
    if (rec.lance === 'grande' || rec.lance === 'chica') {
      if (rec.outcome === 'paso') payments.push([1, 'paso']);
    } else if (rec.lance === 'pares') {
      const pts = s.hands
        .map((h, seat) => (teamOf(seat) === team ? paresPoints(paresOf(h, reyes)) : 0))
        .reduce((a, b) => a + b, 0);
      payments.push([pts, 'jugada']);
    } else if (rec.lance === 'juego') {
      const pts = s.hands
        .map((h, seat) => {
          const p = handPoints(h, reyes);
          return teamOf(seat) === team && p >= 31 ? juegoPoints(p) : 0;
        })
        .reduce((a, b) => a + b, 0);
      payments.push([pts, 'jugada']);
    } else if (rec.lance === 'punto') {
      payments.push([1, 'jugada']);
    }
    for (const [pts, kind] of payments) {
      if (pay(s, rec.lance, team, pts, kind)) {
        over = true;
        break;
      }
    }
  }
  s.summary = { hands: s.hands.map((h) => [...h]), winners, awards: [...s.awards], gameOver: over };
  s.phase = over ? 'finPartida' : 'finMano';
}

// ───────────────────────── validación ─────────────────────────

export function actors(s: MusState): Seat[] {
  if (s.phase === 'finPartida') return [];
  if (s.phase === 'finMano') return [0, 1, 2, 3];
  if (s.phase === 'descarte') return [...s.pendingDiscard];
  return s.turn === null ? [] : [s.turn];
}

export function validate(s: MusState, seat: number, a: MusAction): string | null {
  if (!Number.isInteger(seat) || seat < 0 || seat > 3) return 'Asiento inválido';
  if (!a || typeof a !== 'object') return 'Acción inválida';
  if (s.phase === 'finPartida') return 'La partida ha terminado';
  if (a.type === 'sena') {
    if (!ACTIVE_PHASES.includes(s.phase)) return 'Ahora no se pueden hacer señas';
    if (!SENAS.includes(a.sena)) return 'Seña desconocida';
    const n = s.senas.filter((x) => x.seat === seat).length;
    if (n >= s.config.maxSenasPerHand) return 'Demasiadas señas en esta mano';
    return null;
  }
  if (a.type === 'siguienteMano') return s.phase === 'finMano' ? null : 'La mano no ha terminado';
  if (s.phase === 'descarte') {
    if (a.type !== 'descarte') return 'Toca descartar';
    if (!s.pendingDiscard.includes(seat as Seat)) return 'Ya has descartado';
    if (!Array.isArray(a.cards) || a.cards.length < 1 || a.cards.length > 4) return 'Descarta entre 1 y 4 cartas';
    if (new Set(a.cards).size !== a.cards.length) return 'Cartas repetidas';
    if (!a.cards.every((c) => s.hands[seat].includes(c))) return 'No tienes esa carta';
    return null;
  }
  if (s.turn !== seat) return 'No es tu turno';
  if (s.phase === 'mus') return a.type === 'mus' || a.type === 'noMus' ? null : 'Di mus o no hay mus';
  if (s.phase === 'finMano') return 'La mano ha terminado';
  const b = s.betting!;
  const hasBet = b.proposed > 0 || b.ordago;
  switch (a.type) {
    case 'paso':
      return hasBet ? 'Hay una apuesta: quiere o no quiere' : null;
    case 'quiero':
    case 'noQuiero':
      return hasBet ? null : 'No hay apuesta';
    case 'envido':
      if (b.ordago) return 'Al órdago sólo se puede querer o no querer';
      if (!Number.isInteger(a.amount) || a.amount < 2 || a.amount > MAX_ENVIDO) return 'Envite inválido';
      return null;
    case 'ordago':
      return b.ordago ? 'Ya hay un órdago' : null;
    default:
      return 'Acción no válida en este lance';
  }
}

/** Lista canónica de acciones (sin señas). El orden forma parte del contrato de conformidad. */
export function legalActions(s: MusState, seat: number): MusAction[] {
  if (!actors(s).includes(seat as Seat)) return [];
  switch (s.phase) {
    case 'finMano':
      return [{ type: 'siguienteMano' }];
    case 'mus':
      return [{ type: 'mus' }, { type: 'noMus' }];
    case 'descarte': {
      const hand = s.hands[seat];
      const out: MusAction[] = [];
      for (let mask = 1; mask < 1 << hand.length; mask++) {
        out.push({ type: 'descarte', cards: hand.filter((_, i) => mask & (1 << i)) });
      }
      return out;
    }
    default: {
      const b = s.betting!;
      if (b.ordago) return [{ type: 'quiero' }, { type: 'noQuiero' }];
      if (b.proposed === 0)
        return [{ type: 'paso' }, { type: 'envido', amount: 2 }, { type: 'envido', amount: 5 }, { type: 'ordago' }];
      return [
        { type: 'quiero' },
        { type: 'noQuiero' },
        { type: 'envido', amount: 2 },
        { type: 'envido', amount: 5 },
        { type: 'ordago' },
      ];
    }
  }
}

// ───────────────────────── aplicar ─────────────────────────

export function applyAction(state: MusState, seat: number, a: MusAction): MusState {
  const err = validate(state, seat, a);
  if (err) throw new IllegalActionError(err);
  const s = cloneState(state);
  const st = seat as Seat;
  s.history.push({ hand: s.handNumber, phase: s.phase, seat: st, action: a });

  switch (a.type) {
    case 'sena':
      s.senas.push({ seat: st, sena: a.sena });
      return s;
    case 'siguienteMano':
      startHand(s, manoOf(s.dealer));
      return s;
    case 'mus': {
      const order = manoOrder(s);
      const idx = order.indexOf(st);
      if (idx === 3) {
        s.phase = 'descarte';
        s.turn = null;
        s.pendingDiscard = order;
        s.lastDiscardCounts = [0, 0, 0, 0];
      } else {
        s.turn = order[idx + 1];
      }
      return s;
    }
    case 'noMus':
      startBetting(s, 'grande', manoOrder(s));
      return s;
    case 'descarte': {
      s.hands[st] = s.hands[st].filter((c) => !a.cards.includes(c));
      s.discardPile.push(...a.cards);
      s.lastDiscardCounts[st] = a.cards.length;
      s.pendingDiscard = s.pendingDiscard.filter((x) => x !== st);
      if (s.pendingDiscard.length === 0) {
        for (const p of manoOrder(s)) while (s.hands[p].length < 4) s.hands[p].push(drawCard(s));
        s.phase = 'mus';
        s.turn = manoOf(s.dealer);
        s.musRound += 1;
      }
      return s;
    }
    default:
      applyBetting(s, st, a);
      return s;
  }
}

function applyBetting(s: MusState, seat: Seat, a: MusAction): void {
  const b = s.betting as Betting;
  const lance = b.lance;
  const team = teamOf(seat);
  switch (a.type) {
    case 'paso':
      b.queue.shift();
      if (b.queue.length === 0) {
        record(s, { lance, outcome: 'paso' });
        advanceAfter(s, lance);
        return;
      }
      break;
    case 'envido':
      if (b.proposed === 0) {
        b.proposed = a.amount;
        b.deje = 1;
      } else {
        b.deje = b.proposed;
        b.proposed += a.amount;
      }
      b.bettingTeam = team;
      b.queue = opponentsAfter(b.participants, seat);
      break;
    case 'ordago':
      b.deje = b.proposed === 0 ? 1 : b.proposed;
      b.ordago = true;
      b.bettingTeam = team;
      b.queue = opponentsAfter(b.participants, seat);
      break;
    case 'quiero':
      if (b.ordago) return resolveOrdago(s, lance);
      record(s, { lance, outcome: 'querido', amount: b.proposed });
      advanceAfter(s, lance);
      return;
    case 'noQuiero':
      b.queue.shift();
      if (b.queue.length === 0) {
        const t = b.bettingTeam as Team;
        record(s, { lance, outcome: 'noQuerido', amount: b.deje, team: t, ordago: b.ordago });
        if (pay(s, lance, t, b.deje, 'deje')) return finishGame(s, {});
        advanceAfter(s, lance);
        return;
      }
      break;
  }
  s.turn = b.queue[0];
}

// ───────────────────────── vista ─────────────────────────

export function viewFor(s: MusState, seat: number): MusView {
  const st = seat as Seat;
  const ended = s.phase === 'finMano' || s.phase === 'finPartida';
  const visibleSena = (x: Seat) => x === st || x === partnerOf(st);
  return {
    seat: st,
    phase: s.phase,
    turn: s.turn,
    dealer: s.dealer,
    mano: manoOf(s.dealer),
    handNumber: s.handNumber,
    scores: [s.scores[0], s.scores[1]],
    config: s.config,
    hand: [...s.hands[st]],
    musRound: s.musRound,
    pendingDiscard: [...s.pendingDiscard],
    lastDiscardCounts: [...s.lastDiscardCounts],
    betting: s.betting
      ? { ...s.betting, participants: [...s.betting.participants], queue: [...s.betting.queue] }
      : null,
    lances: s.lances.map((l) => ({ ...l })),
    awards: [...s.awards],
    declarations: {
      pares: s.declarations.pares ? [...s.declarations.pares] : null,
      juego: s.declarations.juego ? [...s.declarations.juego] : null,
    },
    senas: s.senas.filter((x) => visibleSena(x.seat)),
    log: s.history.filter(
      (e: LogEntry) => e.hand === s.handNumber && (e.action.type !== 'sena' || visibleSena(e.seat)),
    ),
    revealed: ended ? s.hands.map((h) => [...h]) : null,
    summary: s.summary,
    winner: s.winner,
    legal: legalActions(s, st),
  };
}

// ───────────────────────── módulo ─────────────────────────

export const MusGame: GameModule<MusConfig, MusState, MusAction, MusView> = {
  id: 'mus',
  minPlayers: 4,
  maxPlayers: 4,
  defaultConfig: () => ({ ...DEFAULT_CONFIG }),
  create: (config, seed) => createGame(config, seed),
  actors,
  legalActions,
  validate,
  apply: applyAction,
  view: viewFor,
  isOver: (s) => s.phase === 'finPartida',
};
