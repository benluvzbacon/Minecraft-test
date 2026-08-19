#include "generator.h"
#include "biomes.h"
#include "finders.h"
#include "util.h"

#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

static int parse_mc(const char *s) {
    if (!s || !*s) return MC_1_21;
    int v = str2mc(s);
    return v > 0 ? v : MC_1_21;
}

static uint64_t parse_seed(const char *s) {
    return (uint64_t)strtoll(s, NULL, 10);
}

static int biome_id_from_name(int mc, const char *name) {
    if (!name || !*name) return none;
    if (name[0] >= '0' && name[0] <= '9') return atoi(name);
    for (int id = 0; id <= 186; id++) {
        const char *n = biome2str(mc, id);
        if (n && strcmp(n, name) == 0) return id;
    }
    return none;
}

static int village_ok_biome(int id) {
    switch (id) {
    case plains:
    case sunflower_plains:
    case meadow:
    case desert:
    case savanna:
    case savanna_plateau:
    case taiga:
    case snowy_taiga:
    case snowy_plains:
        return 1;
    default:
        return 0;
    }
}

typedef struct {
    int x, z, biome;
} Vil;

static int collect_villages(uint64_t seed, int radius, int mc, Vil *out, int maxn) {
    Generator g;
    setupGenerator(&g, mc, 0);
    applySeed(&g, DIM_OVERWORLD, seed);
    int spacing = 34;
    int extra = (radius / 16) / spacing + 3;
    int n = 0;
    for (int rx = -extra; rx <= extra; rx++) {
        for (int rz = -extra; rz <= extra; rz++) {
            Pos p;
            if (!getStructurePos(Village, mc, seed, rx, rz, &p)) continue;
            long long dx = p.x, dz = p.z;
            if (dx * dx + dz * dz > (long long)radius * radius) continue;
            if (!isViableStructurePos(Village, &g, p.x, p.z, 0)) continue;
            int biome = getBiomeAt(&g, 1, p.x + 8, 256, p.z + 8);
            if (biome == lush_caves || biome == dripstone_caves || biome == deep_dark)
                biome = getBiomeAt(&g, 4, (p.x + 8) >> 2, 4, (p.z + 8) >> 2);
            if (!village_ok_biome(biome)) continue;
            if (n < maxn) {
                out[n].x = p.x;
                out[n].z = p.z;
                out[n].biome = biome;
                n++;
            }
        }
    }
    return n;
}

static int find_village(uint64_t seed, int radius, int mc, Pos *out, int *biome_out) {
    Vil list[48];
    int n = collect_villages(seed, radius, mc, list, 48);
    if (n <= 0) return 0;
    int best = 0;
    int best_d = list[0].x * list[0].x + list[0].z * list[0].z;
    for (int i = 1; i < n; i++) {
        int d = list[i].x * list[i].x + list[i].z * list[i].z;
        if (d < best_d) {
            best_d = d;
            best = i;
        }
    }
    if (out) {
        out->x = list[best].x;
        out->z = list[best].z;
    }
    if (biome_out) *biome_out = list[best].biome;
    return 1;
}

/* True if some village has (need-1) others within maxDist. Writes members into cluster[]. */
static int find_cluster(Vil *v, int n, int need, int maxDist, Vil *cluster) {
    if (n < need || need < 1) return 0;
    long long md2 = (long long)maxDist * maxDist;
    int best_i = -1, best_c = 0;
    for (int i = 0; i < n; i++) {
        int c = 0;
        for (int j = 0; j < n; j++) {
            long long dx = (long long)v[i].x - v[j].x;
            long long dz = (long long)v[i].z - v[j].z;
            if (dx * dx + dz * dz <= md2) c++;
        }
        if (c > best_c) {
            best_c = c;
            best_i = i;
        }
    }
    if (best_c < need) return 0;
    int k = 0;
    for (int j = 0; j < n && k < need; j++) {
        long long dx = (long long)v[best_i].x - v[j].x;
        long long dz = (long long)v[best_i].z - v[j].z;
        if (dx * dx + dz * dz <= md2) cluster[k++] = v[j];
    }
    return k;
}

