import { parseSeed, formatSeed } from "./java-random.js";
import {
  PILLAR_SLOTS,
  PILLAR_HEIGHTS,
  inspectSeed,
  pillarsFromPillarSeed,
} from "./worldgen.js";
import { PRESETS, collectMatchingPillarSeeds, searchSeeds, evaluateWorld } from "./finder.js";

const $ = (id) => document.getElementById(id);

let searching = false;
let seenSeeds = new Set();
try {
  seenSeeds = new Set(JSON.parse(sessionStorage.getItem("seenSeeds") || "[]"));
} catch {
  seenSeeds = new Set();
}

function svgEl(name, attrs) {
  const el = document.createElementNS("http://www.w3.org/2000/svg", name);
  for (const [k, v] of Object.entries(attrs || {})) el.setAttribute(k, String(v));
  return el;
}

function drawPillars(svg, pillars, highlight = {}) {
  while (svg.firstChild) svg.removeChild(svg.firstChild);
  const w = 520, h = 520, cx = 260, cy = 260, ring = 168;
  svg.setAttribute("viewBox", `0 0 ${w} ${h}`);

  svg.appendChild(svgEl("circle", { cx, cy, r: 210, fill: "#0b0714", stroke: "rgba(192,132,252,0.18)", "stroke-width": 2 }));
  svg.appendChild(svgEl("circle", { cx, cy, r: ring, fill: "none", stroke: "rgba(103,232,249,0.18)", "stroke-dasharray": "4 6" }));
  svg.appendChild(svgEl("circle", { cx, cy, r: 22, fill: "#1e1030", stroke: "#c084fc", "stroke-width": 2 }));
  svg.appendChild(svgEl("text", { x: cx, y: cy + 4, fill: "#e9d5ff", "font-size": 11, "text-anchor": "middle" })).textContent = "dragon";

  svg.appendChild(svgEl("text", { x: cx + 188, y: cy + 4, fill: "#fbbf24", "font-size": 11, "text-anchor": "start" })).textContent = "you enter →";

  for (const p of pillars) {
    const ang = ((p.angle - 90) * Math.PI) / 180;
    const x = cx + Math.cos(ang) * ring;
    const y = cy + Math.sin(ang) * ring;
    const tall = (p.height - 70) / 36;
    const bw = 10 + p.radius * 3;
    const bh = 28 + tall * 54;
    const color = p.guarded ? "#fbbf24" : `hsl(${260 + p.sizeIndex * 8}, 70%, ${42 + tall * 28}%)`;
    const g = svgEl("g", {});
    const tower = svgEl("rect", {
      x: x - bw / 2,
      y: y - bh,
      width: bw,
      height: bh,
      rx: 3,
      fill: color,
      stroke: highlight.slot === p.slot ? "#fff" : "rgba(0,0,0,0.35)",
      "stroke-width": highlight.slot === p.slot ? 2 : 1,
    });
    g.appendChild(tower);
    if (p.guarded) {
      g.appendChild(svgEl("rect", {
        x: x - bw / 2 - 4,
        y: y - bh - 14,
        width: bw + 8,
        height: 16,
        fill: "none",
        stroke: "#e5e7eb",
        "stroke-width": 1.4,
      }));
      g.appendChild(svgEl("line", { x1: x - bw / 2 - 4, y1: y - bh - 6, x2: x + bw / 2 + 4, y2: y - bh - 6, stroke: "#e5e7eb", "stroke-width": 1 }));
    } else {
      g.appendChild(svgEl("circle", { cx: x, cy: y - bh - 5, r: 4, fill: "#f9a8d4" }));
    }
    const label = svgEl("text", {
      x,
      y: y + 16,
      fill: "#d6c7f0",
      "font-size": 10,
      "text-anchor": "middle",
      "font-family": "IBM Plex Mono, monospace",
    });
    label.textContent = `${p.x},${p.z}`;
    g.appendChild(label);
    const hy = svgEl("text", {
      x,
      y: y - bh - (p.guarded ? 20 : 14),
      fill: p.guarded ? "#fde68a" : "#f5f3ff",
      "font-size": 11,
      "text-anchor": "middle",
      "font-weight": 600,
    });
    hy.textContent = p.guarded ? `${p.height} cage` : String(p.height);
    g.appendChild(hy);
    svg.appendChild(g);
  }
}

