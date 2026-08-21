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

static int is_cave_id(int id) {
    return id == dripstone_caves || id == lush_caves;
}

/* Java WorldgenRandom.setDecorationSeed(worldSeed, blockX, blockZ), then 12
 * portal frames: each nextFloat() > 0.9f has an eye (vanilla PortalRoom). */
static int portal_eye_count(uint64_t worldSeed, int x, int z) {
    uint64_t rnd;
    setSeed(&rnd, worldSeed);
    uint64_t a = nextLong(&rnd) | 1ULL;
    uint64_t b = nextLong(&rnd) | 1ULL;
    uint64_t k = (uint64_t)(int64_t)x * a + (uint64_t)(int64_t)z * b ^ worldSeed;
    setSeed(&rnd, k);
    int e = 0;
    int i;
    for (i = 0; i < 12; i++) {
        if (nextFloat(&rnd) > 0.9f)
            e++;
    }
    return e;
}

static Pos first_stronghold(Generator *g, uint64_t seed, int mc) {
    StrongholdIter sh;
    initFirstStronghold(&sh, mc, seed);
    nextStronghold(&sh, g);
    return sh.pos;
}

typedef struct {
    int x, y, z;
    int biome_id;
    int cave_neighbors;
    int impossible;
    char biome[48];
} SpawnInfo;

/* Spawn is "impossible-style" when cubiomes world-spawn sits in a dripstone
 * (or lush) cave at player height, the four sides are also cave, and dripstone
 * continues down where lava aquifers generate. Calculated, not a seed list. */
static SpawnInfo analyze_spawn(Generator *g, int mc) {
    SpawnInfo s;
    memset(&s, 0, sizeof(s));
    Pos p = getSpawn(g);
    s.x = p.x;
    s.z = p.z;
    s.y = 80;
    int c80 = getBiomeAt(g, 1, p.x, 80, p.z);
    int c64 = getBiomeAt(g, 1, p.x, 64, p.z);
    int c8 = getBiomeAt(g, 1, p.x, 8, p.z);
    int c0 = getBiomeAt(g, 1, p.x, 0, p.z);
    int dirs[4][2] = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    int n80 = 0, n8 = 0, i;
    for (i = 0; i < 4; i++) {
        int a = getBiomeAt(g, 1, p.x + dirs[i][0], 80, p.z + dirs[i][1]);
        int b = getBiomeAt(g, 1, p.x + dirs[i][0], 8, p.z + dirs[i][1]);
        if (is_cave_id(a))
            n80++;
        if (b == dripstone_caves)
            n8++;
    }
    s.cave_neighbors = n80;
    int lava_below = (c8 == dripstone_caves) || (c0 == dripstone_caves);
    s.impossible = is_cave_id(c80) && is_cave_id(c64) && lava_below && n80 >= 3 && n8 >= 2;
    if (s.impossible)
        s.y = 72;
    if (s.impossible) {
        s.biome_id = c80;
    } else {
        static const int ys[] = {96, 80, 120, 160, 256, 72, 64};
        s.biome_id = none;
        for (i = 0; i < 7; i++) {
            int id = getBiomeAt(g, 1, p.x, ys[i], p.z);
            if (id != none && id != lush_caves && id != dripstone_caves && id != deep_dark) {
                s.biome_id = id;
                break;
            }
        }
        if (s.biome_id == none)
            s.biome_id = c80;
    }
    {
        const char *nm = biome2str(mc, s.biome_id);
        snprintf(s.biome, sizeof(s.biome), "%s", nm ? nm : "unknown");
    }
    return s;
}

static uint64_t scramble_seed(void) {
    uint64_t seed = ((uint64_t)time(NULL) << 20) ^ (uint64_t)clock() ^ 0x9e3779b97f4a7c15ULL;
    seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
    if (((seed >> 48) & 0xffffULL) == 0)
        seed |= 0xA5A5ULL << 48;
    return seed;
}

static uint64_t next_big_seed(uint64_t seed) {
    seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
    if (((seed >> 48) & 0xffffULL) == 0)
        seed |= 0xC0DEULL << 48;
    return seed;
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
        printf("{\"x\":%d,\"y\":80,\"z\":%d,\"biome\":\"%s\",\"tp\":\"/tp @s %d 80 %d\"}",
               v[i].x, v[i].z, name ? name : "unknown", v[i].x, v[i].z);
    }
    printf("]");
}

