#!/usr/bin/env python3
"""Launch an isolated Fabric server and a real rendered client; exercise vanilla network paths.

Linux CI: LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null xvfb-run -a python3 scripts/smoke_multiplayer.py --accept-eula
The temporary, offline-mode server is confined to build/smoke-server. Never use its properties in production.
"""
import argparse
from pathlib import Path
import queue
import subprocess
import sys
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "build/smoke-reports"
SERVER = ROOT / "build/smoke-server"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--accept-eula", action="store_true", help="Accept https://aka.ms/MinecraftEULA for the isolated test server")
    args = parser.parse_args()
    if not args.accept_eula:
        parser.error("This test starts Minecraft. Read its EULA, then pass --accept-eula to proceed.")
    REPORT.mkdir(parents=True, exist_ok=True)
    SERVER.mkdir(parents=True, exist_ok=True)
    (SERVER / "eula.txt").write_text("eula=true\n")
    (SERVER / "server.properties").write_text("\n".join([
        "online-mode=false", "server-ip=0.0.0.0", "server-port=25565", "max-players=2", "view-distance=5", "simulation-distance=5",
        "spawn-protection=0", "difficulty=normal", "level-seed=4197231", "level-name=smoke-world", "sync-chunk-writes=false",
        "enable-status=false", "enforce-secure-profile=false", "motd=Riftborn isolated CI test", ""]))
    client_dir = ROOT / "run/smoke-client"
    client_dir.mkdir(parents=True, exist_ok=True)
    (client_dir / "options.txt").write_text("onboardAccessibility:false\nskipMultiplayerWarning:true\ntutorialStep:none\npauseOnLostFocus:false\nnarrator:0\nrenderDistance:5\nmaxFps:60\n")
    messages = queue.Queue()
    processes = {}
    logs = {}
    gradle = [str(ROOT / "gradlew"), "--no-daemon", "-Dorg.gradle.jvmargs=-Xmx512m"]

    def start(name, task):
        logs[name] = open(REPORT / (name + ".log"), "w", encoding="utf-8")
        process = subprocess.Popen(gradle + [task], cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, bufsize=1)
        processes[name] = process

        def consume():
            for line in process.stdout:
                logs[name].write(line)
                logs[name].flush()
                messages.put((name, line.rstrip()))
            messages.put((name, None))
        threading.Thread(target=consume, daemon=True).start()

    def command(command):
        processes["server"].stdin.write(command + "\n")
        processes["server"].stdin.flush()

    def fixture():
        for cmd in [
            "gamerule doMobSpawning false", "gamerule doWeatherCycle false", "gamerule doDaylightCycle false",
            "gamerule keepInventory true", "gamerule spawnChunkRadius 0", "gamerule spawnRadius 0", "time set day",
            "forceload add -16 -16 16 16",
            "execute in riftborn:the_rift run forceload add -16 -16 48 64",
            "fill -6 99 -6 6 99 6 minecraft:stone", "fill -6 100 -6 6 106 6 minecraft:air",
            "setblock 0 100 0 riftborn:rift_anchor[open=false]", "setworldspawn 0 100 2",
            "execute in riftborn:the_rift run place template riftborn:guardian_shrine 0 140 0",
            "execute in riftborn:the_rift run fill -4 140 34 36 140 48 riftborn:rift_stone",
            'execute in riftborn:the_rift run summon riftborn:rift_stalker 10.5 141 25.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}',
            'execute in riftborn:the_rift run summon riftborn:void_brute 23.5 141 25.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}',
            'execute in riftborn:the_rift run summon riftborn:rift_wisp 13.5 144 25.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}',
            'execute in riftborn:the_rift run summon riftborn:rift_guardian 16.5 142 18.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}',
            "execute in riftborn:the_rift run locate structure riftborn:guardian_shrine",
            "execute in minecraft:overworld run locate structure riftborn:overworld_ruin",
        ]:
            command(cmd)

    success = False
    joined = False
    server_ready = False
    expected_stop = False
    located = set()
    markers = set()
    required_markers = {
        "RIFTBORN_TRAVEL_ENTRY_OK", "RIFTBORN_MULTIPLAYER_BLINK_OK", "RIFTBORN_FLOATING_TERRAIN_OK",
        "RIFTBORN_SCENE_RENDER_OK", "RIFTBORN_TRAVEL_RETURN_OK", "RIFTBORN_MULTIPLAYER_SMOKE_OK",
        "RIFTBORN_FLIGHT_MOVEMENT_OK", "RIFTBORN_FLIGHT_CONTROLS_OK", "RIFTBORN_ARMOR_RENDER_OK", "RIFTBORN_ARMOR_REMOVAL_OK",
        "RIFTBORN_ADVENTURE_FLIGHT_OK", "RIFTBORN_CREATIVE_FLIGHT_OK", "RIFTBORN_CREATIVE_ARMOR_REMOVAL_OK",
        "RIFTBORN_SURVIVAL_FLIGHT_RESET_OK", "RIFTBORN_DIMENSION_FLIGHT_OK", "RIFTBORN_FLIGHT_RECONNECT_OK", "RIFTBORN_RESPAWN_FLIGHT_RESET_OK",
    }
    armor = [("head", "helmet"), ("chest", "chestplate"), ("legs", "leggings"), ("feet", "boots")]
    server_checks = []
    surface_checks = set()
    natural_checks = set()
    def equipment(slot, item):
        command(f"item replace entity RiftbornTester armor.{slot} with {item}")
    deadline = time.monotonic() + 900
    try:
        start("server", "runSmokeServer")
        while time.monotonic() < deadline:
            try:
                name, line = messages.get(timeout=1)
            except queue.Empty:
                continue
            if line is None:
                code = processes[name].wait(timeout=10)
                if name == "client" and success and code == 0:
                    expected_stop = True
                    command("stop")
                elif name == "server" and expected_stop and code == 0:
                    if located != {"guardian_shrine", "overworld_ruin"}:
                        raise RuntimeError("Both natural structure locates must succeed: " + repr(located))
                    if not required_markers.issubset(markers):
                        raise RuntimeError("Missing client milestones: " + repr(required_markers - markers))
                    if surface_checks != {"rift_ruin", "guardian_shrine"} or natural_checks != surface_checks:
                        raise RuntimeError("Rift placement and natural-generation checks must both succeed")
                    if server_checks.count("flight_active") < 5 or server_checks.count("flight_absent") < 6 or server_checks.count("creative_native") < 2:
                        raise RuntimeError("Missing server-side flight validations: " + repr(server_checks))
                    print("RIFTBORN_DEDICATED_SERVER_AND_CLIENT_OK", flush=True)
                    return 0
                else:
                    raise RuntimeError(f"{name} exited unexpectedly (exit {code}, client success {success})")
                continue
            print(f"[{name}] {line}", flush=True)
            if name == "client":
                markers.update(marker for marker in required_markers if marker in line)
            if name == "server":
                for structure in ("rift_ruin", "guardian_shrine"):
                    if "RIFTBORN_SURFACE_PLACEMENT_OK " + structure in line: surface_checks.add(structure)
                    if "RIFTBORN_NATURAL_STRUCTURE_OK " + structure in line: natural_checks.add(structure)
                for state in ("flight_active", "flight_absent", "creative_native"):
                    if "RIFTBORN_SERVER_FLIGHT_CHECK " + state in line: server_checks.append(state)
            if name == "server" and "The nearest riftborn:" in line:
                for structure in ("guardian_shrine", "overworld_ruin"):
                    if "riftborn:" + structure in line: located.add(structure)
            if name == "server" and "Done (" in line and not server_ready:
                server_ready = True
                fixture()
                start("client", "runClientSmoke")
            if name == "server" and "RiftbornTester joined the game" in line and not joined:
                joined = True
                for cmd in ["gamemode survival RiftbornTester", "clear RiftbornTester",
                            "tp RiftbornTester 0.5 100 2.5 180 0", "effect give RiftbornTester minecraft:resistance 999 4 true",
                            "item replace entity RiftbornTester weapon.mainhand with riftborn:rift_core 3"]:
                    command(cmd)
            if name == "client" and "RIFTBORN_TRAVEL_ENTRY_OK" in line:
                command("execute in riftborn:the_rift run tp RiftbornTester 16.5 141 40.5 180 0")
                command("item replace entity RiftbornTester weapon.mainhand with riftborn:riftblade")
                command("execute in riftborn:the_rift run particle riftborn:rift_mote 16 144 27 4 2 4 0.02 150 force RiftbornTester")
            if name == "client" and "RIFTBORN_SCENE_RENDER_OK" in line:
                command("clear RiftbornTester")
                command("execute in riftborn:the_rift run tp RiftbornTester 16.5 141 6.5 180 0")
            if name == "client" and "RIFTBORN_TRAVEL_RETURN_OK" in line:
                command("tp RiftbornTester 0.5 100 2.5 -90 0")
                for slot, piece in armor: equipment(slot, "riftborn:rift_" + piece)
            if name == "client" and "RIFTBORN_FLIGHT_MOVEMENT_OK" in line:
                command("riftborn_test flight_active")
            if name == "client" and "RIFTBORN_ARMOR_RENDER_OK" in line:
                command("tp RiftbornTester 0.5 100 2.5 180 0")
                equipment("head", "minecraft:air")
            if name == "client" and "RIFTBORN_ARMOR_PIECE_REMOVED_OK" in line:
                piece = line.split()[-1]
                index = [name for _, name in armor].index(piece)
                command("riftborn_test flight_absent")
                equipment(armor[index][0], "riftborn:rift_" + piece)
                if index < 3: equipment(armor[index + 1][0], "minecraft:air")
                else:
                    command("gamemode adventure RiftbornTester")
                    command("tp RiftbornTester 0.5 106 2.5 180 0")
            if name == "client" and "RIFTBORN_ADVENTURE_FLIGHT_OK" in line:
                command("riftborn_test flight_active")
                command("gamemode creative RiftbornTester")
            if name == "client" and "RIFTBORN_CREATIVE_FLIGHT_OK" in line:
                command("riftborn_test creative_native"); equipment("head", "minecraft:air")
            if name == "client" and "RIFTBORN_CREATIVE_ARMOR_REMOVAL_OK" in line:
                command("riftborn_test creative_native"); command("gamemode survival RiftbornTester")
            if name == "client" and "RIFTBORN_SURVIVAL_FLIGHT_RESET_OK" in line:
                command("riftborn_test flight_absent"); equipment("head", "riftborn:rift_helmet")
                command("execute in riftborn:the_rift run tp RiftbornTester 16.5 149 40.5 160 0")
            if name == "client" and "RIFTBORN_DIMENSION_FLIGHT_OK" in line:
                command("riftborn_test flight_active")
                command("execute in minecraft:overworld run tp RiftbornTester 0.5 108 2.5 180 0")
            if name == "client" and "RIFTBORN_FLIGHT_RECONNECT_START" in line:
                command("riftborn_test flight_active")
            if name == "client" and "RIFTBORN_FLIGHT_RECONNECT_OK" in line:
                command("riftborn_test flight_active")
                command("gamerule keepInventory false"); command("kill RiftbornTester")
            if name == "client" and "RIFTBORN_RESPAWN_FLIGHT_RESET_OK" in line:
                command("riftborn_test flight_absent")
            if name == "client" and "RIFTBORN_MULTIPLAYER_SMOKE_OK" in line:
                success = True
            if "RIFTBORN_SMOKE_FAILURE:" in line:
                raise RuntimeError(line)
        raise TimeoutError("The multiplayer smoke test exceeded 15 minutes")
    finally:
        for process in processes.values():
            if process.poll() is None: process.terminate()
        for process in processes.values():
            try: process.wait(timeout=20)
            except subprocess.TimeoutExpired: process.kill()
        for log in logs.values(): log.close()


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:
        print(f"RIFTBORN_SMOKE_FAILURE: {exc}", file=sys.stderr)
        sys.exit(1)
