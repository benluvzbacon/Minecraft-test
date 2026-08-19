/**
 * Minecraft Java Edition worldgen that can be computed exactly
 * from the world seed (no biome / surface simulation).
 *
 * End spikes: 1.9+ (unchanged through 1.21)
 * Structures: Java 1.18.2 – 1.21 placement (48-bit structure seed)
 */
import { JavaRandom, asInt64, lcgPrev } from "./java-random.js";

export const PILLAR_HEIGHTS = [76, 79, 82, 85, 88, 91, 94, 97, 100, 103];
export const PILLAR_RADII = [2, 2, 2, 3, 3, 3, 4, 4, 4, 5];

/** Generation order around the exit portal (j = 0..9). Positions are seed-independent. */
export const PILLAR_SLOTS = [
  { id: 0, x: 42, z: 0, angle: 0, name: "+X  spawn-facing" },
  { id: 1, x: 33, z: 24, angle: 36, name: "+X +Z" },
  { id: 2, x: 12, z: 39, angle: 72, name: "+Z" },
  { id: 3, x: -13, z: 39, angle: 108, name: "−X +Z" },
  { id: 4, x: -34, z: 24, angle: 144, name: "−X +Z far" },
  { id: 5, x: -42, z: -1, angle: 180, name: "−X  far side" },
  { id: 6, x: -34, z: -25, angle: 216, name: "−X −Z" },
  { id: 7, x: -13, z: -40, angle: 252, name: "−Z" },
  { id: 8, x: 12, z: -40, angle: 288, name: "+X −Z" },
  { id: 9, x: 33, z: -25, angle: 324, name: "+X −Z near" },
];

export function getPillarSeed(worldSeed) {
  const rand = new JavaRandom(asInt64(worldSeed));
  return Number(rand.nextLong() & 65535n);
}

function collectionsShuffle(list, rand) {
  for (let i = list.length; i > 1; i--) {
    const j = rand.nextInt(i);
    const tmp = list[i - 1];
    list[i - 1] = list[j];
    list[j] = tmp;
  }
  return list;
}

export function pillarsFromPillarSeed(pillarSeed) {
  const order = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];
  collectionsShuffle(order, new JavaRandom(BigInt(pillarSeed) & 65535n));
  return order.map((sizeIndex, slot) => {
    const slotInfo = PILLAR_SLOTS[slot];
    return {
      slot,
      x: slotInfo.x,
      z: slotInfo.z,
      angle: slotInfo.angle,
      name: slotInfo.name,
      sizeIndex,
      height: 76 + sizeIndex * 3,
      radius: 2 + Math.floor(sizeIndex / 3),
      guarded: sizeIndex === 1 || sizeIndex === 2,
      crystalY: 76 + sizeIndex * 3,
    };
  });
}

export function getEndPillars(worldSeed) {
  const pillarSeed = getPillarSeed(worldSeed);
  return {
    worldSeed: asInt64(worldSeed).toString(),
    pillarSeed,
    pillars: pillarsFromPillarSeed(pillarSeed),
  };
}

/**
 * Build a 48-bit world seed whose End pillar seed equals `pillarSeed`.
 * `extra` is a 32-bit free parameter (0 .. 2^32-1) covering every such seed.
 */
export function worldSeedFromPillar(pillarSeed, extra = 0n) {
  const e = BigInt(extra) & 0xffffffffn;
  const high16 = e >> 16n;
  const low16 = e & 0xffffn;
  let state = (high16 << 32n) | (BigInt(pillarSeed & 65535) << 16n) | low16;
  state = lcgPrev(state);
  state = lcgPrev(state);
  return asInt64(state ^ 0x5deece66dn);
}