typedef struct {
    int x, y, z, biome;
    char tp[64];
} Hit;

static int stype_from_name(const char *s) {
    if (!s) return -1;
    if (!strcmp(s, "village")) return Village;
    if (!strcmp(s, "outpost")) return Outpost;
    if (!strcmp(s, "monument")) return Monument;
    if (!strcmp(s, "mansion")) return Mansion;
    if (!strcmp(s, "swamp_hut")) return Swamp_Hut;
    if (!strcmp(s, "desert_pyramid")) return Desert_Pyramid;
    if (!strcmp(s, "jungle_pyramid") || !strcmp(s, "jungle_temple")) return Jungle_Pyramid;
    if (!strcmp(s, "igloo")) return Igloo;
    if (!strcmp(s, "ruined_portal")) return Ruined_Portal;
    if (!strcmp(s, "ancient_city")) return Ancient_City;
    if (!strcmp(s, "trail_ruins")) return Trail_Ruins;
    if (!strcmp(s, "trial_chambers")) return Trial_Chambers;
    if (!strcmp(s, "fortress")) return Fortress;
    if (!strcmp(s, "bastion")) return Bastion;
    if (!strcmp(s, "end_city")) return End_City;
    if (!strcmp(s, "shipwreck")) return Shipwreck;
    if (!strcmp(s, "ocean_ruin")) return Ocean_Ruin;
    return -1;
}

static int struct_dim(int st) {
    if (st == Fortress || st == Bastion || st == Ruined_Portal_N) return DIM_NETHER;
    if (st == End_City) return DIM_END;
    return DIM_OVERWORLD;
}

static int struct_y(int st) {
    if (st == Ancient_City) return -37;
    if (st == Trial_Chambers) return -20;
    if (st == Monument) return 48;
    if (st == Fortress || st == Bastion) return 64;
    if (st == End_City) return 80;
    return 80;
}

static int nearest_struct(uint64_t seed, int st, int radius, int mc, Hit *out) {
    StructureConfig sc;
    if (!getStructureConfig(st, mc, &sc)) return 0;
    int dim = struct_dim(st);
    Generator *gp = (Generator *)calloc(1, sizeof(Generator));
    if (!gp) return 0;
    setupGenerator(gp, mc, 0);
    applySeed(gp, dim, seed);
    int spacing = sc.regionSize > 0 ? sc.regionSize : 32;
    int extra = (radius / 16) / spacing + 3;
    int found = 0;
    Pos best;
    int best_d = 0;
    for (int rx = -extra; rx <= extra; rx++) {
        for (int rz = -extra; rz <= extra; rz++) {
            Pos p;
            if (!getStructurePos(st, mc, seed, rx, rz, &p)) continue;
            long long dx = p.x, dz = p.z;
            long long d2 = dx * dx + dz * dz;
            if (d2 > (long long)radius * radius) continue;
            if (st != End_City) {
                if (!isViableStructurePos(st, gp, p.x, p.z, 0)) continue;
            }
            if (!found || d2 < best_d) {
                found = 1;
                best = p;
                best_d = (int)d2;
            }
        }
    }
    if (!found) {
        free(gp);
        return 0;
    }
    int y = struct_y(st);
    int biome = none;
    if (dim == DIM_OVERWORLD)
        biome = getBiomeAt(gp, 1, best.x + 8, y > 0 ? y : 80, best.z + 8);
    out->x = best.x + 8;
    out->z = best.z + 8;
    out->y = y;
    out->biome = biome;
    snprintf(out->tp, sizeof(out->tp), "/tp @s %d %d %d", out->x, out->y, out->z);
    free(gp);
    return 1;
}

