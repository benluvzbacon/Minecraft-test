/**
 * Spawn-biome guess for Java 1.18+ style worlds.
 *
 * This is not cubiomes. It uses the same idea Minecraft does — several
 * climate noises from the world seed, then a walk out from 0,0 looking
 * for land you can spawn on — but the noise tables are a compact stand-in.
 * Same seed always gives the same guess.
 */
import { JavaRandom, asInt64 } from "./java-random.js";

function fade(t) {
  return t * t * t * (t * (t * 6 - 15) + 10);
}
function lerp(t, a, b) {
  return a + t * (b - a);
}
function grad(hash, x, y, z) {
  const h = hash & 15;
  const u = h < 8 ? x : y;
  const v = h < 4 ? y : h === 12 || h === 14 ? x : z;
  return ((h & 1) === 0 ? u : -u) + ((h & 2) === 0 ? v : -v);
}

class ImprovedNoise {
  constructor(rand) {
    this.xo = rand.nextDouble() * 256;
    this.yo = rand.nextDouble() * 256;
    this.zo = rand.nextDouble() * 256;
    this.p = new Uint8Array(256);
    for (let i = 0; i < 256; i++) this.p[i] = i;
    for (let i = 0; i < 256; i++) {
      const j = i + rand.nextInt(256 - i);
      const tmp = this.p[i];
      this.p[i] = this.p[j];
      this.p[j] = tmp;
    }
  }
  perm(i) {
    return this.p[i & 255];
  }
  sample(x, y, z) {
    x += this.xo;
    y += this.yo;
    z += this.zo;
    const xi = Math.floor(x);
    const yi = Math.floor(y);
    const zi = Math.floor(z);
    const xf = x - xi;
    const yf = y - yi;
    const zf = z - zi;
    const u = fade(xf);
    const v = fade(yf);
    const w = fade(zf);
    const A = this.perm(xi) + yi;
    const AA = this.perm(A) + zi;
    const AB = this.perm(A + 1) + zi;
    const B = this.perm(xi + 1) + yi;
    const BA = this.perm(B) + zi;
    const BB = this.perm(B + 1) + zi;
    return lerp(
      w,
      lerp(
        v,
        lerp(u, grad(this.perm(AA), xf, yf, zf), grad(this.perm(BA), xf - 1, yf, zf)),
        lerp(u, grad(this.perm(AB), xf, yf - 1, zf), grad(this.perm(BB), xf - 1, yf - 1, zf))
      ),
      lerp(
        v,
        lerp(u, grad(this.perm(AA + 1), xf, yf, zf - 1), grad(this.perm(BA + 1), xf - 1, yf, zf - 1)),
        lerp(u, grad(this.perm(AB + 1), xf, yf - 1, zf - 1), grad(this.perm(BB + 1), xf - 1, yf - 1, zf - 1))
      )
    );
  }
}

function octaves(rand, count) {
  const layers = [];
  for (let i = 0; i < count; i++) layers.push(new ImprovedNoise(rand));
  return layers;
}

function fbm(layers, x, z, xzScale) {
  let sum = 0;
  let amp = 1;
  let freq = xzScale;
  let norm = 0;
  for (const layer of layers) {
    sum += layer.sample(x * freq, 0, z * freq) * amp;
    norm += amp;
    amp *= 0.5;
    freq *= 2;
  }
  return sum / (norm || 1);
}

const FAMILIES = {
  plains: { label: "Plains", icon: "🌾" },
  forest: { label: "Forest", icon: "🌲" },
  desert: { label: "Desert", icon: "🏜️" },
  snow: { label: "Snowy", icon: "❄️" },
  ocean: { label: "Ocean", icon: "🌊" },
  jungle: { label: "Jungle", icon: "🌴" },
  savanna: { label: "Savanna", icon: "🦁" },
  mountains: { label: "Mountains", icon: "⛰️" },
  swamp: { label: "Swamp", icon: "🐸" },
  mushroom: { label: "Mushroom", icon: "🍄" },
};

export const SPAWN_FAMILY_OPTIONS = [
  { id: "", name: "Any start biome" },
  { id: "plains", name: "Plains" },
  { id: "forest", name: "Forest / taiga" },
  { id: "desert", name: "Desert" },
  { id: "snow", name: "Snowy" },
  { id: "jungle", name: "Jungle" },
  { id: "savanna", name: "Savanna" },
  { id: "mountains", name: "Mountains" },
  { id: "swamp", name: "Swamp" },
  { id: "ocean", name: "Ocean (island start)" },
];

const SPAWNABLE = new Set(["plains", "forest", "desert", "snow", "jungle", "savanna", "mountains", "swamp"]);