export function isSlimeChunk(worldSeed, chunkX, chunkZ) {
  const seed = asInt64(worldSeed);
  const cx = BigInt.asIntN(32, BigInt(chunkX));
  const cz = BigInt.asIntN(32, BigInt(chunkZ));
  let rnd = seed;
  rnd += BigInt.asIntN(32, cx * cx * 0x4c1906n);
  rnd += BigInt.asIntN(32, cx * 0x5ac0dbn);
  rnd += BigInt.asIntN(32, cz * cz) * 0x4307a7n;
  rnd += BigInt.asIntN(32, cz * 0x5f24fn);
  rnd ^= 0x3ad8025fn;
  const rand = new JavaRandom(rnd);
  return rand.nextInt(10) === 0;
}

export function floorDiv(a, b) {
  return Math.floor(a / b);
}

export const STRUCTURES = {
  village: { id: "village", name: "Village", salt: 10387312, spacing: 34, range: 26, dim: "overworld", spread: "linear", icon: "🏠" },
  desert_pyramid: { id: "desert_pyramid", name: "Desert pyramid", salt: 14357617, spacing: 32, range: 24, dim: "overworld", spread: "linear", icon: "🏜️" },
  jungle_pyramid: { id: "jungle_pyramid", name: "Jungle temple", salt: 14357619, spacing: 32, range: 24, dim: "overworld", spread: "linear", icon: "🌴" },
  igloo: { id: "igloo", name: "Igloo", salt: 14357618, spacing: 32, range: 24, dim: "overworld", spread: "linear", icon: "🧊" },
  swamp_hut: { id: "swamp_hut", name: "Swamp hut", salt: 14357620, spacing: 32, range: 24, dim: "overworld", spread: "linear", icon: "🧙" },
  outpost: { id: "outpost", name: "Pillager outpost", salt: 165745296, spacing: 32, range: 24, dim: "overworld", spread: "linear", icon: "🚩", frequency: 0.2 },
  monument: { id: "monument", name: "Ocean monument", salt: 10387313, spacing: 32, range: 27, dim: "overworld", spread: "triangular", icon: "🌊" },
  mansion: { id: "mansion", name: "Woodland mansion", salt: 10387319, spacing: 80, range: 60, dim: "overworld", spread: "triangular", icon: "🏚️" },
  ruined_portal: { id: "ruined_portal", name: "Ruined portal", salt: 34222645, spacing: 40, range: 25, dim: "overworld", spread: "linear", icon: "🟣" },
  shipwreck: { id: "shipwreck", name: "Shipwreck", salt: 165745295, spacing: 24, range: 20, dim: "overworld", spread: "linear", icon: "⛵" },
  ocean_ruin: { id: "ocean_ruin", name: "Ocean ruin", salt: 14357621, spacing: 20, range: 12, dim: "overworld", spread: "linear", icon: "🏛️" },
  ancient_city: { id: "ancient_city", name: "Ancient city", salt: 20083232, spacing: 24, range: 16, dim: "overworld", spread: "linear", icon: "👁️" },
  trail_ruins: { id: "trail_ruins", name: "Trail ruins", salt: 83469867, spacing: 34, range: 26, dim: "overworld", spread: "linear", icon: "🧱" },
  trial_chambers: { id: "trial_chambers", name: "Trial chambers", salt: 94251327, spacing: 34, range: 22, dim: "overworld", spread: "linear", icon: "⚔️" },
  fortress: { id: "fortress", name: "Nether fortress", salt: 30084232, spacing: 27, range: 23, dim: "nether", spread: "linear", icon: "🔥" },
  bastion: { id: "bastion", name: "Bastion remnant", salt: 30084232, spacing: 27, range: 23, dim: "nether", spread: "linear", icon: "🐷" },
  end_city: { id: "end_city", name: "End city", salt: 10387313, spacing: 20, range: 9, dim: "end", spread: "triangular", icon: "🏙️" },
};

function setLargeFeatureSeed(worldSeed, regionX, regionZ, salt) {
  const mixed =
    BigInt(regionX) * 341873128712n +
    BigInt(regionZ) * 132897987541n +
    asInt64(worldSeed) +
    BigInt(salt);
  return new JavaRandom(mixed);
}

