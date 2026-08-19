#!/usr/bin/env node
/**
 * Usage:
 *   node cli.js inspect 12345
 *   node cli.js find --tallest 0 --no-cage 0 --village 280 --limit 5
 */
import { parseSeed, formatSeed } from "./js/java-random.js";
import { inspectSeed, PILLAR_SLOTS } from "./js/worldgen.js";
import { searchSeeds } from "./js/finder.js";

function arg(name, fallback = null) {
  const i = process.argv.indexOf(name);
  if (i === -1) return fallback;
  return process.argv[i + 1] ?? true;
}

const cmd = process.argv[2] || "help";

if (cmd === "inspect") {
  const seed = parseSeed(process.argv[3] ?? "0");
  const data = inspectSeed(seed, { radius: Number(arg("--radius", 800)), slimeChunks: 8 });
  console.log("Seed", data.seed);
  console.log("Pillar seed", data.pillarSeed);
  console.log("Structure seed", data.structureSeed);
  console.log("Shadow seed", data.shadowSeed);
  console.log("\nEnd pillars");
  for (const p of data.pillars) {
    console.log(
      `  (${String(p.x).padStart(3)}, ${String(p.z).padStart(3)})  Y=${p.height}  r=${p.radius}  ${p.guarded ? "CAGE" : "open"}`
    );
  }
  console.log("\nCages", data.cages.map((p) => `(${p.x},${p.z})`).join(", "));
  console.log("First stronghold ~", data.stronghold.x, data.stronghold.z);
  console.log(`Slime chunks in ±8: ${data.slime.length}`);
  console.log("\nStructure attempts");
  for (const hits of Object.values(data.structures)) {
    if (!hits.length) continue;
    console.log(" ", hits[0].name);
    for (const h of hits.slice(0, 5)) console.log(`    ${h.x}, ${h.z}`);
  }
  process.exit(0);
}

if (cmd === "find") {
  const spec = { slots: Array.from({ length: 10 }, () => null) };
  let used = false;
  const tallest = arg("--tallest");
  const shortest = arg("--shortest");
  const noCage = arg("--no-cage");
  const cage = arg("--cage");
  const height = arg("--height");
  if (tallest != null) {
    spec.tallestSlot = Number(tallest);
    used = true;
  }
  if (shortest != null) {
    spec.shortestSlot = Number(shortest);
    used = true;
  }
  if (noCage != null && noCage !== true) {
    spec.cageOff = String(noCage).split(",").map(Number);
    used = true;
  }
  if (cage != null && cage !== true) {
    spec.cageOn = String(cage).split(",").map(Number);
    used = true;
  }
  if (arg("--adjacent")) {
    spec.cagesAdjacent = true;
    used = true;
  }
  if (arg("--opposite")) {
    spec.cagesOpposite = true;
    used = true;
  }
  if (height) {
    // --height 0:103
    for (const part of String(height).split(",")) {
      const [slot, h] = part.split(":").map(Number);
      spec.slots[slot] = spec.slots[slot] || {};
      spec.slots[slot].height = h;
      used = true;
    }
  }
  const filters = {};
  if (used) filters.pillars = spec;
  if (arg("--slime")) {
    filters.slime = { radius: Number(arg("--slime-radius", 6)), min: Number(arg("--slime", 3)) };
  }
  const structures = [];
  for (const id of ["village", "ruined_portal", "outpost", "ancient_city", "trial_chambers", "fortress", "bastion", "end_city", "monument", "swamp_hut"]) {
    const v = arg("--" + id.replace("_", "-"));
    if (v != null) structures.push({ id, radius: Number(v === true ? 400 : v) });
  }
  if (structures.length) filters.structures = structures;
  if (arg("--treasure")) filters.buriedTreasure = { radiusChunks: 12 };
  if (arg("--stronghold")) filters.stronghold = { maxDistance: Number(arg("--stronghold", 1600)) };

  const limit = Number(arg("--limit", 8));
  const maxChecked = Number(arg("--max", 1_000_000));
  console.log("Searching…");
  const pack = searchSeeds(filters, { maxResults: limit, maxChecked, startSeed: parseSeed(arg("--start", "0")) });
  if (pack.impossible) {
    console.error("Impossible End layout.");
    process.exit(2);
  }
  console.log(`Checked ${pack.checked}. ${pack.results.length} hit(s).`);
  for (const r of pack.results) {
    console.log("\nSeed", r.seed, "  pillar", r.pillarSeed);
    console.log(
      "  cages",
      r.cages.map((c) => `(${c.x},${c.z}) Y=${c.height}`).join(", ")
    );
    for (const line of r.reasons) console.log(" ", line);
  }
  process.exit(pack.results.length ? 0 : 1);
}

console.log(`Endseed CLI
  node cli.js inspect <seed>
  node cli.js find [filters]

Pillar slots are 0..9 around the ring, starting at (42,0):
${PILLAR_SLOTS.map((s) => `  ${s.id}: (${s.x}, ${s.z})`).join("\n")}

Filters
  --tallest <slot>       Y=103 at that tower
  --shortest <slot>      Y=76 at that tower
  --cage 4,5             those slots must be caged
  --no-cage 0            those slots must be open
  --height 0:103,5:76    exact heights
  --adjacent / --opposite
  --slime [min]          slime chunks near origin
  --village 280          village attempt within N blocks
  --ruined-portal --outpost --ancient-city --trial-chambers
  --fortress --bastion --end-city --monument --swamp-hut
  --treasure --stronghold
  --limit 8 --max 1000000 --start 0
`);
void formatSeed;
