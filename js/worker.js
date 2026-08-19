import { searchSeeds, collectMatchingPillarSeeds } from "./finder.js";

let stop = false;

self.onmessage = (ev) => {
  const { type, payload } = ev.data || {};
  if (type === "stop") {
    stop = true;
    return;
  }
  if (type !== "search") return;
  stop = false;

  const filters = payload.filters || {};
  if (filters.pillars) {
    const list = collectMatchingPillarSeeds(filters.pillars);
    self.postMessage({ type: "pillars", count: list.length });
    if (list.length === 0) {
      self.postMessage({
        type: "done",
        result: { results: [], checked: 0, pillarMatches: 0, exhausted: true, impossible: true },
      });
      return;
    }
    filters.pillarSeeds = new Set(list);
  }

  const result = searchSeeds(filters, {
    maxResults: payload.maxResults ?? 20,
    maxChecked: payload.maxChecked ?? 1_500_000,
    startSeed: payload.startSeed ?? 0,
    randomize: payload.randomize !== false,
    exclude: payload.exclude || [],
    shouldStop: () => stop,
    onProgress: (checked, found) => {
      self.postMessage({ type: "progress", checked, found });
    },
  });

  self.postMessage({ type: "done", result });
};