static void print_vil_array(Vil *v, int n, int mc) {
    printf("[");
    for (int i = 0; i < n; i++) {
        const char *name = biome2str(mc, v[i].biome);
        if (i) printf(",");
        printf("{\"x\":%d,\"z\":%d,\"biome\":\"%s\"}", v[i].x, v[i].z, name ? name : "unknown");
    }
    printf("]");
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "usage: biome_tool at|search|villages|filter_village|cluster ...\n");
        return 2;
    }

    if (strcmp(argv[1], "villages") == 0 && argc >= 4) {
        uint64_t seed = parse_seed(argv[2]);
        int radius = atoi(argv[3]);
        int mc = parse_mc(argc >= 5 ? argv[4] : "1.21");
        if (radius < 64) radius = 64;
        if (radius > 2500) radius = 2500;
        Vil list[48];
        int n = collect_villages(seed, radius, mc, list, 48);
        printf("{\"seed\":\"%" PRId64 "\",\"villages\":", (int64_t)seed);
        print_vil_array(list, n, mc);
        printf("}\n");
        return 0;
    }

    if (strcmp(argv[1], "cluster") == 0 && argc >= 6) {
        /* cluster <want> <maxcheck> <howMany> <maxDist> [mc] */
        int want = atoi(argv[2]);
        int maxn = atoi(argv[3]);
        int howMany = atoi(argv[4]);
        int maxDist = atoi(argv[5]);
        int mc = parse_mc(argc >= 7 ? argv[6] : "1.21");
        if (want < 1) want = 1;
        if (want > 12) want = 12;
        if (howMany < 2) howMany = 2;
        if (howMany > 4) howMany = 4;
        if (maxDist < 200) maxDist = 200;
        if (maxDist > 900) maxDist = 900;
        if (maxn < 200) maxn = 200;
        if (maxn > 80000) maxn = 80000;
        int scanR = 1400;
        uint64_t seed = ((uint64_t)time(NULL) << 20) ^ (uint64_t)clock() ^ 0x9e3779b97f4a7c15ULL;
        int found = 0, checked = 0;
        printf("{\"hits\":[");
        for (int n = 0; n < maxn && found < want; n++) {
            seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
            /* keep seeds looking like full random longs */
            if (((seed >> 48) & 0xffffULL) == 0) seed |= 0xA5A5ULL << 48;
            Vil list[48];
            int nv = collect_villages(seed, scanR, mc, list, 48);
            checked++;
            Vil cl[8];
            int nc = find_cluster(list, nv, howMany, maxDist, cl);
            if (nc < howMany) continue;
            if (found) printf(",");
            printf("{\"seed\":\"%" PRId64 "\",\"count\":%d,\"villages\":", (int64_t)seed, nc);
            print_vil_array(cl, nc, mc);
            printf("}");
            found++;
        }
        printf("],\"checked\":%d,\"found\":%d,\"want\":%d}\n", checked, found, howMany);
        return found ? 0 : 1;
    }

    if (strcmp(argv[1], "filter_village") == 0 && argc >= 4) {
        int radius = atoi(argv[2]);
        int mc = parse_mc(argv[3]);
        if (radius < 64) radius = 64;
        printf("{\"hits\":[");
        int found = 0;
        for (int i = 4; i < argc; i++) {
            uint64_t seed = parse_seed(argv[i]);
            Pos p;
            int biome = none;
            if (!find_village(seed, radius, mc, &p, &biome)) continue;
            const char *name = biome2str(mc, biome);
            if (found) printf(",");
            printf("{\"seed\":\"%" PRId64 "\",\"x\":%d,\"z\":%d,\"biome\":\"%s\"}",
                   (int64_t)seed, p.x, p.z, name ? name : "unknown");
            found++;
        }
        printf("],\"found\":%d}\n", found);
        return found ? 0 : 1;
    }

    if (strcmp(argv[1], "at") == 0 && argc >= 5) {
        uint64_t seed = parse_seed(argv[2]);
        int x = atoi(argv[3]);
        int z = atoi(argv[4]);
        int mc = parse_mc(argc >= 6 ? argv[5] : "1.21");
        Generator g;
        setupGenerator(&g, mc, 0);
        applySeed(&g, DIM_OVERWORLD, seed);
        int id = getBiomeAt(&g, 1, x, 64, z);
        const char *name = biome2str(mc, id);
        printf("{\"id\":%d,\"name\":\"%s\",\"x\":%d,\"z\":%d,\"seed\":\"%" PRId64 "\"}\n",
               id, name ? name : "unknown", x, z, (int64_t)seed);
        return id == none ? 1 : 0;
    }

    if (strcmp(argv[1], "search") == 0 && argc >= 6) {
        int want = atoi(argv[2]);
        int maxn = atoi(argv[3]);
        int mc = parse_mc(argv[4]);
        if (want < 1) want = 1;
        if (want > 20) want = 20;
        if (maxn < 100) maxn = 100;
        if (maxn > 200000) maxn = 200000;

        int npts = (argc - 5) / 3;
        if (npts < 1) return 2;
        int xs[16], zs[16], ids[16];
        if (npts > 16) npts = 16;
        for (int i = 0; i < npts; i++) {
            xs[i] = atoi(argv[5 + i * 3]);
            zs[i] = atoi(argv[6 + i * 3]);
            ids[i] = biome_id_from_name(mc, argv[7 + i * 3]);
            if (ids[i] == none) {
                fprintf(stderr, "unknown biome %s\n", argv[7 + i * 3]);
                return 2;
            }
        }

        Generator g;
        setupGenerator(&g, mc, 0);
        uint64_t seed = ((uint64_t)time(NULL) << 20) ^ (uint64_t)clock() ^ 0x9e3779b97f4a7c15ULL;
        int found = 0;
        int checked = 0;
        printf("{\"hits\":[");
        for (int n = 0; n < maxn && found < want; n++) {
            seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
            applySeed(&g, DIM_OVERWORLD, seed);
            int ok = 1;
            for (int i = 0; i < npts; i++) {
                int got = getBiomeAt(&g, 4, xs[i] >> 2, 16, zs[i] >> 2);
                if (got != ids[i]) {
                    ok = 0;
                    break;
                }
            }
            checked++;
            if (!ok) continue;
            if (found) printf(",");
            printf("{\"seed\":\"%" PRId64 "\",\"points\":[", (int64_t)seed);
            for (int i = 0; i < npts; i++) {
                int got = getBiomeAt(&g, 1, xs[i], 64, zs[i]);
                const char *name = biome2str(mc, got);
                if (i) printf(",");
                printf("{\"x\":%d,\"z\":%d,\"id\":%d,\"name\":\"%s\"}",
                       xs[i], zs[i], got, name ? name : "unknown");
            }
            printf("]}");
            found++;
        }
        printf("],\"checked\":%d,\"found\":%d}\n", checked, found);
        return found ? 0 : 1;
    }

    return 2;
}