function fillPillarTable(tbody, pillars) {
  tbody.innerHTML = pillars
    .map(
      (p) => `<tr class="${p.guarded ? "caged" : ""}">
        <td class="mono">${p.x}, ${p.z}</td>
        <td>${p.height} blocks</td>
        <td>${p.guarded ? "yes — iron bars" : "no"}</td>
      </tr>`
    )
    .join("");
}

function renderInspect(data) {
  $("meta-seed").textContent = data.seed;
  $("meta-pillar").textContent = String(data.pillarSeed);
  $("meta-struct").textContent = data.structureSeed;
  $("meta-shadow").textContent = data.shadowSeed;
  $("meta-sh").textContent = `${data.stronghold.x}, ${data.stronghold.z}`;
  if ($("meta-slime")) $("meta-slime").textContent = String(data.slime.length);
  $("meta-cages").textContent = "2 of 10";
  if (data.spawn) {
    $("meta-spawn").textContent = `${data.spawn.icon} ${data.spawn.name}`;
  }

  drawPillars($("pillar-svg"), data.pillars);
  fillPillarTable($("pillar-table"), data.pillars);

  const cages = data.cages.map((p) => `the tower at ${p.x}, ${p.z} (height ${p.height})`).join(" and ");
  $("cage-summary").textContent = cages ? `Cages are on ${cages}.` : "—";

  const structRoot = $("struct-results");
  const blocks = [];
  for (const [id, hits] of Object.entries(data.structures)) {
    if (!hits.length) continue;
    const list = hits
      .slice(0, 4)
      .map((h) => `${h.x}, ${h.z}`)
      .join(" · ");
    blocks.push(`<li><span class="struct-k">${hits[0].icon} ${hits[0].name}</span><div class="mono">${list}</div></li>`);
  }
  if (data.treasures.length) {
    blocks.push(
      `<li><span class="struct-k">💎 Buried treasure</span><div class="mono">${data.treasures
        .slice(0, 4)
        .map((t) => `${t.x}, ${t.z}`)
        .join(" · ")}</div></li>`
    );
  }
  structRoot.innerHTML = `<ul>${blocks.join("") || "<li>No structure attempts in radius.</li>"}</ul>`;
}

function inspectFromInput() {
  const seed = parseSeed($("seed-input").value);
  $("seed-input").value = formatSeed(seed);
  const radius = Number($("inspect-radius").value) || 800;
  const data = inspectSeed(seed, { radius, slimeChunks: 8 });
  renderInspect(data);
  fetch("/api/villages", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ seed: data.seed, radius: 600, mc: "1.21" }),
  })
    .then((r) => r.json())
    .then((v) => {
      const list = v.villages || [];
      const box = $("struct-results");
      if (!box) return;
      const extra = list.length
        ? `<li><span class="struct-k">🏠 Village (confirmed)</span><div class="mono">${list
            .map((h) => `${h.x}, ${h.z} ${h.biome || ""}`)
            .join(" · ")}</div></li>`
        : `<li><span class="struct-k">🏠 Village (confirmed)</span><div>none within 600 blocks</div></li>`;
      const ul = box.querySelector("ul");
      if (ul) {
        [...ul.querySelectorAll("li")].forEach((li) => {
          if (li.textContent.includes("Village")) li.remove();
        });
        ul.insertAdjacentHTML("afterbegin", extra);
      }
    })
    .catch(() => {});
}

