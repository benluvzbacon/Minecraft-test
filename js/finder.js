import {
  PILLAR_SLOTS,
  pillarsFromPillarSeed,
  worldSeedFromPillar,
  getPillarSeed,
  isSlimeChunk,
  STRUCTURES,
  findStructuresInRadius,
  firstStrongholdApprox,
  isBuriedTreasureChunk,
} from "./worldgen.js";
import { asInt64 } from "./java-random.js";

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

export function evaluateWorld(worldSeed, filters) {
  const seed = asInt64(worldSeed);
  const reasons = [];

  if (filters.pillarSeeds instanceof Set) {
    const ps = getPillarSeed(seed);
    if (!filters.pillarSeeds.has(ps)) return null;
  } else if (filters.pillars) {
    if (!evaluatePillars(pillarsFromPillarSeed(getPillarSeed(seed)), filters.pillars)) return null;
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
  return {
    seed: seed.toString(),
    pillarSeed: getPillarSeed(seed),
    pillars,
    cages: pillars.filter((p) => p.guarded),
    nearby,
    reasons,
  };
}

/**
 * Search world seeds.
 * When pillarSeeds is provided, only seeds that produce those End layouts are visited.
 */
export function searchSeeds(filters, opts = {}) {
  const maxResults = opts.maxResults ?? 20;
  const maxChecked = opts.maxChecked ?? 2_000_000;
  const onProgress = opts.onProgress;
  const shouldStop = opts.shouldStop;
  const results = [];
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
    filters.stronghold
  );

  if (pillarList && !hasWorldFilters) {
    const extras = opts.startExtra ?? 0n;
    for (let i = 0; i < maxResults; i++) {
      const ps = pillarList[i % pillarList.length];
      const extra = extras + BigInt(Math.floor(i / pillarList.length));
      const seed = worldSeedFromPillar(ps, extra);
      const hit = evaluateWorld(seed, { ...filters, pillarSeeds: undefined, pillars: undefined });
      if (hit) results.push(hit);
      checked++;
    }
    return { results, checked, pillarMatches: pillarList.length, exhausted: false };
  }

  if (pillarList) {
    const startExtra = BigInt(opts.startExtra ?? 0);
    const set = new Set(pillarList);
    let extra = startExtra;
    while (checked < maxChecked && results.length < maxResults) {
      if (shouldStop && shouldStop()) break;
      for (let i = 0; i < pillarList.length && checked < maxChecked && results.length < maxResults; i++) {
        const seed = worldSeedFromPillar(pillarList[i], extra);
        checked++;
        const hit = evaluateWorld(seed, { ...filters, pillarSeeds: set });
        if (hit) results.push(hit);
        if (onProgress && checked % 25000 === 0) onProgress(checked, results.length);
      }
      extra += 1n;
    }
    return { results, checked, pillarMatches: pillarList.length, exhausted: checked >= maxChecked };
  }

  let seed = asInt64(opts.startSeed ?? 0);
  const step = BigInt(opts.step ?? 1);
  while (checked < maxChecked && results.length < maxResults) {
    if (shouldStop && shouldStop()) break;
    const hit = evaluateWorld(seed, filters);
    checked++;
    if (hit) results.push(hit);
    if (onProgress && checked % 25000 === 0) onProgress(checked, results.length);
    seed = asInt64(seed + step);
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
    name: "Easy dragon fight",
    blurb: "No cage on the spawn-facing tower, cages clustered on the far side.",
    pillars: {
      slots: [{ guarded: false, maxHeight: 91 }, null, null, null, null, null, null, null, null, null],
      cageOff: [0],
      cagesAdjacent: true,
    },
  },
  cages_far: {
    name: "Cages on the far rim",
    blurb: "Both iron cages sit on the −X half, away from the (100, 0) End spawn.",
    pillars: { cageOn: [4, 5] },
  },
  cages_near: {
    name: "Cages next to spawn",
    blurb: "Caged crystals on the +X towers — harder fight, useful if you want a specific layout.",
    pillars: { cageOn: [0, 9] },
  },
  tall_front: {
    name: "Monument tower at spawn",
    blurb: "The Y=103 pillar sits at (42, 0), the closest tower to End spawn.",
    pillars: { tallestSlot: 0 },
  },
  short_front: {
    name: "Short uncaged front",
    blurb: "Lowest tower facing spawn, no cage — fastest first crystal.",
    pillars: { shortestSlot: 0, cageOff: [0] },
  },
  opposite_cages: {
    name: "Opposite cages",
    blurb: "The two cages sit across the ring from each other.",
    pillars: { cagesOpposite: true },
  },
  village_slime: {
    name: "Village + slime + easy End",
    blurb: "Village attempt near origin, several slime chunks, uncaged front pillar.",
    pillars: { cageOff: [0] },
    slime: { radius: 6, min: 4 },
    structures: [{ id: "village", radius: 250 }],
  },
  nether_spawn: {
    name: "Nether hub seed",
    blurb: "Fortress and bastion generation attempts close to Nether 0,0 plus a ruined portal near origin.",
    structures: [
      { id: "fortress", radius: 200 },
      { id: "bastion", radius: 280 },
      { id: "ruined_portal", radius: 200 },
    ],
  },
  late_game: {
    name: "Ancient city + trial + stronghold",
    blurb: "All three late-game structure attempts packed near the origin.",
    structures: [
      { id: "ancient_city", radius: 400 },
      { id: "trial_chambers", radius: 350 },
    ],
    stronghold: { maxDistance: 1600 },
  },
};
