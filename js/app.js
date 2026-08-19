import { parseSeed, formatSeed } from "./java-random.js";
import {
  PILLAR_SLOTS,
  PILLAR_HEIGHTS,
  inspectSeed,
  pillarsFromPillarSeed,
} from "./worldgen.js";
import { PRESETS, collectMatchingPillarSeeds } from "./finder.js";

const $ = (id) => document.getElementById(id);

let worker = null;
let searching = false;
const seenSeeds = new Set();

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
  const spawnBiome = $("spawn-biome")?.value;
  if (spawnBiome) filters.spawnBiome = spawnBiome;
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
  el.textContent = `That End setup works. Here’s one example below.`;
  drawPillars($("finder-svg"), pillarsFromPillarSeed(matches[0]));
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

function ensureWorker() {
  if (worker) return worker;
  worker = new Worker("./js/worker.js", { type: "module" });
  worker.onmessage = (ev) => {
    const msg = ev.data;
    if (msg.type === "pillars") {
      $("search-status").textContent = "Checking worlds that match your End…";
    }
    if (msg.type === "progress") {
      const max = Number($("max-checked").value) || 1_500_000;
      const pct = Math.min(100, (msg.checked / max) * 100);
      $("bar").style.width = pct + "%";
      $("search-status").textContent = `Still looking… found ${msg.found} so far`;
    }
    if (msg.type === "done") {
      searching = false;
      $("search-btn").disabled = false;
      $("stop-btn").disabled = true;
      $("bar").style.width = "100%";
      $("search-status").textContent = msg.result.results.length
        ? `Found ${msg.result.results.length}. Copy one into Minecraft.`
        : "Finished this search.";
      showResults(msg.result);
    }
  };
  worker.onerror = (err) => {
    searching = false;
    $("search-btn").disabled = false;
    $("search-status").innerHTML = `<span class="err">${err.message}</span>`;
  };
  return worker;
}

function startSearch() {
  const filters = readFilters();
  const maxResults = Number($("max-results").value) || 12;
  const maxChecked = Number($("max-checked").value) || 1_500_000;
  const startSeed = parseSeed($("start-seed").value || "0");
  $("results").innerHTML = "";
  $("bar").style.width = "8%";
  $("search-status").textContent = "Looking for worlds…";
  searching = true;
  $("search-btn").disabled = true;
  $("stop-btn").disabled = false;
  ensureWorker().postMessage({
    type: "search",
    payload: {
      filters,
      maxResults,
      maxChecked,
      startSeed: startSeed.toString(),
      randomize: true,
      exclude: [...seenSeeds],
    },
  });
}

function stopSearch() {
  if (worker) worker.postMessage({ type: "stop" });
}

function switchTab(name) {
  document.querySelectorAll(".tabs button").forEach((b) => b.classList.toggle("active", b.dataset.tab === name));
  $("view-inspect").classList.toggle("hidden", name !== "inspect");
  $("view-find").classList.toggle("hidden", name !== "find");
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
  $("search-btn").addEventListener("click", startSearch);
  $("stop-btn").addEventListener("click", stopSearch);
  $("slot-editor").addEventListener("change", updateMatchCount);
  document.querySelectorAll("#find-toggles input").forEach((el) => el.addEventListener("change", updateMatchCount));

  $("results").addEventListener("click", (e) => {
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

  $("seed-input").value = "12345";
  inspectFromInput();
  updateMatchCount();
}

init();