function buildSlotEditor() {
  const root = $("slot-editor");
  root.innerHTML = "";
  PILLAR_SLOTS.forEach((slot) => {
    const card = document.createElement("div");
    card.className = "slot-card";
    card.innerHTML = `
      <strong>(${slot.x}, ${slot.z})</strong>
      <select data-slot="${slot.id}" data-k="height">
        <option value="">any height</option>
        ${PILLAR_HEIGHTS.map((h) => `<option value="${h}">${h} blocks tall</option>`).join("")}
      </select>
      <select data-slot="${slot.id}" data-k="cage">
        <option value="">cage: either</option>
        <option value="yes">needs a cage</option>
        <option value="no">no cage</option>
      </select>`;
    root.appendChild(card);
  });
}

function readPillarSpec() {
  const slots = Array.from({ length: 10 }, () => null);
  let used = false;
  document.querySelectorAll("#slot-editor select").forEach((el) => {
    const i = Number(el.dataset.slot);
    const k = el.dataset.k;
    const v = el.value;
    if (!v) return;
    used = true;
    slots[i] = slots[i] || {};
    if (k === "height") slots[i].height = Number(v);
    if (k === "cage") slots[i].guarded = v === "yes";
  });
  const spec = {};
  if (used) spec.slots = slots;
  if ($("flt-cages-adj").checked) spec.cagesAdjacent = true;
  if ($("flt-cages-opp").checked) spec.cagesOpposite = true;
  if ($("flt-front-open").checked) spec.cageOff = [0];
  if ($("flt-tall-front").checked) spec.tallestSlot = 0;
  if ($("flt-short-front").checked) spec.shortestSlot = 0;
  return Object.keys(spec).length ? spec : null;
}

function readFilters() {
  const filters = {};
  const pillars = readPillarSpec();
  if (pillars) filters.pillars = pillars;
  if ($("flt-slime").checked) {
    filters.slime = {
      radius: Number($("slime-radius").value) || 6,
      min: Number($("slime-min").value) || 3,
    };
  }
  const structures = [];
  document.querySelectorAll("[data-struct]").forEach((el) => {
    if (el.checked) {
      structures.push({
        id: el.dataset.struct,
        radius: Number(el.dataset.radius || 400),
      });
    }
  });
  if (structures.length) filters.structures = structures;
  if ($("flt-treasure").checked) filters.buriedTreasure = { radiusChunks: 12 };
  if ($("flt-stronghold").checked) filters.stronghold = { maxDistance: Number($("sh-dist").value) || 1600 };
  return filters;
}

function applyPreset(id) {
  document.querySelectorAll(".preset").forEach((b) => b.classList.toggle("active", b.dataset.id === id));
  const p = PRESETS[id];
  if (!p) return;
  document.querySelectorAll("#slot-editor select").forEach((el) => {
    el.value = "";
  });
  $("flt-cages-adj").checked = !!p.pillars?.cagesAdjacent;
  $("flt-cages-opp").checked = !!p.pillars?.cagesOpposite;
  $("flt-front-open").checked = !!(p.pillars?.cageOff && p.pillars.cageOff.includes(0));
  $("flt-tall-front").checked = p.pillars?.tallestSlot === 0;
  $("flt-short-front").checked = p.pillars?.shortestSlot === 0;
  if (p.pillars?.slots) {
    p.pillars.slots.forEach((want, i) => {
      if (!want) return;
      const h = document.querySelector(`#slot-editor select[data-slot="${i}"][data-k="height"]`);
      const c = document.querySelector(`#slot-editor select[data-slot="${i}"][data-k="cage"]`);
      if (h && want.height != null) h.value = String(want.height);
      if (c && want.guarded === true) c.value = "yes";
      if (c && want.guarded === false) c.value = "no";
    });
  }
  if (p.pillars?.cageOn) {
    p.pillars.cageOn.forEach((i) => {
      const c = document.querySelector(`#slot-editor select[data-slot="${i}"][data-k="cage"]`);
      if (c) c.value = "yes";
    });
  }
  $("flt-slime").checked = !!p.slime;
  if (p.slime) {
    $("slime-radius").value = p.slime.radius;
    $("slime-min").value = p.slime.min;
  }
  document.querySelectorAll("[data-struct]").forEach((el) => {
    el.checked = !!(p.structures && p.structures.some((s) => s.id === el.dataset.struct));
  });
  $("flt-treasure").checked = !!p.buriedTreasure;
  $("flt-stronghold").checked = !!p.stronghold;
  seenSeeds.clear();
  if ($("search-btn")) $("search-btn").textContent = "Find seeds";
  updateMatchCount();
}