function featureChunkInRegion(cfg, worldSeed, rx, rz) {
  const rand = setLargeFeatureSeed(worldSeed, rx, rz, cfg.salt);
  if (cfg.spread === "triangular") {
    const x = (rand.nextInt(cfg.range) + rand.nextInt(cfg.range)) >> 1;
    const z = (rand.nextInt(cfg.range) + rand.nextInt(cfg.range)) >> 1;
    return { x, z };
  }
  return { x: rand.nextInt(cfg.range), z: rand.nextInt(cfg.range) };
}

export function getStructurePos(cfg, worldSeed, regionX, regionZ) {
  const c = featureChunkInRegion(cfg, worldSeed, regionX, regionZ);
  return {
    chunkX: regionX * cfg.spacing + c.x,
    chunkZ: regionZ * cfg.spacing + c.z,
    x: (regionX * cfg.spacing + c.x) << 4,
    z: (regionZ * cfg.spacing + c.z) << 4,
  };
}

function setAttemptSeed(worldSeed, chunkX, chunkZ) {
  let s = asInt64(worldSeed);
  s ^= BigInt(chunkX >> 4) ^ (BigInt(chunkZ >> 4) << 4n);
  const rand = new JavaRandom(s);
  rand.next(31);
  return rand;
}

function chunkGenerateRnd(worldSeed, chunkX, chunkZ) {
  const rand = new JavaRandom(asInt64(worldSeed));
  const a = rand.nextLong();
  const b = rand.nextLong();
  return new JavaRandom(a * BigInt(chunkX) ^ b * BigInt(chunkZ) ^ asInt64(worldSeed));
}

export function structureAttemptValid(cfg, worldSeed, pos) {
  if (cfg.id === "outpost") {
    const rand = setAttemptSeed(worldSeed, pos.chunkX, pos.chunkZ);
    return rand.nextInt(5) === 0;
  }
  if (cfg.id === "end_city") {
    const dx = pos.x;
    const dz = pos.z;
    return dx * dx + dz * dz >= 1008 * 1008;
  }
  if (cfg.id === "bastion") {
    const rand = chunkGenerateRnd(worldSeed, pos.chunkX, pos.chunkZ);
    return rand.nextInt(5) >= 2;
  }
  if (cfg.id === "fortress") {
    const rand = chunkGenerateRnd(worldSeed, pos.chunkX, pos.chunkZ);
    return rand.nextInt(5) < 2;
  }
  return true;
}

export function isBuriedTreasureChunk(worldSeed, chunkX, chunkZ) {
  const mixed =
    BigInt(chunkX) * 341873128712n +
    BigInt(chunkZ) * 132897987541n +
    asInt64(worldSeed) +
    10387320n;
  const rand = new JavaRandom(mixed);
  return rand.nextFloat() < 0.01;
}

export function isMineshaftChunk(worldSeed, chunkX, chunkZ) {
  const rand = new JavaRandom(asInt64(worldSeed));
  const a = rand.nextLong();
  const b = rand.nextLong();
  const local = new JavaRandom(BigInt(chunkX) * a ^ BigInt(chunkZ) * b ^ asInt64(worldSeed));
  return local.nextDouble() < 0.004;
}

/** Approximate first stronghold (1.9+ rings). Exact block pos needs a biome search (~±112). */
export function firstStrongholdApprox(worldSeed) {
  const rand = new JavaRandom(asInt64(worldSeed));
  const angle = rand.nextDouble() * Math.PI * 2;
  const dist = 4 * 32 + (rand.nextDouble() - 0.5) * 32 * 2.5;
  const cx = Math.round(Math.cos(angle) * dist);
  const cz = Math.round(Math.sin(angle) * dist);
  return { x: cx * 16, z: cz * 16, chunkX: cx, chunkZ: cz, angle, distChunks: dist };
}

