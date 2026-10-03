import { describe, expect, it } from 'vitest';
import { createRng, cyrb128, nextInt, rngFromSeed, shuffleInPlace } from '../src/core/rng.js';

describe('rng', () => {
  it('es determinista por semilla', () => {
    const a = rngFromSeed('x');
    const b = rngFromSeed('x');
    const c = rngFromSeed('y');
    const sa = Array.from({ length: 20 }, () => nextInt(a, 1000));
    expect(Array.from({ length: 20 }, () => nextInt(b, 1000))).toEqual(sa);
    expect(Array.from({ length: 20 }, () => nextInt(c, 1000))).not.toEqual(sa);
  });
  it('cyrb128 produce uint32', () => {
    for (const v of cyrb128('hola')) expect(v >>> 0).toBe(v);
  });
  it('nextInt cubre el rango y es aproximadamente uniforme', () => {
    const s = rngFromSeed('u');
    const counts = new Array(10).fill(0);
    for (let i = 0; i < 20000; i++) counts[nextInt(s, 10)]++;
    for (const n of counts) expect(Math.abs(n - 2000)).toBeLessThan(250);
  });
  it('shuffle es una permutación', () => {
    const arr = shuffleInPlace(Array.from({ length: 40 }, (_, i) => i), rngFromSeed('p'));
    expect([...arr].sort((a, b) => a - b)).toEqual(Array.from({ length: 40 }, (_, i) => i));
  });
  it('createRng.float en [0,1)', () => {
    const r = createRng('f');
    for (let i = 0; i < 1000; i++) {
      const f = r.float();
      expect(f).toBeGreaterThanOrEqual(0);
      expect(f).toBeLessThan(1);
    }
  });
});