function updateMatchCount() {
  const spec = readPillarSpec();
  const el = $("pillar-match-count");
  if (!spec) {
    el.textContent = "No End rules picked yet — any island is fine.";
    drawPillars($("finder-svg"), pillarsFromPillarSeed(0));
    return;
  }
  const matches = collectMatchingPillarSeeds(spec);
  if (!matches.length) {
    el.innerHTML = `<span class="err">Minecraft can’t build that End.</span> Only two cages exist, and they always sit on the 79 and 82 towers. Each height is used once.`;
    return;
  }
  el.textContent = `That End setup works. Preview below — found seeds will be different worlds with this layout.`;
  const pick = matches[Math.floor(Math.random() * matches.length)];
  drawPillars($("finder-svg"), pillarsFromPillarSeed(pick));
}

function showResults(pack) {
  const root = $("results");
  if (pack.impossible) {
    root.innerHTML = `<p class="err">That mix of tower heights and cages can’t happen in Java Edition. Turn one option off and try again.</p>`;
    return;
  }
  if (!pack.results.length) {
    root.innerHTML = `<p>Didn’t find one this time. Uncheck a couple of extras (village + fortress + slimes all at once is picky) and try again.</p>`;
    return;
  }
  root.innerHTML = pack.results
    .map((r) => {
      const cage = r.cages.map((c) => `${c.x}, ${c.z}`).join(" and ");
      const start = r.spawn ? `${r.spawn.icon} ${r.spawn.name}` : "";
      return `<article class="result">
        <div class="result-head">
          <b class="mono">${r.seed}</b>
          <span>
            <button class="copy" data-copy="${r.seed}">Copy seed</button>
            <button class="copy" data-inspect="${r.seed}">See the map</button>
          </span>
        </div>
        <div class="note">${start ? `${start} · ` : ""}Cages at ${cage}</div>
        <ol>${(r.reasons || []).map((x) => `<li>${x}</li>`).join("")}</ol>
      </article>`;
    })
    .join("");
}

function rememberSeeds(list) {
  for (const seed of list) seenSeeds.add(String(seed));
  const keep = [...seenSeeds].slice(-300);
  seenSeeds.clear();
  keep.forEach((s) => seenSeeds.add(s));
  try {
    sessionStorage.setItem("seenSeeds", JSON.stringify(keep));
  } catch {
    /* ignore quota */
  }
}

const CLUSTER_WORDS = {
  1: "1 — just one nearby",
  2: "2 — a pair close together",
  3: "3 — a rare triple",
  4: "4 — a very rare quad",
};

function clusterCount() {
  const n = Number($("village-cluster")?.value || 1);
  return Math.max(1, Math.min(4, n));
}

function clusterMaxDist(n) {
  if (n >= 4) return 600;
  if (n >= 3) return 470;
  return 360;
}

function clusterMaxChecks(n) {
  if (n >= 4) return 35000;
  if (n >= 3) return 16000;
  return 8000;
}

