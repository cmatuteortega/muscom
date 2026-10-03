/**
 * PRNG determinista y portable (idéntico en TS y Kotlin).
 *
 * El servidor genera la semilla con un CSPRNG (crypto.randomBytes) y la guarda
 * para auditoría; el motor sólo deriva el barajado de esa semilla, de modo que
 * cualquier partida se puede reproducir exactamente a partir de (semilla, acciones).
 */

/** Hash de cadena a 128 bits (cyrb128). */
export function cyrb128(str: string): [number, number, number, number] {
  let h1 = 1779033703,
    h2 = 3144134277,
    h3 = 1013904242,
    h4 = 2773480762;
  for (let i = 0; i < str.length; i++) {
    const k = str.charCodeAt(i);
    h1 = h2 ^ Math.imul(h1 ^ k, 597399067);
    h2 = h3 ^ Math.imul(h2 ^ k, 2869860233);
    h3 = h4 ^ Math.imul(h3 ^ k, 951274213);
    h4 = h1 ^ Math.imul(h4 ^ k, 2716044179);
  }
  h1 = Math.imul(h3 ^ (h1 >>> 18), 597399067);
  h2 = Math.imul(h4 ^ (h2 >>> 22), 2869860233);
  h3 = Math.imul(h1 ^ (h3 >>> 17), 951274213);
  h4 = Math.imul(h2 ^ (h4 >>> 19), 2716044179);
  h1 ^= h2 ^ h3 ^ h4;
  h2 ^= h1;
  h3 ^= h1;
  h4 ^= h1;
  return [h1 >>> 0, h2 >>> 0, h3 >>> 0, h4 >>> 0];
}

export type RngState = [number, number, number, number];

/** sfc32: devuelve el siguiente uint32 y muta el estado. */
export function nextUint32(s: RngState): number {
  let [a, b, c, d] = s;
  a >>>= 0;
  b >>>= 0;
  c >>>= 0;
  d >>>= 0;
  let t = (a + b) | 0;
  a = b ^ (b >>> 9);
  b = (c + (c << 3)) | 0;
  c = (c << 21) | (c >>> 11);
  d = (d + 1) | 0;
  t = (t + d) | 0;
  c = (c + t) | 0;
  s[0] = a >>> 0;
  s[1] = b >>> 0;
  s[2] = c >>> 0;
  s[3] = d >>> 0;
  return t >>> 0;
}

/** Entero uniforme en [0, n) sin sesgo (rechazo). */
export function nextInt(s: RngState, n: number): number {
  if (n <= 0 || n > 0x100000000) throw new Error(`nextInt: rango inválido ${n}`);
  const limit = 0x100000000 - (0x100000000 % n);
  for (;;) {
    const x = nextUint32(s);
    if (x < limit) return x % n;
  }
}

/** Flotante en [0,1). */
export function nextFloat(s: RngState): number {
  return nextUint32(s) / 0x100000000;
}

export function rngFromSeed(seed: string): RngState {
  const s = cyrb128(seed);
  // Calentamiento estándar de sfc32.
  for (let i = 0; i < 15; i++) nextUint32(s);
  return s;
}

/** Fisher–Yates in situ. */
export function shuffleInPlace<T>(arr: T[], s: RngState): T[] {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = nextInt(s, i + 1);
    const tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }
  return arr;
}

/** Interfaz mínima de RNG que usan los bots. */
export interface Rng {
  float(): number;
  int(n: number): number;
}

export function createRng(seed: string): Rng {
  const s = rngFromSeed(seed);
  return { float: () => nextFloat(s), int: (n) => nextInt(s, n) };
}
