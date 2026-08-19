import { JavaRandom, parseSeed, javaStringHash } from "../js/java-random.js";
import {
  getPillarSeed,
  pillarsFromPillarSeed,
  getEndPillars,
  worldSeedFromPillar,
  isSlimeChunk,
  getStructurePos,
  STRUCTURES,
  structureAttemptValid,
  isBuriedTreasureChunk,
} from "../js/worldgen.js";
import { collectMatchingPillarSeeds, searchSeeds } from "../js/finder.js";

let passed = 0;
let failed = 0;

function assert(cond, msg) {
  if (cond) {
    passed++;
    console.log("  ok  ", msg);
  } else {
    failed++;
    console.error("  FAIL", msg);
  }
}

function assertEq(a, b, msg) {
  const ok = Object.is(a, b) || (typeof a === "bigint" && a === b);
  assert(ok, `${msg} (got ${a}, expected ${b})`);
}

console.log("Java Random");
{
  const r = new JavaRandom(0n);
  assertEq(r.nextInt(), -1155484576, "Random(0).nextInt()");
}
{
  const r = new JavaRandom(0n);
  assertEq(r.nextLong(), -4962768465676381896n, "Random(0).nextLong()");
}
{
  const r = new JavaRandom(0n);
  assert(Math.abs(r.nextDouble() - 0.730967787376657) < 1e-15, "Random(0).nextDouble()");
}
{
  const r = new JavaRandom(1n);
  assertEq(r.nextInt(), -1155869325, "Random(1).nextInt()");
}
{
  const r = new JavaRandom(0n);
  const seq = [];
  for (let n = 10; n >= 2; n--) seq.push(r.nextInt(n));
  assert(seq.every((v, i) => v >= 0 && v < 10 - i), "nextInt(n) in range for shuffle bounds");
}

console.log("Seed parsing");
assertEq(parseSeed("0"), 0n, "numeric 0");
assertEq(parseSeed("-12345"), -12345n, "negative numeric");
assertEq(parseSeed("Glitter"), BigInt(javaStringHash("Glitter")), "text uses hashCode");
assertEq(javaStringHash("abc"), 96354, "hashCode('abc')");
assertEq(javaStringHash("Glitter"), 1780740401, "hashCode('Glitter')");
assertEq(parseSeed("800"), 800n, "numeric not hashed");

console.log("End pillars");
{
  const ps0 = getPillarSeed(0n);
  assert(ps0 >= 0 && ps0 <= 65535, "pillar seed in 0..65535");
  const pillars = pillarsFromPillarSeed(ps0);
  assertEq(pillars.length, 10, "10 pillars");
  const heights = pillars.map((p) => p.height).sort((a, b) => a - b);
  assertEq(heights.join(","), "76,79,82,85,88,91,94,97,100,103", "unique official heights");
  const cages = pillars.filter((p) => p.guarded);
  assertEq(cages.length, 2, "exactly two cages");
  assert(
    cages.every((c) => c.height === 79 || c.height === 82),
    "cages only on Y=79 and Y=82"
  );
  assertEq(pillars[0].x, 42, "slot 0 at x=42");
  assertEq(pillars[5].x, -42, "slot 5 at x=-42");
  assertEq(pillars[5].z, -1, "slot 5 at z=-1 (fp floor)");
}

{
  const seed = 12345n;
  const { pillarSeed, pillars } = getEndPillars(seed);
  const rebuilt = worldSeedFromPillar(pillarSeed, 0n);
  assertEq(getPillarSeed(rebuilt), pillarSeed, "constructed seed reproduces pillar seed");
  const again = pillarsFromPillarSeed(getPillarSeed(rebuilt));
  assertEq(
    again.map((p) => `${p.height}:${p.guarded}`).join("|"),
    pillars.map((p) => `${p.height}:${p.guarded}`).join("|"),
    "constructed seed same pillar layout"
  );
}

{
  // invert seed 0: find extra that rebuilds a seed with same pillar seed
  const target = getPillarSeed(0n);
  let found = false;
  for (let extra = 0n; extra < 64n; extra++) {
    if (getPillarSeed(worldSeedFromPillar(target, extra)) === target) {
      found = true;
      break;
    }
  }
  assert(found, "worldSeedFromPillar produces matching pillar seeds");
}

console.log("Slime chunks");
{
  // Wiki algorithm: seed 0, chunk (0,0) is well-known
  const r = new JavaRandom(
    0n +
      BigInt.asIntN(32, 0n) +
      BigInt.asIntN(32, 0n) +
      0n * 0x4307a7n +
      0n ^
      0x3ad8025fn
  );
  // just sanity: function returns boolean and some chunks in a 32x32 are slimes (~10%)
  let n = 0;
  for (let z = 0; z < 32; z++) for (let x = 0; x < 32; x++) if (isSlimeChunk(0n, x, z)) n++;
  assert(n > 50 && n < 160, `seed 0 has a plausible slime density (${n}/1024)`);
  const a = isSlimeChunk(0n, 0, 0);
  const b = isSlimeChunk(0n, 0, 0);
  assertEq(a, b, "slime chunk is deterministic");
}

console.log("Structures");
{
  const pos = getStructurePos(STRUCTURES.village, 0n, 0, 0);
  assert(pos.x % 16 === 0 && pos.z % 16 === 0, "village attempt is chunk-aligned");
  assert(pos.chunkX >= 0 && pos.chunkX < 34, "village region 0,0 chunk in range");
  const city = getStructurePos(STRUCTURES.end_city, 0n, 0, 0);
  const far = structureAttemptValid(STRUCTURES.end_city, 0n, {
    ...getStructurePos(STRUCTURES.end_city, 0n, 8, 0),
  });
  assert(typeof far === "boolean", "end city distance gate returns boolean");
  assert(typeof isBuriedTreasureChunk(0n, 0, 0) === "boolean", "buried treasure boolean");
  void city;
}

console.log("Finder");
{
  const spec = { tallestSlot: 0 };
  const matches = collectMatchingPillarSeeds(spec);
  assert(matches.length > 0, "some pillar seeds put Y=103 at slot 0");
  assert(
    matches.every((ps) => pillarsFromPillarSeed(ps)[0].height === 103),
    "all collected seeds satisfy tallestSlot=0"
  );
  const none = collectMatchingPillarSeeds({
    slots: [{ height: 103 }, { height: 103 }, null, null, null, null, null, null, null, null],
  });
  assertEq(none.length, 0, "impossible duplicate heights rejected");

  const search = searchSeeds({ pillars: { tallestSlot: 0 } }, { maxResults: 5, maxChecked: 100 });
  assertEq(search.results.length, 5, "returns 5 seeds for tall-front preset");
  assert(
    search.results.every((r) => r.pillars[0].height === 103),
    "every found seed has tallest at spawn-facing pillar"
  );
}

{
  const search = searchSeeds(
    { pillars: { cageOff: [0] }, slime: { radius: 8, min: 2 } },
    { maxResults: 3, maxChecked: 200000 }
  );
  assert(search.results.length >= 1, "finds a seed with uncaged front + slimes");
  for (const r of search.results) {
    assert(!r.pillars[0].guarded, "front uncaged");
  }
}

console.log("\n" + passed + " passed,", failed + " failed");
if (failed) process.exit(1);