function updateClusterLabel() {
  const n = clusterCount();
  if ($("cluster-label")) $("cluster-label").textContent = CLUSTER_WORDS[n] || String(n);
  if ($("cluster-hint")) {
    $("cluster-hint").textContent =
      n === 1
        ? "1 is a normal nearby village. Slide up for rare clumps."
        : n === 2
          ? "Two confirmed villages within about 360 blocks of each other."
          : n === 3
            ? "Three villages packed together. This can take a little longer."
            : "Four villages in one clump — uncommon. Search may take up to a minute.";
  }
  if (n >= 2) {
    const box = document.querySelector("[data-struct='village']");
    if (box) box.checked = true;
  }
}

async function searchVillageCluster(howMany, count) {
  const res = await fetch("/api/villages/cluster", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      count,
      howMany,
      maxDist: clusterMaxDist(howMany),
      max: clusterMaxChecks(howMany),
      mc: "1.21",
    }),
  });
  const data = await res.json();
  if (data.error) throw new Error(data.error);
  return data;
}

async function confirmVillages(seeds, radius) {
  const res = await fetch("/api/villages/filter", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ seeds, radius, mc: "1.21" }),
  });
  const data = await res.json();
  if (data.error) throw new Error(data.error);
  const map = new Map();
  for (const h of data.hits || []) map.set(String(h.seed), h);
  return map;
}

function startSearch() {
  searching = false;
  const filters = readFilters();
  const wantVillage = (filters.structures || []).some((s) => s.id === "village");
  const villageRadius = (filters.structures || []).find((s) => s.id === "village")?.radius || 450;
  const maxResults = Math.max(1, Math.min(40, Number($("max-results")?.value) || 8));
  const typed = String($("start-seed")?.value || "").trim();
  if ($("results")) $("results").innerHTML = "<p class='note'>Looking for new worlds…</p>";
  if ($("bar")) $("bar").style.width = "20%";
  if ($("search-status")) {
    $("search-status").textContent = wantVillage
      ? "Finding village attempts, then checking real biomes…"
      : "Looking for new worlds…";
  }
  if ($("search-btn")) $("search-btn").disabled = true;
  searching = true;

  window.setTimeout(async () => {
    try {
      const pack = searchSeeds(filters, {
        maxResults: wantVillage ? Math.max(maxResults * 6, 24) : maxResults,
        maxChecked: 120000,
        randomize: true,
        exclude: seenSeeds,
        startSeed: typed ? parseSeed(typed) : undefined,
      });
      if (typed) {
        const yours = evaluateWorld(parseSeed(typed), filters);
        if (yours && !pack.results.some((r) => r.seed === yours.seed)) {
          pack.results.unshift(yours);
        }
      }
      if (wantVillage && pack.results.length) {
        if ($("search-status")) $("search-status").textContent = "Confirming villages with cubiomes…";
        const ok = await confirmVillages(
          pack.results.map((r) => r.seed),
          villageRadius
        );
        pack.results = pack.results.filter((r) => ok.has(r.seed)).map((r) => {
          const v = ok.get(r.seed);
          const line = `Village at ${v.x}, ${v.z} (${(v.biome || "").replaceAll("_", " ")})`;
          return {
            ...r,
            reasons: [line, ...(r.reasons || []).filter((x) => !String(x).startsWith("Village around"))],
          };
        });
        pack.results = pack.results.slice(0, maxResults);
      }
      rememberSeeds(pack.results.map((r) => r.seed));
      if ($("bar")) $("bar").style.width = "100%";
      if ($("search-btn")) {
        $("search-btn").disabled = false;
        $("search-btn").textContent = "Find different seeds";
      }
      searching = false;
      if ($("search-status")) {
        $("search-status").textContent = pack.results.length
          ? wantVillage
            ? `Here are ${pack.results.length} seeds with a biome-checked village.`
            : `Here are ${pack.results.length} new seeds. Click again for another batch.`
          : "Nothing this time. Uncheck something or try again.";
      }
      showResults(pack);
    } catch (err) {
      searching = false;
      if ($("search-btn")) $("search-btn").disabled = false;
      if ($("search-status")) $("search-status").innerHTML = `<span class="err">${err.message || err}</span>`;
    }
  }, 30);
}