function classify(temp, humid, cont, eros, weird) {
  if (cont < -0.42) {
    if (temp < -0.4) return { id: "frozen_ocean", name: "Frozen ocean", family: "ocean" };
    if (temp > 0.5) return { id: "lukewarm_ocean", name: "Lukewarm ocean", family: "ocean" };
    return { id: "ocean", name: "Ocean", family: "ocean" };
  }
  if (cont < -0.19) {
    if (temp < -0.35) return { id: "snowy_beach", name: "Snowy beach", family: "ocean" };
    if (eros < -0.3) return { id: "stony_shore", name: "Stony shore", family: "ocean" };
    return { id: "beach", name: "Beach", family: "ocean" };
  }

  if (weird > 0.55 && cont > 0.2 && humid > 0.2) {
    return { id: "mushroom_fields", name: "Mushroom fields", family: "mushroom" };
  }

  if (eros < -0.45 && cont > 0.15) {
    if (temp < -0.3) return { id: "jagged_peaks", name: "Jagged peaks", family: "mountains" };
    if (temp > 0.45) return { id: "stony_peaks", name: "Stony peaks", family: "mountains" };
    return { id: "windswept_hills", name: "Windswept hills", family: "mountains" };
  }

  if (temp < -0.45) {
    if (humid > 0.25) return { id: "snowy_taiga", name: "Snowy taiga", family: "snow" };
    if (weird > 0.4) return { id: "ice_spikes", name: "Ice spikes", family: "snow" };
    return { id: "snowy_plains", name: "Snowy plains", family: "snow" };
  }

  if (temp > 0.55) {
    if (humid > 0.35) return { id: "jungle", name: "Jungle", family: "jungle" };
    if (humid < -0.1) return { id: "desert", name: "Desert", family: "desert" };
    return { id: "savanna", name: "Savanna", family: "savanna" };
  }

  if (temp > 0.2 && humid < -0.25) return { id: "savanna", name: "Savanna", family: "savanna" };
  if (humid > 0.4 && eros > 0.2) return { id: "swamp", name: "Swamp", family: "swamp" };
  if (humid > 0.2) {
    if (temp < -0.1) return { id: "taiga", name: "Taiga", family: "forest" };
    if (weird > 0.25) return { id: "flower_forest", name: "Flower forest", family: "forest" };
    if (humid > 0.45) return { id: "dark_forest", name: "Dark forest", family: "forest" };
    return { id: "forest", name: "Forest", family: "forest" };
  }
  if (eros < -0.15) return { id: "meadow", name: "Meadow", family: "plains" };
  return { id: "plains", name: "Plains", family: "plains" };
}

const climateCache = new Map();

function climateFor(seed) {
  const key = seed.toString();
  if (climateCache.has(key)) return climateCache.get(key);
  if (climateCache.size > 400) climateCache.clear();
  const s = asInt64(seed);
  const temp = octaves(new JavaRandom(s ^ 0x74656d70n), 4);
  const humid = octaves(new JavaRandom(s ^ 0x68756d6964n), 4);
  const cont = octaves(new JavaRandom(s ^ 0x636f6e74n), 4);
  const eros = octaves(new JavaRandom(s ^ 0x65726f73n), 4);
  const weird = octaves(new JavaRandom(s ^ 0x77656972n), 4);
  const pack = { temp, humid, cont, eros, weird };
  climateCache.set(key, pack);
  return pack;
}

function sampleAt(seed, x, z) {
  const n = climateFor(seed);
  const scale = 0.0045;
  const temp = fbm(n.temp, x, z, scale);
  const humid = fbm(n.humid, x + 80, z - 40, scale * 1.05);
  const cont = fbm(n.cont, x - 30, z + 90, scale * 0.7);
  const eros = fbm(n.eros, x + 200, z + 20, scale * 0.9);
  const weird = fbm(n.weird, x - 120, z + 160, scale * 1.2);
  const biome = classify(temp, humid, cont, eros, weird);
  return { ...biome, x, z, temp, humid, cont, eros, weird };
}

function spiralPoints() {
  const pts = [{ x: 0, z: 0 }];
  for (const r of [64, 128, 224, 352, 512]) {
    for (let i = 0; i < 8; i++) {
      const a = (Math.PI / 4) * i;
      pts.push({ x: Math.round(Math.cos(a) * r), z: Math.round(Math.sin(a) * r) });
    }
  }
  return pts;
}

const SPIRAL = spiralPoints();

export function guessSpawnBiome(worldSeed) {
  const seed = asInt64(worldSeed);
  let fallback = null;
  for (const p of SPIRAL) {
    const here = sampleAt(seed, p.x, p.z);
    if (!fallback) fallback = here;
    if (SPAWNABLE.has(here.family)) {
      const fam = FAMILIES[here.family];
      return {
        ...here,
        icon: fam.icon,
        familyLabel: fam.label,
        guess: true,
      };
    }
  }
  const fam = FAMILIES[fallback.family] || FAMILIES.ocean;
  return { ...fallback, icon: fam.icon, familyLabel: fam.label, guess: true };
}

export function biomeMatchesFilter(spawn, familyId) {
  if (!familyId) return true;
  return spawn.family === familyId;
}