static void print_hit(int64_t seed, Hit *h) {
    const char *bname = biome2str(MC_1_21, h->biome);
    printf("{\"seed\":\"%" PRId64 "\",\"x\":%d,\"y\":%d,\"z\":%d,\"biome\":\"%s\",\"tp\":\"%s\"}",
           seed, h->x, h->y, h->z, bname ? bname : "", h->tp);
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

    if (strcmp(argv[1], "spawn") == 0 && argc >= 3) {
        uint64_t seed = parse_seed(argv[2]);
        int mc = parse_mc(argc >= 4 ? argv[3] : "1.21");
        Generator *gp = (Generator *)calloc(1, sizeof(Generator));
        if (!gp)
            return 1;
        setupGenerator(gp, mc, 0);
        applySeed(gp, DIM_OVERWORLD, seed);
        SpawnInfo s = analyze_spawn(gp, mc);
        Pos sh = first_stronghold(gp, seed, mc);
        int eyes = portal_eye_count(seed, sh.x, sh.z);
        printf("{\"seed\":\"%" PRId64 "\",\"x\":%d,\"y\":%d,\"z\":%d,\"biome\":\"%s\","
               "\"tp\":\"/tp @s %d %d %d\",\"impossible\":%s,\"caveNeighbors\":%d,"
               "\"stronghold\":{\"x\":%d,\"z\":%d,\"eyes\":%d,\"tp\":\"/tp @s %d 20 %d\"}}\n",
               (int64_t)seed, s.x, s.y, s.z, s.biome, s.x, s.y, s.z,
               s.impossible ? "true" : "false", s.cave_neighbors,
               sh.x, sh.z, eyes, sh.x, sh.z);
        free(gp);
        return 0;
    }

    if (strcmp(argv[1], "portal") == 0 && argc >= 3) {
        uint64_t seed = parse_seed(argv[2]);
        int mc = parse_mc(argc >= 4 ? argv[3] : "1.21");
        Generator *gp = (Generator *)calloc(1, sizeof(Generator));
        if (!gp)
            return 1;
        setupGenerator(gp, mc, 0);
        applySeed(gp, DIM_OVERWORLD, seed);
        Pos sh = first_stronghold(gp, seed, mc);
        int eyes = portal_eye_count(seed, sh.x, sh.z);
        printf("{\"seed\":\"%" PRId64 "\",\"x\":%d,\"y\":20,\"z\":%d,\"eyes\":%d,\"frames\":12,"
               "\"tp\":\"/tp @s %d 20 %d\"}\n",
               (int64_t)seed, sh.x, sh.z, eyes, sh.x, sh.z);
        free(gp);
        return 0;
    }

    if (strcmp(argv[1], "hunt") == 0 && argc >= 6) {
        /* hunt <want> <maxn> <impossible 0|1> <eyes -1 or 0-12> [mc] */
        int want = atoi(argv[2]);
        int maxn = atoi(argv[3]);
        int want_imp = atoi(argv[4]);
        int want_eyes = atoi(argv[5]);
        int mc = parse_mc(argc >= 7 ? argv[6] : "1.21");
        if (want < 1)
            want = 1;
        if (want > 12)
            want = 12;
        if (maxn < 200)
            maxn = 200;
        if (maxn > 80000)
            maxn = 80000;
        if (want_eyes > 12)
            want_eyes = 12;
        Generator *gp = (Generator *)calloc(1, sizeof(Generator));
        if (!gp)
            return 1;
        setupGenerator(gp, mc, 0);
        uint64_t seed = scramble_seed();
        int found = 0, checked = 0;
        printf("{\"hits\":[");
        int n;
        for (n = 0; n < maxn && found < want; n++) {
            seed = next_big_seed(seed);
            applySeed(gp, DIM_OVERWORLD, seed);
            checked++;
            SpawnInfo s;
            memset(&s, 0, sizeof(s));
            if (want_imp) {
                s = analyze_spawn(gp, mc);
                if (!s.impossible)
                    continue;
            } else {
                s.y = 80;
                snprintf(s.biome, sizeof(s.biome), "%s", "unknown");
            }
            Pos sh = first_stronghold(gp, seed, mc);
            int eyes = portal_eye_count(seed, sh.x, sh.z);
            if (want_eyes >= 0 && eyes != want_eyes)
                continue;
            if (!want_imp) {
                Pos p = getSpawn(gp);
                s.x = p.x;
                s.z = p.z;
                s.y = 80;
                int id = getBiomeAt(gp, 1, p.x, 80, p.z);
                if (id == lush_caves || id == dripstone_caves || id == deep_dark)
                    id = getBiomeAt(gp, 1, p.x, 96, p.z);
                const char *nm = biome2str(mc, id);
                snprintf(s.biome, sizeof(s.biome), "%s", nm ? nm : "unknown");
            }
            if (found)
                printf(",");
            printf("{\"seed\":\"%" PRId64 "\",\"x\":%d,\"y\":%d,\"z\":%d,\"biome\":\"%s\","
                   "\"impossible\":%s,\"caveNeighbors\":%d,\"tp\":\"/tp @s %d %d %d\","
                   "\"stronghold\":{\"x\":%d,\"z\":%d,\"eyes\":%d,\"tp\":\"/tp @s %d 20 %d\"}}",
                   (int64_t)seed, s.x, s.y, s.z, s.biome,
                   s.impossible ? "true" : "false", s.cave_neighbors,
                   s.x, s.y, s.z, sh.x, sh.z, eyes, sh.x, sh.z);
            found++;
        }
        printf("],\"checked\":%d,\"found\":%d}\n", checked, found);
        free(gp);
        return found ? 0 : 1;
    }

    if (strcmp(argv[1], "find_struct") == 0 && argc >= 6) {
        /* find_struct <type> <want> <maxcheck> <radius> [mc] */
        int st = stype_from_name(argv[2]);
        if (st < 0) {
            fprintf(stderr, "unknown structure %s\n", argv[2]);
            return 2;
        }
        int want = atoi(argv[3]);
        int maxn = atoi(argv[4]);
        int radius = atoi(argv[5]);
        int mc = parse_mc(argc >= 7 ? argv[6] : "1.21");
        if (want < 1) want = 1;
        if (want > 12) want = 12;
        if (radius < 80) radius = 80;
        if (maxn < 200) maxn = 200;
        if (maxn > 60000) maxn = 60000;
        uint64_t seed = ((uint64_t)time(NULL) << 20) ^ (uint64_t)clock() ^ 0xC2B2AE3D27D4EB4FULL;
        int found = 0, checked = 0;
        printf("{\"hits\":[");
        for (int n = 0; n < maxn && found < want; n++) {
            seed = seed * 6364136223846793005ULL + 1;
            if (((seed >> 48) & 0xffffULL) == 0) seed |= 0x9E37ULL << 48;
            Hit h;
            checked++;
            if (!nearest_struct(seed, st, radius, mc, &h)) continue;
            if (found) printf(",");
            print_hit((int64_t)seed, &h);
            found++;
        }
        printf("],\"checked\":%d,\"found\":%d}\n", checked, found);
        return found ? 0 : 1;
    }

    if (strcmp(argv[1], "filter_struct") == 0 && argc >= 5) {
        /* filter_struct <type> <radius> <mc> <seed>... */
        int st = stype_from_name(argv[2]);
        if (st < 0) {
            fprintf(stderr, "unknown structure %s\n", argv[2]);
            return 2;
        }
        int radius = atoi(argv[3]);
        int mc = parse_mc(argv[4]);
        if (radius < 80) radius = 80;
        printf("{\"hits\":[");
        int found = 0;
        for (int i = 5; i < argc; i++) {
            uint64_t seed = parse_seed(argv[i]);
            Hit h;
            if (!nearest_struct(seed, st, radius, mc, &h)) continue;
            if (found) printf(",");
            print_hit((int64_t)seed, &h);
            found++;
        }
        printf("],\"found\":%d}\n", found);
        return found ? 0 : 1;
    }

    if (strcmp(argv[1], "around") == 0 && argc >= 4) {
        uint64_t seed = parse_seed(argv[2]);
        int radius = atoi(argv[3]);
        int mc = parse_mc(argc >= 5 ? argv[4] : "1.21");
        if (radius < 80) radius = 80;
        if (radius > 2500) radius = 2500;
        static const char *names[] = {
            "village", "outpost", "swamp_hut", "desert_pyramid", "igloo",
            "ruined_portal", "ancient_city", "trail_ruins", "trial_chambers",
            "fortress", "bastion", "shipwreck", "ocean_ruin", NULL
        };
        printf("{\"seed\":\"%" PRId64 "\",\"structures\":{", (int64_t)seed);
        int first = 1;
        for (int i = 0; names[i]; i++) {
            int st = stype_from_name(names[i]);
            Hit h;
            if (!nearest_struct(seed, st, radius, mc, &h)) continue;
            if (!first) printf(",");
            first = 0;
            {
                const char *bn = biome2str(mc, h.biome);
                printf("\"%s\":{\"x\":%d,\"y\":%d,\"z\":%d,\"biome\":\"%s\",\"tp\":\"%s\"}",
                       names[i], h.x, h.y, h.z, bn ? bn : "", h.tp);
            }
        }
        printf("}}\n");
        return 0;
    }

    return 2;
}