function switchTab(name) {
  document.querySelectorAll(".tabs button").forEach((b) => b.classList.toggle("active", b.dataset.tab === name));
  $("view-inspect")?.classList.toggle("hidden", name !== "inspect");
  $("view-find")?.classList.toggle("hidden", name !== "find");
  $("view-biomes")?.classList.toggle("hidden", name !== "biomes");
}

const BIOME_CHOICES = [
  "plains", "sunflower_plains", "meadow", "cherry_grove", "forest", "flower_forest",
  "birch_forest", "dark_forest", "taiga", "snowy_taiga", "snowy_plains", "ice_spikes",
  "desert", "savanna", "badlands", "jungle", "sparse_jungle", "bamboo_jungle",
  "swamp", "mangrove_swamp", "mushroom_fields", "beach", "ocean", "warm_ocean",
  "lukewarm_ocean", "cold_ocean", "frozen_ocean", "river", "jagged_peaks",
  "stony_peaks", "grove", "pale_garden",
];

function biomeSelect(selected) {
  return BIOME_CHOICES.map((b) => `<option value="${b}" ${b === selected ? "selected" : ""}>${b.replaceAll("_", " ")}</option>`).join("");
}

function addBiomeRow(x = 0, z = 0, biome = "plains") {
  const root = $("biome-rows");
  if (!root) return;
  const row = document.createElement("div");
  row.className = "row biome-row";
  row.innerHTML = `
    <label class="field">X<input type="number" data-k="x" value="${x}" /></label>
    <label class="field">Z<input type="number" data-k="z" value="${z}" /></label>
    <label class="field grow">Biome<select data-k="biome">${biomeSelect(biome)}</select></label>
    <button type="button" class="btn secondary biome-del">Remove</button>`;
  row.querySelector(".biome-del").addEventListener("click", () => row.remove());
  root.appendChild(row);
}

function readBiomePoints() {
  return [...document.querySelectorAll("#biome-rows .biome-row")].map((row) => ({
    x: Number(row.querySelector('[data-k="x"]').value) || 0,
    z: Number(row.querySelector('[data-k="z"]').value) || 0,
    biome: row.querySelector('[data-k="biome"]').value,
  }));
}

async function lookupBiomeAt() {
  const status = $("biome-status");
  const seed = $("biome-seed").value.trim();
  const points = readBiomePoints();
  if (!seed) {
    status.textContent = "Type a seed first, or use Find biome seeds below.";
    return;
  }
  status.textContent = "Checking cubiomes…";
  const bits = [];
  for (const p of points) {
    const res = await fetch("/api/biomes/at", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ seed, x: p.x, z: p.z, mc: "1.21" }),
    });
    const data = await res.json();
    if (data.error) throw new Error(data.error);
    bits.push(`At ${data.x}, ${data.z} this seed is <b>${(data.name || "").replaceAll("_", " ")}</b>`);
  }
  status.innerHTML = bits.join("<br>");
}

