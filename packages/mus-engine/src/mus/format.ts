import { cardToString } from './cards.js';
import type { MusAction } from './types.js';

/** Codificación compacta de acciones (usada en logs, CLI y vectores de conformidad). */
export function encodeAction(a: MusAction): string {
  switch (a.type) {
    case 'envido': return `envido:${a.amount}`;
    case 'descarte': return `descarte:${a.cards.join(',')}`;
    case 'sena': return `sena:${a.sena}`;
    default: return a.type;
  }
}

export function decodeAction(s: string): MusAction {
  const [type, arg] = s.split(':');
  switch (type) {
    case 'envido': return { type, amount: Number(arg) };
    case 'descarte': return { type, cards: arg.split(',').map(Number) };
    case 'sena': return { type, sena: arg as never };
    default: return { type } as MusAction;
  }
}

export function describeAction(a: MusAction): string {
  switch (a.type) {
    case 'mus': return 'Mus';
    case 'noMus': return 'No hay mus';
    case 'descarte': return `Descarta ${a.cards.length} (${a.cards.map(cardToString).join(' ')})`;
    case 'paso': return 'Paso';
    case 'envido': return `Envido ${a.amount}`;
    case 'quiero': return 'Quiero';
    case 'noQuiero': return 'No quiero';
    case 'ordago': return '¡Órdago!';
    case 'sena': return `Seña: ${a.sena}`;
    case 'siguienteMano': return 'Siguiente mano';
  }
}
