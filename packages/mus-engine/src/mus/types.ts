import type { RngState } from '../core/rng.js';
import type { Card } from './cards.js';

/** Asientos en orden de juego (antihorario): 0 → 1 → 2 → 3. Compañeros: 0-2 y 1-3. */
export type Seat = 0 | 1 | 2 | 3;
export type Team = 0 | 1;
export const teamOf = (s: number): Team => (s % 2) as Team;
export const partnerOf = (s: number): Seat => ((s + 2) % 4) as Seat;

export type Lance = 'grande' | 'chica' | 'pares' | 'juego' | 'punto';
export const BETTING_LANCES: readonly Lance[] = ['grande', 'chica', 'pares', 'juego', 'punto'];

export type Phase =
  | 'mus'
  | 'descarte'
  | 'grande'
  | 'chica'
  | 'pares'
  | 'juego'
  | 'punto'
  | 'finMano'
  | 'finPartida';

/** Señas clásicas. Sólo las ve el compañero. */
export type Sena =
  | 'dosReyes'
  | 'tresReyes'
  | 'dosAses'
  | 'tresAses'
  | 'duples'
  | 'medias'
  | 'treintaYUna'
  | 'ciego';
export const SENAS: readonly Sena[] = [
  'dosReyes', 'tresReyes', 'dosAses', 'tresAses', 'duples', 'medias', 'treintaYUna', 'ciego',
];

/**
 * Acciones. `envido.amount`:
 *  - sin apuesta previa: tantos que se envidan (≥2),
 *  - respondiendo a una apuesta: tantos que se suben ("N más", ≥2).
 */
export type MusAction =
  | { type: 'mus' }
  | { type: 'noMus' }
  | { type: 'descarte'; cards: Card[] }
  | { type: 'paso' }
  | { type: 'envido'; amount: number }
  | { type: 'quiero' }
  | { type: 'noQuiero' }
  | { type: 'ordago' }
  | { type: 'sena'; sena: Sena }
  | { type: 'siguienteMano' };

export interface MusConfig {
  /** 8 reyes (treses=reyes, doses=ases) o 4 reyes. */
  reyes: 4 | 8;
  /** Tantos para ganar la partida. */
  target: number;
  /** Máximo de señas por jugador y mano (antiflood). */
  maxSenasPerHand: number;
  /** Repartidor de la primera mano (por defecto 3, así el asiento 0 es mano). */
  firstDealer: Seat;
}

export const DEFAULT_CONFIG: MusConfig = { reyes: 8, target: 40, maxSenasPerHand: 3, firstDealer: 3 };

export interface Betting {
  lance: Lance;
  /** Jugadores que intervienen, en orden desde la mano. */
  participants: Seat[];
  /** Cola de quién debe hablar; el primero tiene el turno. */
  queue: Seat[];
  /** Tantos en juego si se quiere (0 = nadie ha envidado). */
  proposed: number;
  /** Tantos que se cobran si no se quiere. */
  deje: number;
  bettingTeam: Team | null;
  ordago: boolean;
}

export type LanceOutcome =
  /** Todos pasaron. */
  | 'paso'
  | 'querido'
  | 'noQuerido'
  /** Pares/juego: sólo una pareja lo tiene, no se apuesta. */
  | 'sinApuesta'
  /** Pares: nadie tiene. */
  | 'noHay';

export interface LanceRecord {
  lance: Lance;
  outcome: LanceOutcome;
  /** querido: tantos en juego; noQuerido: deje cobrado. */
  amount: number;
  /** Pareja que cobró el deje (noQuerido). */
  team: Team | null;
  ordago: boolean;
}

export type AwardKind = 'deje' | 'apuesta' | 'paso' | 'jugada' | 'ordago';
export interface Award {
  lance: Lance;
  team: Team;
  points: number;
  kind: AwardKind;
}

export interface HandSummary {
  hands: Card[][];
  /** Ganador (asiento) de cada lance jugado. */
  winners: Partial<Record<Lance, Seat>>;
  /** Todos los tantos de la mano en orden de cobro (dejes incluidos). */
  awards: Award[];
  /** La partida terminó en esta mano. */
  gameOver: boolean;
}

export interface LogEntry {
  hand: number;
  phase: Phase;
  seat: Seat;
  action: MusAction;
}

export interface SenaRecord {
  seat: Seat;
  sena: Sena;
}

export interface MusState {
  config: MusConfig;
  /** Semilla de la partida (auditoría). */
  seed: string;
  rng: RngState;
  scores: [number, number];
  dealer: Seat;
  handNumber: number;
  deck: Card[];
  discardPile: Card[];
  hands: Card[][];
  phase: Phase;
  turn: Seat | null;
  musRound: number;
  pendingDiscard: Seat[];
  lastDiscardCounts: number[];
  betting: Betting | null;
  lances: LanceRecord[];
  declarations: { pares: boolean[] | null; juego: boolean[] | null };
  senas: SenaRecord[];
  /** Tantos cobrados en la mano en curso (dejes inmediatos y conteo). */
  awards: Award[];
  summary: HandSummary | null;
  winner: Team | null;
  /** Historial completo de acciones de la partida (para reproducir y auditar). */
  history: LogEntry[];
}

export interface MusView {
  seat: Seat;
  phase: Phase;
  turn: Seat | null;
  dealer: Seat;
  mano: Seat;
  handNumber: number;
  scores: [number, number];
  config: MusConfig;
  hand: Card[];
  musRound: number;
  pendingDiscard: Seat[];
  lastDiscardCounts: number[];
  betting: Betting | null;
  lances: LanceRecord[];
  awards: Award[];
  declarations: { pares: boolean[] | null; juego: boolean[] | null };
  /** Señas propias y del compañero (nunca las de los rivales). */
  senas: SenaRecord[];
  /** Log de la mano actual (sin señas rivales). */
  log: LogEntry[];
  /** Todas las manos, sólo al terminar la mano. */
  revealed: Card[][] | null;
  summary: HandSummary | null;
  winner: Team | null;
  legal: MusAction[];
}