async function searchBiomes() {
  const status = $("biome-status");
  const out = $("biome-results");
  const points = readBiomePoints();
  if (!points.length) {
    status.textContent = "Add at least one X / Z / biome row.";
    return;
  }
  status.textContent = "Searching real Java 1.21 biomes (cubiomes)… this can take a few seconds.";
  out.innerHTML = "";
  $("biome-find").disabled = true;
  try {
    const res = await fetch("/api/biomes/search", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ points, count: Number($("biome-count").value) || 5, max: 12000, mc: "1.21" }),
    });
    const data = await res.json();
    if (data.error) throw new Error(data.error);
    if (!data.hits || !data.hits.length) {
      status.textContent = `Checked ${data.checked || 0} seeds and found none. Try fewer points or a more common biome.`;
      return;
    }
    status.textContent = `Found ${data.found} after ${data.checked} checks. These match Java 1.21.`;
    out.innerHTML = data.hits
      .map((h) => {
        const pts = (h.points || [])
          .map((p) => `${p.x}, ${p.z} = ${(p.name || "").replaceAll("_", " ")}`)
          .join(" · ");
        return `<article class="result">
          <div class="result-head">
            <b class="mono">${h.seed}</b>
            <span>
              <button class="copy" data-copy="${h.seed}">Copy seed</button>
              <button class="copy" data-inspect="${h.seed}">See the End</button>
            </span>
          </div>
          <div class="note">${pts}</div>
        </article>`;
      })
      .join("");
  } catch (err) {
    status.innerHTML = `<span class="err">${err.message || err}</span>`;
  } finally {
    $("biome-find").disabled = false;
  }
}

function init() {
  buildSlotEditor();
  const presetRoot = $("presets");
  Object.entries(PRESETS).forEach(([id, p]) => {
    const b = document.createElement("button");
    b.className = "preset";
    b.dataset.id = id;
    b.innerHTML = `<b>${p.name}</b><small>${p.blurb}</small>`;
    b.addEventListener("click", () => applyPreset(id));
    presetRoot.appendChild(b);
  });

  $("inspect-btn").addEventListener("click", inspectFromInput);
  $("seed-input").addEventListener("keydown", (e) => {
    if (e.key === "Enter") inspectFromInput();
  });
  $("random-seed").addEventListener("click", () => {
    const n = (BigInt(Math.floor(Math.random() * 0xffffffff)) << 32n) ^ BigInt(Math.floor(Math.random() * 0xffffffff));
    $("seed-input").value = formatSeed(n);
    inspectFromInput();
  });
  document.querySelectorAll(".tabs button").forEach((b) => {
    b.addEventListener("click", () => switchTab(b.dataset.tab));
  });
  $("village-cluster")?.addEventListener("input", updateClusterLabel);
  updateClusterLabel();
  $("search-btn")?.addEventListener("click", startSearch);
  $("stop-btn")?.addEventListener("click", startSearch);
  $("slot-editor")?.addEventListener("change", updateMatchCount);
  document.querySelectorAll("#find-toggles input").forEach((el) => el.addEventListener("change", updateMatchCount));

  $("biome-add")?.addEventListener("click", () => addBiomeRow(0, 0, "plains"));
  $("biome-find")?.addEventListener("click", () => searchBiomes().catch((e) => {
    $("biome-status").innerHTML = `<span class="err">${e.message}</span>`;
  }));
  $("biome-lookup")?.addEventListener("click", () => lookupBiomeAt().catch((e) => {
    $("biome-status").innerHTML = `<span class="err">${e.message}</span>`;
  }));
  if ($("biome-rows") && !$("biome-rows").children.length) addBiomeRow(0, 0, "plains");

  document.body.addEventListener("click", (e) => {
    const t = e.target;
    if (!(t instanceof HTMLElement)) return;
    if (t.dataset.copy) {
      navigator.clipboard?.writeText(t.dataset.copy);
      t.textContent = "Copied";
      setTimeout(() => (t.textContent = "Copy seed"), 900);
    }
    if (t.dataset.inspect) {
      $("seed-input").value = t.dataset.inspect;
      switchTab("inspect");
      inspectFromInput();
    }
  });

  const fresh =
    (BigInt(Math.floor(Math.random() * 0xffffffff)) << 32n) ^
    BigInt(Math.floor(Math.random() * 0xffffffff));
  $("seed-input").value = formatSeed(fresh);
  inspectFromInput();
  updateMatchCount();
}

try {
  init();
} catch (err) {
  document.body.insertAdjacentHTML(
    "afterbegin",
    `<p class="err" style="padding:12px">Page failed to start: ${err.message}. Hard-refresh (Ctrl+Shift+R).</p>`
  );
}
