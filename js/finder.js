import {
  PILLAR_SLOTS,
  pillarsFromPillarSeed,
  fullSeedFromPillar,
  getPillarSeed,
  isSlimeChunk,
  STRUCTURES,
  findStructuresInRadius,
  firstStrongholdApprox,
  isBuriedTreasureChunk,
} from "./worldgen.js";
import { asInt64 } from "./java-random.js";
import { guessSpawnBiome, biomeMatchesFilter } from "./biome.js";

function cageSlots(pillars) {
  return pillars.filter((p) => p.guarded).map((p) => p.slot).sort((a, b) => a - b);
}

function ringDistance(a, b) {
  const d = Math.abs(a - b);
  return Math.min(d, 10 - d);
}

export function evaluatePillars(pillars, spec) {
  if (!spec) return true;

  if (spec.slots) {
    for (let i = 0; i < 10; i++) {
      const want = spec.slots[i];
      if (!want) continue;
      const p = pillars[i];
      if (want.height != null && p.height !== want.height) return false;
      if (want.guarded === true && !p.guarded) return false;
      if (want.guarded === false && p.guarded) return false;
      if (want.minHeight != null && p.height < want.minHeight) return false;
      if (want.maxHeight != null && p.height > want.maxHeight) return false;
    }
  }

  const cages = cageSlots(pillars);
  if (spec.cageSlots) {
    const want = [...spec.cageSlots].sort((a, b) => a - b);
    if (want.length !== 2 || want[0] !== cages[0] || want[1] !== cages[1]) return false;
  }
  if (spec.cageOn && spec.cageOn.some((s) => !pillars[s].guarded)) return false;
  if (spec.cageOff && spec.cageOff.some((s) => pillars[s].guarded)) return false;

  if (spec.cagesAdjacent === true && ringDistance(cages[0], cages[1]) !== 1) return false;
  if (spec.cagesAdjacent === false && ringDistance(cages[0], cages[1]) === 1) return false;
  if (spec.cagesOpposite === true && ringDistance(cages[0], cages[1]) !== 5) return false;
  if (spec.minCageSeparation != null && ringDistance(cages[0], cages[1]) < spec.minCageSeparation) return false;
  if (spec.maxCageSeparation != null && ringDistance(cages[0], cages[1]) > spec.maxCageSeparation) return false;

  const tallest = pillars.reduce((a, b) => (a.height >= b.height ? a : b));
  const shortest = pillars.reduce((a, b) => (a.height <= b.height ? a : b));
  if (spec.tallestSlot != null && tallest.slot !== spec.tallestSlot) return false;
  if (spec.shortestSlot != null && shortest.slot !== spec.shortestSlot) return false;

  if (spec.spawnFacingMaxHeight != null && pillars[0].height > spec.spawnFacingMaxHeight) return false;
  if (spec.spawnFacingUnguarded && pillars[0].guarded) return false;

  return true;
}

export function collectMatchingPillarSeeds(spec) {
  const matches = [];
  for (let ps = 0; ps < 65536; ps++) {
    if (evaluatePillars(pillarsFromPillarSeed(ps), spec)) matches.push(ps);
  }
  return matches;
}

function countSlimeNearOrigin(seed, radiusChunks, minCount) {
  let n = 0;
  for (let z = -radiusChunks; z <= radiusChunks; z++) {
    for (let x = -radiusChunks; x <= radiusChunks; x++) {
      if (isSlimeChunk(seed, x, z)) {
        n++;
        if (n >= minCount) return n;
      }
    }
  }
  return n;
}

function randU32() {
  if (typeof crypto !== "undefined" && crypto.getRandomValues) {
    const a = new Uint32Array(1);
    crypto.getRandomValues(a);
    return BigInt(a[0]);
  }
  return BigInt(Math.floor(Math.random() * 0x100000000));
}

function randU16() {
  return randU32() & 0xffffn;
}