export function getShadowSeed(worldSeed) {
  return asInt64(-7379792620528906219n - asInt64(worldSeed));
}

export function findStructuresInRadius(worldSeed, cfg, radiusBlocks, originX = 0, originZ = 0) {
  const hits = [];
  const minX = originX - radiusBlocks;
  const maxX = originX + radiusBlocks;
  const minZ = originZ - radiusBlocks;
  const maxZ = originZ + radiusBlocks;
  const r0x = floorDiv(minX >> 4, cfg.spacing) - 1;
  const r1x = floorDiv(maxX >> 4, cfg.spacing) + 1;
  const r0z = floorDiv(minZ >> 4, cfg.spacing) - 1;
  const r1z = floorDiv(maxZ >> 4, cfg.spacing) + 1;
  const r2 = radiusBlocks * radiusBlocks;
  for (let rx = r0x; rx <= r1x; rx++) {
    for (let rz = r0z; rz <= r1z; rz++) {
      const pos = getStructurePos(cfg, worldSeed, rx, rz);
      const dx = pos.x - originX;
      const dz = pos.z - originZ;
      if (dx * dx + dz * dz > r2) continue;
      if (!structureAttemptValid(cfg, worldSeed, pos)) continue;
      hits.push({ ...pos, regionX: rx, regionZ: rz, type: cfg.id, name: cfg.name, icon: cfg.icon, dim: cfg.dim });
    }
  }
  hits.sort((a, b) => a.x * a.x + a.z * a.z - (b.x * b.x + b.z * b.z));
  return hits;
}

export function findBuriedTreasure(worldSeed, radiusChunks, originChunkX = 0, originChunkZ = 0) {
  const hits = [];
  for (let cz = originChunkZ - radiusChunks; cz <= originChunkZ + radiusChunks; cz++) {
    for (let cx = originChunkX - radiusChunks; cx <= originChunkX + radiusChunks; cx++) {
      if (isBuriedTreasureChunk(worldSeed, cx, cz)) {
        hits.push({ x: cx * 16 + 9, z: cz * 16 + 9, chunkX: cx, chunkZ: cz, type: "buried_treasure", name: "Buried treasure", icon: "💎" });
      }
    }
  }
  return hits;
}

export function slimeChunksInArea(worldSeed, x0, z0, x1, z1) {
  const out = [];
  for (let cz = z0; cz <= z1; cz++) {
    for (let cx = x0; cx <= x1; cx++) {
      if (isSlimeChunk(worldSeed, cx, cz)) out.push({ chunkX: cx, chunkZ: cz, x: cx * 16, z: cz * 16 });
    }
  }
  return out;
}

export function inspectSeed(worldSeed, options = {}) {
  const radius = options.radius ?? 800;
  const seed = asInt64(worldSeed);
  const end = getEndPillars(seed);
  const cages = end.pillars.filter((p) => p.guarded);
  const tallest = end.pillars.reduce((a, b) => (a.height >= b.height ? a : b));
  const shortest = end.pillars.reduce((a, b) => (a.height <= b.height ? a : b));
  const structures = {};
  for (const cfg of Object.values(STRUCTURES)) {
    structures[cfg.id] = findStructuresInRadius(seed, cfg, radius);
  }
  const slimeRadius = options.slimeChunks ?? 8;
  return {
    seed: seed.toString(),
    unsigned: (seed & ((1n << 64n) - 1n)).toString(),
    structureSeed: (seed & ((1n << 48n) - 1n)).toString(),
    shadowSeed: getShadowSeed(seed).toString(),
    pillarSeed: end.pillarSeed,
    pillars: end.pillars,
    cages,
    tallest,
    shortest,
    stronghold: firstStrongholdApprox(seed),
    slime: slimeChunksInArea(seed, -slimeRadius, -slimeRadius, slimeRadius, slimeRadius),
    treasures: findBuriedTreasure(seed, Math.min(24, Math.ceil(radius / 16))),
    structures,
  };
}
