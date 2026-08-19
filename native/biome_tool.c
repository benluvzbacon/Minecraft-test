#include "generator.h"
#include "biomes.h"
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

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "usage: biome_tool at <seed> <x> <z> [mc]\n");
        fprintf(stderr, "       biome_tool search <count> <max> [mc] <x> <z> <biome> ...\n");
        return 2;
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