export function evaluateWorld(worldSeed, filters) {
  const seed = asInt64(worldSeed);
  const reasons = [];

  if (filters.pillarSeeds instanceof Set) {
    const ps = getPillarSeed(seed);
    if (!filters.pillarSeeds.has(ps)) return null;
  } else if (filters.pillars) {
    if (!evaluatePillars(pillarsFromPillarSeed(getPillarSeed(seed)), filters.pillars)) return null;
  }

  let spawn = null;
  if (filters.spawnBiome) {
    spawn = guessSpawnBiome(seed);
    if (!biomeMatchesFilter(spawn, filters.spawnBiome)) return null;
  }

  if (filters.slime) {
    const { radius = 4, min = 1 } = filters.slime;
    const n = countSlimeNearOrigin(seed, radius, min);
    if (n < min) return null;
    reasons.push(`${n} slime chunks close to world spawn`);
  }

  const nearby = {};
  if (filters.structures) {
    for (const req of filters.structures) {
      const cfg = STRUCTURES[req.id];
      if (!cfg) continue;
      const hits = findStructuresInRadius(seed, cfg, req.radius ?? 400, req.x ?? 0, req.z ?? 0);
      if (hits.length < (req.min ?? 1)) return null;
      nearby[req.id] = hits.slice(0, 6);
      const h = hits[0];
      reasons.push(`${cfg.name} around ${h.x}, ${h.z}`);
    }
  }

  if (filters.buriedTreasure) {
    const r = filters.buriedTreasure.radiusChunks ?? 8;
    let found = null;
    outer: for (let z = -r; z <= r; z++) {
      for (let x = -r; x <= r; x++) {
        if (isBuriedTreasureChunk(seed, x, z)) {
          found = { x: x * 16 + 9, z: z * 16 + 9 };
          break outer;
        }
      }
    }
    if (!found) return null;
    nearby.buried_treasure = [found];
    reasons.push(`Buried treasure at ${found.x}, ${found.z}`);
  }

  if (filters.stronghold) {
    const sh = firstStrongholdApprox(seed);
    const max = filters.stronghold.maxDistance ?? 2000;
    const d = Math.hypot(sh.x, sh.z);
    if (d > max) return null;
    nearby.stronghold = [sh];
    reasons.push(`stronghold around ${sh.x}, ${sh.z}`);
  }

  const pillars = pillarsFromPillarSeed(getPillarSeed(seed));
  if (!spawn) spawn = guessSpawnBiome(seed);
  reasons.unshift(`${spawn.icon} Likely start: ${spawn.name}`);
  return {
    seed: seed.toString(),
    pillarSeed: getPillarSeed(seed),
    pillars,
    cages: pillars.filter((p) => p.guarded),
    nearby,
    reasons,
    spawn,
  };
}

/**
 * Search world seeds.
 * randomize (default true) picks fresh 64-bit worlds each run so results don't repeat.
 */
export function searchSeeds(filters, opts = {}) {
  const maxResults = opts.maxResults ?? 20;
  const maxChecked = opts.maxChecked ?? 2_000_000;
  const onProgress = opts.onProgress;
  const shouldStop = opts.shouldStop;
  const randomize = opts.randomize !== false;
  const exclude = opts.exclude instanceof Set ? opts.exclude : new Set(opts.exclude || []);
  const results = [];
  const seen = new Set(exclude);
  let checked = 0;

  const pillarList = filters.pillarSeeds
    ? [...filters.pillarSeeds]
    : filters.pillars
      ? collectMatchingPillarSeeds(filters.pillars)
      : null;

  if (pillarList && pillarList.length === 0) {
    return { results, checked: 0, pillarMatches: 0, exhausted: true, impossible: true };
  }

  const hasWorldFilters = !!(
    filters.slime ||
    (filters.structures && filters.structures.length) ||
    filters.buriedTreasure ||
    filters.stronghold ||
    filters.spawnBiome
  );

  const take = (seed) => {
    const key = asInt64(seed).toString();
    if (seen.has(key)) return false;
    checked++;
    const hit = evaluateWorld(seed, filters);
    if (!hit) return false;
    seen.add(key);
    results.push(hit);
    return true;
  };

  const pickPillarSeed = (i) => {
    if (!randomize) return pillarList[i % pillarList.length];
    return pillarList[Number(randU32() % BigInt(pillarList.length))];
  };

  if (pillarList && !hasWorldFilters) {
    let i = 0;
    let guard = 0;
    while (results.length < maxResults && guard < maxResults * 80) {
      if (shouldStop && shouldStop()) break;
      const extra = randomize ? randU32() : BigInt(opts.startExtra ?? 0) + BigInt(Math.floor(i / pillarList.length));
      const upper = randomize ? randU16() : 0n;
      take(fullSeedFromPillar(pickPillarSeed(i), extra, upper));
      i++;
      guard++;
    }
    return { results, checked, pillarMatches: pillarList.length, exhausted: false };
  }

  if (pillarList) {
    const set = new Set(pillarList);
    filters = { ...filters, pillarSeeds: set };
    let extra = BigInt(opts.startExtra ?? 0);
    while (checked < maxChecked && results.length < maxResults) {
      if (shouldStop && shouldStop()) break;
      const e = randomize ? randU32() : extra;
      const upper = randomize ? randU16() : 0n;
      take(fullSeedFromPillar(pickPillarSeed(checked), e, upper));
      extra += 1n;
      if (onProgress && checked % 4000 === 0) onProgress(checked, results.length);
    }
    return { results, checked, pillarMatches: pillarList.length, exhausted: checked >= maxChecked };
  }

  let seed = randomize ? asInt64(randU32() | (randU32() << 32n)) : asInt64(opts.startSeed ?? 0);
  const step = BigInt(opts.step ?? 1);
  while (checked < maxChecked && results.length < maxResults) {
    if (shouldStop && shouldStop()) break;
    take(seed);
    if (onProgress && checked % 4000 === 0) onProgress(checked, results.length);
    seed = randomize ? asInt64(randU32() | (randU32() << 32n)) : asInt64(seed + step);
  }
  return { results, checked, pillarMatches: null, exhausted: checked >= maxChecked };
}

