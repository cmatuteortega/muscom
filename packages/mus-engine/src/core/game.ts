/**
 * Interfaz común para módulos de juego de baraja española (Mus, Tute, Brisca, Truco…).
 * El servidor sólo conoce esta interfaz; cada juego es un módulo que la implementa.
 */
export interface GameModule<Config, State, Action, View> {
  readonly id: string;
  readonly minPlayers: number;
  readonly maxPlayers: number;
  defaultConfig(): Config;
  /** Crea la partida. `seed` debe venir de un CSPRNG y guardarse para auditoría. */
  create(config: Config, seed: string): State;
  /** Asiento(s) que pueden actuar ahora. */
  actors(state: State): number[];
  /** Lista canónica (ordenada, determinista) de acciones legales. */
  legalActions(state: State, seat: number): Action[];
  /** Validación general (admite valores no listados en legalActions, p.ej. envidos de 7). */
  validate(state: State, seat: number, action: Action): string | null;
  /** Aplica una acción. Pura: no muta `state`. Lanza IllegalActionError si no es válida. */
  apply(state: State, seat: number, action: Action): State;
  /** Vista redactada para un asiento: nunca contiene cartas ajenas ocultas. */
  view(state: State, seat: number): View;
  isOver(state: State): boolean;
}

export class IllegalActionError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'IllegalActionError';
  }
}
