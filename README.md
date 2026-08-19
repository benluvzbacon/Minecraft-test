# Endseed — Minecraft Java seed finder

A working **Java Edition** seed inspector and finder. You can ask for concrete End-island details (pillar heights, which crystals have iron cages) plus overworld / nether / outer-end structure attempts, and it searches real world seeds.

## What is exact

End spikes since 1.9 are a shuffle of ten fixed towers:

| size index | height | radius (code) | cage? |
| --- | --- | --- | --- |
| 0 | 76 | 2 | no |
| 1 | 79 | 2 | **yes** |
| 2 | 82 | 2 | **yes** |
| 3–5 | 85 / 88 / 91 | 3 | no |
| 6–8 | 94 / 97 / 100 | 4 | no |
| 9 | 103 | 5 | no |

Minecraft does:

```java
Random random = new Random(worldSeed);
long pillarSeed = random.nextLong() & 65535L;
List<Integer> sizes = Arrays.asList(0,1,2,3,4,5,6,7,8,9);
Collections.shuffle(sizes, new Random(pillarSeed));
```

Tower *positions* never change. Only which height/cage lands on which of the ten coordinates changes. This repo reimplements `java.util.Random` (48-bit LCG) and that shuffle, so pillar results match the game.

Also exact (lower 48 bits of the seed):

- slime chunks
- structure **generation attempts** for Java 1.18–1.21 salts/spacings (villages, temples, monuments, mansions, ancient cities, trial chambers, ruined portals, shipwrecks, outposts, nether fortress / bastion split, end cities, …)
- buried treasure chunk rolls
- mineshaft rolls
- first-stronghold *ring estimate* (±112 blocks until biome snap)

## What is not simulated

Biome climate / terrain. A “village at 96, -48” is the same candidate Chunkbase would show; it only becomes a real village if that spot is a village biome. End pillars do **not** depend on biomes.

## Run it

```bash
npm test
python3 -m http.server 8080 --bind 0.0.0.0
# open the site, or:
node cli.js inspect 12345
node cli.js find --no-cage 0 --tallest 0 --slime 3 --limit 5
```

Type a number or any text seed (Java `String.hashCode`) into the inspector. The finder can lock individual towers to a height or cage, require neighboring/opposite cages, and mix in slime / structure filters.