export function describePillarSpec(spec) {
  if (!spec) return [];
  const lines = [];
  if (spec.slots) {
    spec.slots.forEach((want, i) => {
      if (!want) return;
      const s = PILLAR_SLOTS[i];
      const bits = [];
      if (want.height != null) bits.push(`height ${want.height}`);
      if (want.minHeight != null) bits.push(`≥ ${want.minHeight}`);
      if (want.maxHeight != null) bits.push(`≤ ${want.maxHeight}`);
      if (want.guarded === true) bits.push("caged");
      if (want.guarded === false) bits.push("no cage");
      if (bits.length) lines.push(`Pillar (${s.x}, ${s.z}): ${bits.join(", ")}`);
    });
  }
  if (spec.cageOn) lines.push(`Cages required on slots ${spec.cageOn.join(", ")}`);
  if (spec.cagesAdjacent) lines.push("Cages on neighboring pillars");
  if (spec.cagesOpposite) lines.push("Cages opposite each other");
  if (spec.tallestSlot != null) {
    const s = PILLAR_SLOTS[spec.tallestSlot];
    lines.push(`Tallest (Y=103) at (${s.x}, ${s.z})`);
  }
  if (spec.shortestSlot != null) {
    const s = PILLAR_SLOTS[spec.shortestSlot];
    lines.push(`Shortest (Y=76) at (${s.x}, ${s.z})`);
  }
  if (spec.spawnFacingUnguarded) lines.push("Spawn-facing pillar uncaged");
  return lines;
}

export const PRESETS = {
  easy_dragon: {
    name: "Easier dragon",
    blurb: "First tower has no cage. The two cages sit next to each other.",
    pillars: {
      slots: [{ guarded: false, maxHeight: 91 }, null, null, null, null, null, null, null, null, null],
      cageOff: [0],
      cagesAdjacent: true,
    },
  },
  cages_far: {
    name: "Cages far away",
    blurb: "Both cages are on the far side of the island.",
    pillars: { cageOn: [4, 5] },
  },
  cages_near: {
    name: "Cages up front",
    blurb: "Both cages are on the towers closest to where you enter the End.",
    pillars: { cageOn: [0, 9] },
  },
  tall_front: {
    name: "Giant front tower",
    blurb: "The tallest tower is the one you see first.",
    pillars: { tallestSlot: 0 },
  },
  short_front: {
    name: "Tiny first tower",
    blurb: "The closest tower is short and has no cage.",
    pillars: { shortestSlot: 0, cageOff: [0] },
  },
  opposite_cages: {
    name: "Cages across the ring",
    blurb: "The two cages sit on opposite sides.",
    pillars: { cagesOpposite: true },
  },
  village_slime: {
    name: "Village + slimes",
    blurb: "A village and slime chunks near spawn, plus an easy first End tower.",
    pillars: { cageOff: [0] },
    slime: { radius: 6, min: 4 },
    structures: [{ id: "village", radius: 250 }],
  },
  nether_spawn: {
    name: "Good Nether start",
    blurb: "A fortress, a bastion, and a ruined portal close to 0, 0.",
    structures: [
      { id: "fortress", radius: 200 },
      { id: "bastion", radius: 280 },
      { id: "ruined_portal", radius: 200 },
    ],
  },
  late_game: {
    name: "Late-game nearby",
    blurb: "An ancient city, trial chambers, and a closer stronghold.",
    structures: [
      { id: "ancient_city", radius: 400 },
      { id: "trial_chambers", radius: 350 },
    ],
    stronghold: { maxDistance: 1600 },
  },
};
