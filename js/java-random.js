/**
 * java.util.Random (LegacyRandomSource) — 48-bit LCG.
 * Matches Minecraft Java Edition exactly.
 */
const MULT = 0x5deece66dn;
const ADD = 0xbn;
const MASK = (1n << 48n) - 1n;
export const LCG_INVERSE = 0xdfe05bcb1365n;

export function asInt32(n) {
  return Number(BigInt.asIntN(32, BigInt(n)));
}

export function asInt64(n) {
  return BigInt.asIntN(64, BigInt(n));
}

export class JavaRandom {
  constructor(seed = 0n) {
    this.seed = 0n;
    this.setSeed(seed);
  }

  setSeed(seed) {
    this.seed = (BigInt(seed) ^ MULT) & MASK;
  }

  getSeed() {
    return this.seed;
  }

  next(bits) {
    this.seed = (this.seed * MULT + ADD) & MASK;
    return Number(this.seed >> (48n - BigInt(bits)));
  }

  nextInt(bound) {
    if (bound === undefined) {
      return asInt32(this.next(32));
    }
    if (bound <= 0) throw new Error("bound must be positive");
    let r = this.next(31);
    const m = bound - 1;
    if ((bound & m) === 0) {
      r = Number((BigInt(bound) * BigInt(r)) >> 31n);
    } else {
      for (let u = r; (asInt32(u - (r = u % bound) + m) | 0) < 0; u = this.next(31)) {
        /* reject modulo-biased samples, same as Java */
      }
    }
    return r;
  }

  nextLong() {
    const hi = BigInt.asIntN(32, BigInt(this.next(32)));
    const lo = BigInt.asIntN(32, BigInt(this.next(32)));
    return (hi << 32n) + lo;
  }

  nextBoolean() {
    return this.next(1) !== 0;
  }

  nextFloat() {
    return this.next(24) / 16777216;
  }

  nextDouble() {
    const hi = this.next(26);
    const lo = this.next(27);
    return (hi * 134217728 + lo) / 9007199254740992;
  }
}

/** Undo one LCG step (next()). */
export function lcgPrev(state) {
  return ((BigInt(state) - ADD) * LCG_INVERSE) & MASK;
}

export function javaStringHash(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = Math.imul(h, 31) + str.charCodeAt(i);
    h |= 0;
  }
  return h;
}

/**
 * Parse a Minecraft Java seed.
 * Numeric strings (optional leading minus) become signed 64-bit longs.
 * Anything else uses Java String.hashCode().
 */
export function parseSeed(input) {
  const s = String(input ?? "").trim();
  if (s === "") {
    const mixed = (BigInt(Date.now()) ^ (BigInt(Math.floor(Math.random() * 0x100000000)) << 20n)) & ((1n << 64n) - 1n);
    return asInt64(mixed);
  }
  if (/^-?\d+$/.test(s)) {
    return asInt64(BigInt(s));
  }
  return BigInt(javaStringHash(s));
}

export function formatSeed(seed) {
  return asInt64(seed).toString();
}
