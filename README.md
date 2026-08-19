# Minecraft Seed Finder (Java Edition)

A browser tool that looks up a world seed and **finds new seeds** from what you ask for:

- How tall each **End pillar** is, and which crystals have **iron cages**
- Nearby stuff (slime chunks, ruined portals, fortresses, …)
- **Villages that actually generate** (biome-checked with cubiomes)
- **Biomes at exact X, Z** (Java 1.21, same family of math as Chunkbase)

It is **Java Edition only**. Bedrock uses different worldgen.

Repo: [github.com/benluvzbacon/Minecraft-test](https://github.com/benluvzbacon/Minecraft-test)

---

## What you need

| Thing | Why |
| --- | --- |
| **Python 3** | Runs the website (`server.py`) |
| **Node.js 18+** | Optional: tests and the command-line tool |
| **gcc** | Only if you rebuild the cubiomes helper |

On Windows, use [Python](https://www.python.org/downloads/), [Node](https://nodejs.org/), and Git Bash or WSL. The included `native/biome_tool` binary is built for Linux.

---

## Download

### Option A — Git (best)

```bash
git clone https://github.com/benluvzbacon/Minecraft-test.git
cd Minecraft-test
```

### Option B — ZIP

1. Open [github.com/benluvzbacon/Minecraft-test](https://github.com/benluvzbacon/Minecraft-test)
2. Click the green **Code** button → **Download ZIP**
3. Unzip it
4. Open a terminal in that folder

---

## Run the website

From the project folder:

```bash
python3 server.py
```

Then open **http://127.0.0.1:8080** in your browser.

`server.py` does two jobs:

1. Serves the page
2. Talks to **cubiomes** for real biomes and confirmed villages

If you only open `index.html` as a file, End lookup still works, but **Biomes at X, Z** and **village (biome-checked)** will not.

Hard-refresh (**Ctrl+Shift+R** / **Cmd+Shift+R**) if the page looks stuck on an old version.

Paste any seed you get into Minecraft: **Create New World → More → Seed**.

---

## How to use it

There are three tabs.

### 1. Look up a seed

Use this when you already have a number (or a word seed like `Glitter`).

1. Type the seed
2. Click **Show me** (or **Surprise me** for a random one)

You get:

- A map of the **10 End towers** (yellow = iron cage, pink = open crystal)
- Height of each tower and whether it has a cage
- A **confirmed village** nearby (or “none”), checked with cubiomes
- Other nearby structure *attempts* (portals, monuments, fortresses, …)
- First stronghold *estimate*
- A rough “you’ll probably start in …” climate guess (not GPS-accurate)

**End towers are exact** for Java 1.9–1.21. Positions never change; only which height/cage sits on which tower changes with the seed.

### 2. Find me a seed

Use this to roll **new large random seeds** (16+ digits), not tiny list seeds like `12345`.

1. Optionally click a **quick pick** (easier dragon, cages far away, village + slimes, …)
2. Or set each tower: height `76–103` and cage yes/no  
   - Every world uses each height **once**  
   - Cages **only** exist on the **Y=79** and **Y=82** crystals
3. Tick extras if you want them (slime, village, fortress, …)
4. Click **Find seeds**
5. **Copy seed** into Minecraft, or **See the map**

Click **Find different seeds** / **Find another batch** for another unrelated set.

**Village (biome-checked):** the finder first locates a village *spot*, then cubiomes checks the biome (plains, desert, savanna, taiga, snowy plains, meadow). Only seeds that pass are shown. That should match in-game most of the time; weird terrain can still eat a few.

Other structures on this tab are still **generation attempts**. A witch hut only appears if that chunk is swamp, etc.

Leave **Start from this seed** blank for random worlds. If you paste a seed there, that world is checked first, then more random ones are added.

### 3. Biomes at X, Z

Real **Java 1.21** biomes via cubiomes.

**Look up**

1. Paste a seed
2. Set X, Z, and a biome row (or several)
3. **What’s at these coords?**

**Find seeds**

1. Add rows like: `X=0 Z=0 biome=desert` and `X=800 Z=200 biome=forest`
2. **Find biome seeds**
3. Copy a result into Minecraft

Rarer biomes (mushroom fields, ice spikes) take longer. Fewer rows = faster.

---

## End pillar cheat sheet

Ten towers in a ring around the exit portal. Centers (always):

| # | X, Z | Notes |
| --- | --- | --- |
| 0 | 42, 0 | Closest to where you enter the End |
| 1 | 33, 24 | |
| 2 | 12, 39 | |
| 3 | -13, 39 | |
| 4 | -34, 24 | |
| 5 | -42, -1 | Far side |
| 6 | -34, -25 | |
| 7 | -13, -40 | |
| 8 | 12, -40 | |
| 9 | 33, -25 | |

Heights (shuffled by the seed): **76, 79, 82, 85, 88, 91, 94, 97, 100, 103**  
Cages: **only 79 and 82**.

---

## Command line (optional)

Needs Node.js.

```bash
# End towers + nearby structure attempts for one seed
node cli.js inspect 12345

# Find seeds: no cage on the first tower, tallest there, 5 results
node cli.js find --no-cage 0 --tallest 0 --limit 5

node cli.js find --help
```

Useful flags:

```
--tallest 0          Y=103 on tower 0 (42, 0)
--shortest 0         Y=76 on that tower
--cage 4,5           cages on those tower numbers
--no-cage 0
--height 0:103,5:76
--adjacent           cages next to each other
--opposite
--slime 3            at least 3 slime chunks near origin
--village 450        village attempt within 450 blocks (CLI does not cubiomes-check)
--ruined-portal --outpost --ancient-city --trial-chambers
--fortress --bastion --end-city --monument --swamp-hut
--treasure --stronghold
--limit 8 --max 1000000 --start 0
```

For **confirmed villages** and **biome-at-coordinate**, use the website (`python3 server.py`), not the CLI.

---

## Tests

```bash
npm test
# or: node test/test.js
```

Checks Java `Random`, End pillar shuffle, slime chunks, structure math, and that Find returns large unique seeds.

---

## Rebuild the cubiomes helper (optional)

Linux/macOS with `gcc`:

```bash
git clone https://github.com/Cubitect/cubiomes.git /tmp/cubiomes
make -C /tmp/cubiomes libcubiomes
chmod +x native/build.sh
CUBIOMES=/tmp/cubiomes ./native/build.sh
```

Then run `python3 server.py` again.

The helper (`native/biome_tool`) is what makes **Biomes at X, Z** and **village (biome-checked)** match Java 1.21.

---

## How accurate is it?

| Feature | Accuracy |
| --- | --- |
| End pillar heights & cages | Exact (Java 1.9+) |
| Slime chunks | Exact |
| Biome at X, Z | Exact Java 1.21 (cubiomes) |
| Village (biome-checked) | High — cubiomes structure + biome. Odd terrain can still skip a village. |
| Other structures (portal, fortress, monument, …) | **Attempt** locations only. Need the right biome/terrain in game. |
| First stronghold | Ring estimate, about ±112 blocks until biome snap |
| “You’ll probably start in …” | Rough climate guess, not the real spawn search |

---

## Project layout

```
index.html          Website
css/                Styles
js/                 End finder, inspect UI
server.py           Web server + cubiomes API
native/biome_tool   Cubiomes helper (biomes + villages)
cli.js              Command-line inspect / find
test/test.js        Unit tests
```

API (when `server.py` is running):

- `POST /api/biomes/at` — biome at a seed + X, Z
- `POST /api/biomes/search` — find seeds with biomes at points
- `POST /api/villages` — confirmed village near a seed
- `POST /api/villages/filter` — keep seeds that have a real village

---

## Credits

End math follows vanilla `java.util.Random` and the 1.9+ pillar shuffle.  
Biome and village checks use [cubiomes](https://github.com/Cubitect/cubiomes) by Cubitect.

Not affiliated with Mojang or Microsoft.
