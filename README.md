# Minecraft Seed Finder

A simple Java Edition tool:

1. **Look up a seed** — type a number (or a word) and see the End towers: how tall each one is, and which two crystals have iron cages.
2. **Find me a seed** — pick what you want (easy dragon, cages far away, a village near spawn…) and copy a seed into Minecraft.

## Run it

Open `index.html` in a browser (or serve the folder), or:

```bash
python3 -m http.server 8080 --bind 0.0.0.0
node cli.js inspect 12345
node cli.js find --no-cage 0 --limit 5
npm test
```

The End map is exact. Nearby villages / fortresses are “likely spots” — they still need the right biome to actually generate.
