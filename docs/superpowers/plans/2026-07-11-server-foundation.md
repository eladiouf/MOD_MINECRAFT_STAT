# Premium RPG Server Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reproducible, auditable NeoForge 1.21.1 server foundation with pinned server mods, safe test/production profiles, automated backups, pre-generation tooling, diagnostics, and a dedicated-server smoke gate.

**Architecture:** Keep the development `libs/` directory as the client/development source, but make `server-pack/mods.lock.json` the authoritative deployment inventory. A standard-library Python CLI validates and synchronizes the server from that lock, renders environment profiles, and verifies logs; operational mods provide corpse recovery, profiling, pre-generation, and backup scheduling. The live server remains outside Git at `C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server`.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.232, Java 21, Gradle 8.10.2, Python 3 standard library, PowerShell, Modrinth-hosted pinned JARs.

## Global Constraints

- Target exactly Minecraft `1.21.1`, NeoForge `21.1.232`, and Java `21`.
- Support both 2–10-player private play and a future 10–50-player community server.
- The test world is generated normally; the production custom map must remain replaceable without changing progression code.
- Keep the main world permanent; seasonal systems must not reset player builds.
- New mods must provide a missing function, support NeoForge 1.21.1 and dedicated servers, and pass a complete boot plus client connection test.
- Never deploy client-only mods to the dedicated server.
- Preserve user changes already present in the dirty worktree; commits in this plan stage only the files named by each task.
- Do not install FTB Quests in this phase. Install and validate it with the later Quests and Onboarding phase so code/content/version ship together.
- The live server path is `C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server` and must never be committed.
- Every downloaded JAR must match its pinned SHA-512 before deployment.

## Planned File Structure

| Path | Responsibility |
|---|---|
| `server-pack/mods.lock.json` | Authoritative mod filename, source, side, version, URL, and checksum inventory. |
| `server-pack/profiles/test/server.properties.json` | Test-world property overrides. |
| `server-pack/profiles/production/server.properties.json` | Custom-map production property overrides. |
| `server-pack/config/simplebackups-common.toml` | Backup interval, retention, and destination policy. |
| `server-pack/config/corpse-server.toml` | Corpse ownership, persistence, and recovery policy after first generated config inspection. |
| `tools/server_pack.py` | Validate manifest, download pinned mods, synchronize server mods, render profiles, and inspect smoke logs. |
| `tools/tests/test_server_pack.py` | Unit tests for manifest safety, side filtering, profile rendering, and smoke-log detection. |
| `tools/server/verify_restore.ps1` | Copy a backup to an isolated restore directory and verify its world metadata. |
| `tools/server/pregenerate_test_world.ps1` | Issue the exact Chunky commands through RCON after explicit operator setup. |
| `docs/server/OPERATIONS.md` | Install, start, stop, backup, restore, profile, pre-generation, diagnostics, update, and rollback runbook. |
| `docs/server/CLIENT_SERVER_MATRIX.md` | Exact client/both/server-only deployment matrix and rationale. |

---

### Task 1: Create the locked mod inventory and validator

**Files:**
- Create: `server-pack/mods.lock.json`
- Create: `tools/server_pack.py`
- Create: `tools/tests/test_server_pack.py`

**Interfaces:**
- Produces: `load_manifest(path: Path) -> dict`
- Produces: `validate_manifest(manifest: dict) -> list[str]`
- Produces: CLI `python tools/server_pack.py validate --manifest server-pack/mods.lock.json`
- Consumes: current `libs/*.jar` filenames and the four pinned Modrinth artifacts below.

- [ ] **Step 1: Write the failing manifest-validation tests**

Create `tools/tests/test_server_pack.py` with:

```python
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.server_pack import load_manifest, validate_manifest


class ManifestValidationTest(unittest.TestCase):
    def test_rejects_duplicate_filename_and_invalid_side(self):
        manifest = {
            "schema": 1,
            "minecraft": "1.21.1",
            "neoforge": "21.1.232",
            "mods": [
                {"id": "a", "filename": "same.jar", "side": "both", "source": "local"},
                {"id": "b", "filename": "same.jar", "side": "wrong", "source": "local"},
            ],
        }
        errors = validate_manifest(manifest)
        self.assertTrue(any("duplicate filename" in error for error in errors))
        self.assertTrue(any("invalid side" in error for error in errors))

    def test_remote_mod_requires_url_and_sha512(self):
        manifest = {
            "schema": 1,
            "minecraft": "1.21.1",
            "neoforge": "21.1.232",
            "mods": [{"id": "corpse", "filename": "corpse.jar", "side": "both", "source": "remote"}],
        }
        errors = validate_manifest(manifest)
        self.assertTrue(any("url" in error for error in errors))
        self.assertTrue(any("sha512" in error for error in errors))

    def test_load_manifest_reads_utf8_json(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "mods.lock.json"
            path.write_text(json.dumps({"schema": 1, "mods": []}), encoding="utf-8")
            self.assertEqual(1, load_manifest(path)["schema"])


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
python -m unittest tools.tests.test_server_pack -v
```

Expected: `ModuleNotFoundError: No module named 'tools.server_pack'`.

- [ ] **Step 3: Implement the complete manifest loader and validator**

Create `tools/server_pack.py` with these imports and functions:

```python
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
import tempfile
import urllib.request
from pathlib import Path

VALID_SIDES = {"client", "both", "server"}
VALID_SOURCES = {"local", "remote", "build"}


def load_manifest(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValueError("manifest root must be an object")
    return value


def validate_manifest(manifest: dict) -> list[str]:
    errors: list[str] = []
    if manifest.get("schema") != 1:
        errors.append("schema must be 1")
    if manifest.get("minecraft") != "1.21.1":
        errors.append("minecraft must be 1.21.1")
    if manifest.get("neoforge") != "21.1.232":
        errors.append("neoforge must be 21.1.232")
    mods = manifest.get("mods")
    if not isinstance(mods, list):
        return errors + ["mods must be an array"]
    seen: set[str] = set()
    for index, mod in enumerate(mods):
        prefix = f"mods[{index}]"
        if not isinstance(mod, dict):
            errors.append(f"{prefix} must be an object")
            continue
        filename = mod.get("filename")
        if not isinstance(filename, str) or not filename.endswith(".jar"):
            errors.append(f"{prefix} filename must end in .jar")
        elif filename.casefold() in seen:
            errors.append(f"{prefix} duplicate filename: {filename}")
        else:
            seen.add(filename.casefold())
        if mod.get("side") not in VALID_SIDES:
            errors.append(f"{prefix} invalid side: {mod.get('side')}")
        if mod.get("source") not in VALID_SOURCES:
            errors.append(f"{prefix} invalid source: {mod.get('source')}")
        if mod.get("source") == "remote":
            if not isinstance(mod.get("url"), str) or not mod["url"].startswith("https://"):
                errors.append(f"{prefix} remote mod requires https url")
            sha512 = mod.get("sha512")
            if not isinstance(sha512, str) or len(sha512) != 128:
                errors.append(f"{prefix} remote mod requires 128-character sha512")
    return errors
```

Add the CLI entry point at the end of the same file:

```python
def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Manage the STAT Mod server pack")
    parser.add_argument("--manifest", type=Path, default=Path("server-pack/mods.lock.json"))
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("validate")
    args = parser.parse_args(argv)
    manifest = load_manifest(args.manifest)
    errors = validate_manifest(manifest)
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print(f"VALID: {len(manifest['mods'])} locked mods")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 4: Create the lock inventory**

Create `server-pack/mods.lock.json` with the following top-level form and generate one `local` entry for every current `libs/*.jar`; classify the three known dedicated-server failures as `client`, all other current JARs as `both`, and add the four exact remote entries shown below:

```json
{
  "schema": 1,
  "minecraft": "1.21.1",
  "neoforge": "21.1.232",
  "mods": [
    {
      "id": "corpse",
      "version": "neoforge-1.21.1-1.1.13",
      "filename": "corpse-neoforge-1.21.1-1.1.13.jar",
      "side": "both",
      "source": "remote",
      "url": "https://cdn.modrinth.com/data/WrpuIfhw/versions/Zwf8nv8y/corpse-neoforge-1.21.1-1.1.13.jar",
      "sha512": "473aafd82008c1e041e3b4a5a177507d555c8bc9dd1f121f252f1e81bc0c69c79a91cb64be0df343babcb3d4db76efbaa7aa2adaeaae29337808a368bc290ad0"
    },
    {
      "id": "spark",
      "version": "1.10.124-neoforge-1.21.1",
      "filename": "spark-1.10.124-neoforge.jar",
      "side": "server",
      "source": "remote",
      "url": "https://cdn.modrinth.com/data/l6YH9Als/versions/v5qtqRQi/spark-1.10.124-neoforge.jar",
      "sha512": "f86ce34f2759c69df82578c397ff55b666c84626229a98f598458b960c21b38c95d6bfef4772af7f963c4f4868e5e2d9aef6b99c1d51bab55bf45e0e6e6b5ed4"
    },
    {
      "id": "chunky",
      "version": "1.4.23",
      "filename": "Chunky-NeoForge-1.4.23.jar",
      "side": "server",
      "source": "remote",
      "url": "https://cdn.modrinth.com/data/fALzjamp/versions/LuFhm4eU/Chunky-NeoForge-1.4.23.jar",
      "sha512": "2db769dd723f243a21e1881e7c9f825e9c193da6f2bed454b70cb6fa9e51c57f63fdcf017c0657bbd26f7bba30815413e27c74d3c7be0783390a96ee9baa4bf7"
    },
    {
      "id": "simple_backups",
      "version": "1.21-4.0.30",
      "filename": "SimpleBackups-1.21-4.0.30.jar",
      "side": "server",
      "source": "remote",
      "url": "https://cdn.modrinth.com/data/fzSKSXVK/versions/tHyCDNNH/SimpleBackups-1.21-4.0.30.jar",
      "sha512": "a1a9953182cc6fd9e9c6047b75fdac673802a66a88c11644d7953705270ba299aa7db8b9ebec7086b655dbb15fbf69bf9b77ad47be251ed356ecb184fbd56dc9"
    }
  ]
}
```

The generated local entries must use stable IDs derived from lowercase filenames, `source: "local"`, and the exact original filename. Explicitly set these entries to `side: "client"`:

```text
distraction_free_recipes-neoforge-1.2.2-1.21.1.jar
Epic Fight x Curios Compat 2.2.jar
indestructible-21.16.3-1.21.1.jar
```

Add one build entry for the project JAR:

```json
{
  "id": "statmod",
  "version": "1.2.0",
  "filename": "statmod-1.2.0.jar",
  "side": "both",
  "source": "build",
  "path": "build/libs/statmod-1.2.0.jar"
}
```

- [ ] **Step 5: Run tests and lock validation**

Run:

```powershell
python -m unittest tools.tests.test_server_pack -v
python tools/server_pack.py --manifest server-pack/mods.lock.json validate
```

Expected: all tests pass and the validator prints `VALID:` with the final locked-mod count.

- [ ] **Step 6: Commit the manifest foundation**

```powershell
git add server-pack/mods.lock.json tools/server_pack.py tools/tests/test_server_pack.py
git commit -m "feat: add locked server mod inventory"
```

---

### Task 2: Download, checksum, and synchronize server mods

**Files:**
- Modify: `tools/server_pack.py`
- Modify: `tools/tests/test_server_pack.py`

**Interfaces:**
- Consumes: validated schema from Task 1.
- Produces: `sha512_file(path: Path) -> str`
- Produces: `resolve_source(mod: dict, repo_root: Path, cache_dir: Path) -> Path`
- Produces: `sync_mods(manifest: dict, repo_root: Path, server_dir: Path, cache_dir: Path) -> tuple[int, list[str]]`
- Produces: CLI `sync --server-dir $ServerDir --cache-dir $CacheDir`.

- [ ] **Step 1: Add failing synchronization tests**

Append to `tools/tests/test_server_pack.py`:

```python
from tools.server_pack import sha512_file, sync_mods


class SynchronizationTest(unittest.TestCase):
    def test_sync_copies_server_and_both_but_excludes_client(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / "libs").mkdir()
            (root / "build/libs").mkdir(parents=True)
            (root / "libs/both.jar").write_bytes(b"both")
            (root / "libs/client.jar").write_bytes(b"client")
            (root / "build/libs/statmod.jar").write_bytes(b"build")
            manifest = {
                "schema": 1, "minecraft": "1.21.1", "neoforge": "21.1.232",
                "mods": [
                    {"id": "both", "filename": "both.jar", "side": "both", "source": "local"},
                    {"id": "client", "filename": "client.jar", "side": "client", "source": "local"},
                    {"id": "statmod", "filename": "statmod.jar", "side": "both", "source": "build", "path": "build/libs/statmod.jar"},
                ],
            }
            count, removed = sync_mods(manifest, root, root / "server", root / "cache")
            self.assertEqual(2, count)
            self.assertEqual(["client.jar"], removed)
            self.assertEqual({"both.jar", "statmod.jar"}, {p.name for p in (root / "server/mods").glob("*.jar")})

    def test_sha512_matches_known_content(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "value.jar"
            path.write_bytes(b"statmod")
            self.assertEqual(hashlib.sha512(b"statmod").hexdigest(), sha512_file(path))
```

- [ ] **Step 2: Run tests and verify RED**

Run `python -m unittest tools.tests.test_server_pack.SynchronizationTest -v`.

Expected: import failure for `sha512_file` or `sync_mods`.

- [ ] **Step 3: Implement atomic source resolution and synchronization**

Add to `tools/server_pack.py`:

```python
def sha512_file(path: Path) -> str:
    digest = hashlib.sha512()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def resolve_source(mod: dict, repo_root: Path, cache_dir: Path) -> Path:
    source = mod["source"]
    if source == "local":
        path = repo_root / "libs" / mod["filename"]
    elif source == "build":
        path = repo_root / mod["path"]
    else:
        cache_dir.mkdir(parents=True, exist_ok=True)
        path = cache_dir / mod["filename"]
        if not path.exists() or sha512_file(path) != mod["sha512"].lower():
            with tempfile.NamedTemporaryFile(delete=False, dir=cache_dir, suffix=".part") as handle:
                temporary = Path(handle.name)
            try:
                request = urllib.request.Request(mod["url"], headers={"User-Agent": "STAT-Mod-Server-Pack/1.0"})
                with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as output:
                    shutil.copyfileobj(response, output)
                if sha512_file(temporary) != mod["sha512"].lower():
                    raise ValueError(f"SHA-512 mismatch for {mod['filename']}")
                temporary.replace(path)
            finally:
                temporary.unlink(missing_ok=True)
    if not path.is_file():
        raise FileNotFoundError(path)
    return path


def sync_mods(manifest: dict, repo_root: Path, server_dir: Path, cache_dir: Path) -> tuple[int, list[str]]:
    errors = validate_manifest(manifest)
    if errors:
        raise ValueError("; ".join(errors))
    mods_dir = server_dir / "mods"
    mods_dir.mkdir(parents=True, exist_ok=True)
    desired: dict[str, Path] = {}
    excluded: list[str] = []
    for mod in manifest["mods"]:
        if mod["side"] == "client":
            excluded.append(mod["filename"])
            continue
        desired[mod["filename"]] = resolve_source(mod, repo_root, cache_dir)
    quarantine = server_dir / "disabled-unlocked-mods"
    for existing in mods_dir.glob("*.jar"):
        if existing.name not in desired:
            quarantine.mkdir(parents=True, exist_ok=True)
            existing.replace(quarantine / existing.name)
    for filename, source in desired.items():
        target = mods_dir / filename
        if not target.exists() or sha512_file(target) != sha512_file(source):
            temporary = target.with_suffix(target.suffix + ".part")
            shutil.copy2(source, temporary)
            temporary.replace(target)
    return len(desired), sorted(excluded, key=str.casefold)
```

Extend the CLI with arguments `--server-dir`, `--cache-dir`, and the `sync` command. The command must resolve `repo_root = Path(__file__).resolve().parents[1]`, call `sync_mods`, print copied/excluded counts, and return nonzero on any exception.

- [ ] **Step 4: Run unit tests and dry-run against a temporary server**

```powershell
python -m unittest tools.tests.test_server_pack -v
python tools/server_pack.py --manifest server-pack/mods.lock.json sync --server-dir "$env:TEMP\statmod-server-smoke" --cache-dir "$env:TEMP\statmod-mod-cache"
```

Expected: tests pass; synchronization reports the locked server count; the three client-only JARs are absent; the four remote JAR hashes match the lock.

- [ ] **Step 5: Commit synchronizer**

```powershell
git add tools/server_pack.py tools/tests/test_server_pack.py
git commit -m "feat: synchronize verified server mods"
```

---

### Task 3: Add deterministic test and production property profiles

**Files:**
- Create: `server-pack/profiles/test/server.properties.json`
- Create: `server-pack/profiles/production/server.properties.json`
- Modify: `tools/server_pack.py`
- Modify: `tools/tests/test_server_pack.py`

**Interfaces:**
- Produces: `parse_properties(text: str) -> dict[str, str]`
- Produces: `render_properties(base: str, overrides: dict[str, object]) -> str`
- Produces: CLI `profile --server-dir $ServerDir --profile test|production`.

- [ ] **Step 1: Add failing profile tests**

```python
from tools.server_pack import parse_properties, render_properties


class ProfileTest(unittest.TestCase):
    def test_render_replaces_keys_and_is_deterministic(self):
        base = "motd=Old\ndifficulty=easy\nserver-port=25565\n"
        rendered = render_properties(base, {"difficulty": "normal", "motd": "STAT RPG | Test"})
        self.assertEqual("difficulty=normal\nmotd=STAT RPG | Test\nserver-port=25565\n", rendered)

    def test_parse_ignores_comments_and_blank_lines(self):
        self.assertEqual({"a": "b"}, parse_properties("# comment\n\na=b\n"))
```

- [ ] **Step 2: Run test and verify RED**

Run `python -m unittest tools.tests.test_server_pack.ProfileTest -v`.

Expected: import failure for profile functions.

- [ ] **Step 3: Create exact profile JSON files**

Test profile:

```json
{
  "difficulty": "normal",
  "enable-rcon": false,
  "enforce-whitelist": false,
  "level-name": "world-test",
  "level-seed": "stat-rpg-foundation-2026",
  "max-players": 10,
  "motd": "STAT RPG | Monde de test",
  "online-mode": true,
  "simulation-distance": 8,
  "spawn-protection": 16,
  "view-distance": 10,
  "white-list": false
}
```

Production profile:

```json
{
  "difficulty": "normal",
  "enable-rcon": false,
  "enforce-whitelist": true,
  "level-name": "world-production",
  "max-players": 50,
  "motd": "STAT RPG | Les Cendres vous attendent",
  "online-mode": true,
  "simulation-distance": 8,
  "spawn-protection": 32,
  "view-distance": 10,
  "white-list": true
}
```

Do not set a production seed because the custom map replaces generation.

- [ ] **Step 4: Implement deterministic property rendering**

```python
def parse_properties(text: str) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key] = value
    return values


def render_properties(base: str, overrides: dict[str, object]) -> str:
    values = parse_properties(base)
    for key, value in overrides.items():
        if isinstance(value, bool):
            values[key] = str(value).lower()
        else:
            values[key] = str(value)
    return "".join(f"{key}={values[key]}\n" for key in sorted(values))
```

Add CLI command `profile` with `--profile {test,production}` and `--server-dir`. It reads the existing `server.properties`, loads the selected file under `server-pack/profiles/test/` or `server-pack/profiles/production/`, writes `server.properties.part`, then atomically replaces `server.properties`. It must refuse `production` when `world-production/level.dat` is absent unless `--allow-missing-world` is explicitly supplied.

- [ ] **Step 5: Verify profile tests and rendering**

```powershell
python -m unittest tools.tests.test_server_pack -v
python tools/server_pack.py profile --server-dir "$env:TEMP\statmod-server-smoke" --profile test
Get-Content "$env:TEMP\statmod-server-smoke\server.properties"
```

Expected: deterministic sorted keys with `difficulty=normal`, test seed, and no RCON password.

- [ ] **Step 6: Commit profiles**

```powershell
git add server-pack/profiles tools/server_pack.py tools/tests/test_server_pack.py
git commit -m "feat: add reproducible server profiles"
```

---

### Task 4: Generate and lock Corpse and Simple Backups policies

**Files:**
- Create: `server-pack/config/corpse-server.toml`
- Create: `server-pack/config/simplebackups-common.toml`
- Modify: `tools/server_pack.py`
- Modify: `tools/tests/test_server_pack.py`

**Interfaces:**
- Produces: `sync_config(source_dir: Path, server_dir: Path) -> int`.
- Consumes: actual configuration keys generated by Corpse 1.1.13 and Simple Backups 4.0.30 during the first isolated boot.

- [ ] **Step 1: Synchronize the four new mods into the isolated server**

```powershell
.\gradlew.bat build
python tools/server_pack.py sync --server-dir "$env:TEMP\statmod-server-smoke" --cache-dir "$env:TEMP\statmod-mod-cache"
```

Copy the existing NeoForge `libraries`, `run.bat`, `user_jvm_args.txt`, and EULA into the isolated server, then boot once until config generation completes. Stop with the `stop` console command; do not kill Java during a world save.

- [ ] **Step 2: Inspect generated keys before writing policy**

Run:

```powershell
Get-ChildItem "$env:TEMP\statmod-server-smoke\config" -Recurse -File | Where-Object Name -Match 'corpse|backup' | Select-Object FullName
$corpseConfig = Get-ChildItem "$env:TEMP\statmod-server-smoke\config" -Recurse -File | Where-Object Name -Match '^corpse.*\.toml$' | Select-Object -First 1
$backupConfig = Get-ChildItem "$env:TEMP\statmod-server-smoke\config" -Recurse -File | Where-Object Name -Match 'simple.*backup.*\.toml$' | Select-Object -First 1
if (-not $corpseConfig -or -not $backupConfig) { throw 'Expected generated Corpse and Simple Backups configs' }
Get-Content -LiteralPath $corpseConfig.FullName
Get-Content -LiteralPath $backupConfig.FullName
```

Record the exact filenames and keys. If either mod does not generate server-readable config or fails dedicated boot, stop this plan and replace that dependency in the lock rather than inventing keys.

- [ ] **Step 3: Add failing config-copy test**

```python
from tools.server_pack import sync_config


class ConfigSyncTest(unittest.TestCase):
    def test_sync_config_preserves_relative_paths(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / "source"
            (source / "nested").mkdir(parents=True)
            (source / "nested/value.toml").write_text("enabled=true\n", encoding="utf-8")
            count = sync_config(source, root / "server")
            self.assertEqual(1, count)
            self.assertEqual("enabled=true\n", (root / "server/config/nested/value.toml").read_text(encoding="utf-8"))
```

- [ ] **Step 4: Run config test and verify RED**

Run `python -m unittest tools.tests.test_server_pack.ConfigSyncTest -v`.

Expected: import failure for `sync_config`.

- [ ] **Step 5: Implement configuration synchronization**

```python
def sync_config(source_dir: Path, server_dir: Path) -> int:
    files = [path for path in source_dir.rglob("*") if path.is_file()]
    for source in files:
        relative = source.relative_to(source_dir)
        target = server_dir / "config" / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_suffix(target.suffix + ".part")
        shutil.copy2(source, temporary)
        temporary.replace(target)
    return len(files)
```

Add CLI command `config --server-dir $ServerDir` that synchronizes `server-pack/config`.

- [ ] **Step 6: Write policy files using only generated, verified keys**

Copy the generated files to the planned paths and change only values needed to enforce these outcomes:

- Corpse inventory persists indefinitely until emptied.
- The corpse cannot burn or be destroyed by ordinary environmental damage.
- Owner-only access is enabled for the longest supported protection duration; if team access is supported natively, enable it after owner protection.
- Player teleport-to-corpse is disabled.
- Operators retain corpse-history and recovery commands.
- Backups run every 60 minutes while the server is active.
- Keep 24 hourly backups and 7 daily recovery points if the mod supports both dimensions; otherwise retain the newest 48 archives.
- Backup destination is outside the world directory at `backups/`.
- Announce backup start/completion to operators, not all players.

Add comments beside each changed key explaining the intended outcome. Do not invent unsupported options; document any unsupported Corpse behavior for a later STAT Mod integration task.

- [ ] **Step 7: Verify config sync and dedicated boot**

```powershell
python -m unittest tools.tests.test_server_pack -v
python tools/server_pack.py config --server-dir "$env:TEMP\statmod-server-smoke"
```

Boot the isolated server, confirm no config parse warning, run or wait for one backup, then stop cleanly. Expected: at least one readable archive in `backups/` and a corpse-related config load line without ERROR/FATAL.

- [ ] **Step 8: Commit operational configs**

```powershell
git add server-pack/config tools/server_pack.py tools/tests/test_server_pack.py
git commit -m "feat: configure corpse recovery and backups"
```

---

### Task 5: Add backup restoration verification

**Files:**
- Create: `tools/server/verify_restore.ps1`
- Create: `docs/server/OPERATIONS.md`

**Interfaces:**
- Produces: PowerShell parameters `-Archive`, `-RestoreRoot`, and optional `-ExpectedLevelName`.
- Produces: exit `0` only when `level.dat`, `region/`, and at least one region file exist after extraction.

- [ ] **Step 1: Create a deliberately invalid archive and record the failing manual check**

```powershell
$bad = Join-Path $env:TEMP 'bad-backup.zip'
Compress-Archive -LiteralPath README.md -DestinationPath $bad -Force
Test-Path (Join-Path $env:TEMP 'statmod-restore-check\level.dat')
```

Expected: `False`; there is no reusable verifier yet.

- [ ] **Step 2: Implement `verify_restore.ps1`**

```powershell
param(
    [Parameter(Mandatory=$true)][string]$Archive,
    [Parameter(Mandatory=$true)][string]$RestoreRoot,
    [string]$ExpectedLevelName = 'world-test'
)
$ErrorActionPreference = 'Stop'
$archivePath = (Resolve-Path -LiteralPath $Archive).Path
$root = [System.IO.Path]::GetFullPath($RestoreRoot)
if (Test-Path -LiteralPath $root) {
    throw "RestoreRoot already exists: $root"
}
New-Item -ItemType Directory -Path $root | Out-Null
Expand-Archive -LiteralPath $archivePath -DestinationPath $root
$level = Get-ChildItem -LiteralPath $root -Recurse -Filter level.dat -File | Select-Object -First 1
if (-not $level) { throw 'Backup does not contain level.dat' }
$world = $level.Directory
$region = Join-Path $world.FullName 'region'
if (-not (Test-Path -LiteralPath $region -PathType Container)) { throw 'Backup does not contain region/' }
$mca = Get-ChildItem -LiteralPath $region -Filter '*.mca' -File | Select-Object -First 1
if (-not $mca) { throw 'Backup contains no region file' }
Write-Output "RESTORE VALID: $($world.FullName)"
```

- [ ] **Step 3: Verify failure and success paths**

Run against the invalid archive. Expected: nonzero with `Backup does not contain level.dat`.

Run against the real archive produced in Task 4:

```powershell
$realBackup = Get-ChildItem "$env:TEMP\statmod-server-smoke\backups" -Recurse -Filter '*.zip' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $realBackup) { throw 'No Simple Backups archive found' }
.\tools\server\verify_restore.ps1 -Archive $realBackup.FullName -RestoreRoot "$env:TEMP\statmod-restore-valid"
```

Expected: exit `0` and `RESTORE VALID:`. Delete only the disposable `$env:TEMP\statmod-restore-valid` after inspecting it.

- [ ] **Step 4: Write the initial operations runbook**

Create `docs/server/OPERATIONS.md` with exact sections:

1. Prerequisites: Java 21, Python 3, 6 GiB heap, server path.
2. Build and validate: Gradle and manifest commands.
3. Synchronize mods and configs.
4. Apply test or production profile.
5. Start and clean stop procedure.
6. Backup schedule and archive location.
7. Restore rehearsal with `verify_restore.ps1`.
8. Rollback: stop, preserve failed state, restore archive, restore previous lock/JAR, start and smoke-check.

Include a warning that the production profile must never be applied before the custom map and its verified backup exist.

- [ ] **Step 5: Commit restoration tooling**

```powershell
git add tools/server/verify_restore.ps1 docs/server/OPERATIONS.md
git commit -m "ops: verify server backup restoration"
```

---

### Task 6: Add Chunky pre-generation and spark diagnostic procedures

**Files:**
- Create: `tools/server/pregenerate_test_world.ps1`
- Modify: `docs/server/OPERATIONS.md`

**Interfaces:**
- Produces: script parameters `-ServerHost`, `-RconPort`, `-RconPassword`, `-Radius`.
- Consumes: an operator-enabled, temporary RCON session and Chunky 1.4.23 commands.

- [ ] **Step 1: Verify exact commands on the isolated test server console**

Run these commands manually in the isolated server console:

```text
chunky world world-test
chunky shape circle
chunky center 0 0
chunky radius 5000
chunky start
chunky progress
```

Expected: Chunky accepts each command. If syntax differs, use `/chunky help` and record the accepted 1.4.23 syntax before writing the script.

- [ ] **Step 2: Implement pre-generation script with explicit safety bounds**

Create `tools/server/pregenerate_test_world.ps1`. Use a locally installed RCON client declared in the runbook; do not embed credentials. The script must:

```powershell
param(
    [string]$ServerHost = '127.0.0.1',
    [int]$RconPort = 25575,
    [Parameter(Mandatory=$true)][string]$RconPassword,
    [ValidateRange(1000,10000)][int]$Radius = 5000
)
$ErrorActionPreference = 'Stop'
$commands = @(
    'chunky world world-test',
    'chunky shape circle',
    'chunky center 0 0',
    "chunky radius $Radius",
    'chunky start'
)
foreach ($command in $commands) {
    & mcrcon -H $ServerHost -P $RconPort -p $RconPassword $command
    if ($LASTEXITCODE -ne 0) { throw "RCON failed: $command" }
}
Write-Output "PREGEN STARTED: world-test radius=$Radius"
```

- [ ] **Step 3: Document temporary RCON and diagnostics**

Extend `OPERATIONS.md` with:

- RCON stays disabled in committed profiles.
- An operator may enable it temporarily with a unique secret, bind firewall access to localhost, pre-generate, stop the server, and disable RCON again.
- Default test radius is 5,000 blocks; production radius is chosen only after the custom map boundary is known.
- Run `spark profiler start --timeout 120` during spawn generation and a representative combat/dungeon session.
- Save spark report URLs and timestamp them in an operations log.
- Initial targets: mean tick below 40 ms, 95th percentile below 50 ms outside controlled world generation, and no sustained watchdog warnings.

- [ ] **Step 4: Validate script guardrails without contacting a server**

```powershell
.\tools\server\pregenerate_test_world.ps1 -RconPassword fake -Radius 999
```

Expected: PowerShell rejects `999` via `ValidateRange` before invoking RCON.

- [ ] **Step 5: Commit pre-generation and diagnostics docs**

```powershell
git add tools/server/pregenerate_test_world.ps1 docs/server/OPERATIONS.md
git commit -m "ops: add world pre-generation and profiling workflow"
```

---

### Task 7: Add an automated dedicated-server smoke gate

**Files:**
- Modify: `tools/server_pack.py`
- Modify: `tools/tests/test_server_pack.py`
- Create: `docs/server/CLIENT_SERVER_MATRIX.md`
- Modify: `.github/workflows/build.yml`

**Interfaces:**
- Produces: `inspect_log(text: str) -> list[str]` where an empty list means success.
- Produces: CLI `smoke-log --log $LatestLog`.
- CI consumes manifest validation and Python unit tests; live boot remains a local release gate because proprietary/local mod JARs are not available in CI.

- [ ] **Step 1: Add failing smoke-log tests**

```python
from tools.server_pack import inspect_log


class SmokeLogTest(unittest.TestCase):
    def test_accepts_done_and_statmod_initialization(self):
        log = "STAT Mod initialized on NeoForge 1.21.1\nDone (12.3s)! For help, type help\n"
        self.assertEqual([], inspect_log(log))

    def test_rejects_missing_done_and_fatal(self):
        errors = inspect_log("[main/FATAL] Failed to start the minecraft server\n")
        self.assertTrue(any("FATAL" in error for error in errors))
        self.assertTrue(any("Done" in error for error in errors))
```

- [ ] **Step 2: Run smoke tests and verify RED**

Run `python -m unittest tools.tests.test_server_pack.SmokeLogTest -v`.

Expected: import failure for `inspect_log`.

- [ ] **Step 3: Implement log inspection**

```python
def inspect_log(text: str) -> list[str]:
    errors: list[str] = []
    if "STAT Mod initialized on NeoForge 1.21.1" not in text:
        errors.append("missing STAT Mod initialization marker")
    if "Done (" not in text:
        errors.append("missing dedicated server Done marker")
    fatal_lines = [line.strip() for line in text.splitlines() if "/FATAL]" in line or "Failed to start the minecraft server" in line]
    errors.extend(f"FATAL: {line}" for line in fatal_lines)
    return errors
```

Add `smoke-log --log $LatestLog` to the CLI. It prints each problem and returns `1`, or prints `SMOKE PASS` and returns `0`.

- [ ] **Step 4: Write client/server matrix**

Generate `docs/server/CLIENT_SERVER_MATRIX.md` from the lock inventory with columns `Mod/JAR`, `Side`, `Source`, and `Reason`. Explicitly document:

- Corpse: both client and server.
- spark, Chunky, Simple Backups: server only.
- distraction_free_recipes, Epic Fight x Curios Compat, indestructible: client only because each failed dedicated-side class loading during the 2026-07-11 boot audit.
- STAT Mod: both.

Every remaining local JAR must appear once; unknown side classification is not permitted.

- [ ] **Step 5: Add portable checks to CI**

In `.github/workflows/build.yml`, after the Java build step, add:

```yaml
      - name: Validate server-pack lock
        run: python tools/server_pack.py --manifest server-pack/mods.lock.json validate

      - name: Test server-pack tooling
        run: python -m unittest tools.tests.test_server_pack -v
```

- [ ] **Step 6: Run all portable checks**

```powershell
python -m unittest tools.tests.test_server_pack -v
python tools/server_pack.py --manifest server-pack/mods.lock.json validate
.\gradlew.bat build
```

Expected: all Python tests pass, lock is valid, and Gradle prints `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit smoke gate and matrix**

```powershell
git add tools/server_pack.py tools/tests/test_server_pack.py docs/server/CLIENT_SERVER_MATRIX.md .github/workflows/build.yml
git commit -m "test: add dedicated server foundation gates"
```

---

### Task 8: Deploy the foundation and perform the release rehearsal

**Files:**
- Modify: `docs/server/OPERATIONS.md`
- Runtime only: `C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server`

**Interfaces:**
- Consumes: every command and artifact produced by Tasks 1–7.
- Produces: a live server with a locked server-only mod set, current STAT Mod JAR, test profile, verified backup, completed pre-generation command, spark baseline, and passing smoke log.

- [ ] **Step 1: Stop and snapshot the current live server**

Use the console `stop` command and wait for the Java process to exit. Copy the entire live server to a timestamped sibling backup before synchronizing. Verify the snapshot contains `world/level.dat`, `mods/`, `config/`, `server.properties`, and `libraries/`.

- [ ] **Step 2: Build and deploy atomically**

```powershell
.\gradlew.bat build
python tools/server_pack.py --manifest server-pack/mods.lock.json validate
python tools/server_pack.py --manifest server-pack/mods.lock.json sync --server-dir 'C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server' --cache-dir "$env:LOCALAPPDATA\statmod\mod-cache"
python tools/server_pack.py config --server-dir 'C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server'
python tools/server_pack.py profile --server-dir 'C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server' --profile test
```

Expected: client-only mods are quarantined, unknown stale JARs are moved to `disabled-unlocked-mods`, and all remote hashes pass.

- [ ] **Step 3: Start and verify the full server**

Launch `start.bat`, wait for `Done`, then run:

```powershell
python tools/server_pack.py smoke-log --log 'C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server\logs\latest.log'
netstat -ano | Select-String ':25565'
```

Expected: `SMOKE PASS` and TCP listening on `0.0.0.0:25565` and/or `[::]:25565`.

- [ ] **Step 4: Perform one real client connection test**

Connect with the exact client pack. Create a disposable player profile, move, fight one mob, open STAT UI, and confirm Corpse appears after a controlled death. Recover inventory and verify no item duplication. Record the result and the exact client mod count in `OPERATIONS.md` under a dated release-rehearsal entry.

- [ ] **Step 5: Verify backup and restore rehearsal**

Wait for or trigger a Simple Backups archive. Stop the server cleanly, run `verify_restore.ps1` against that archive into a disposable directory, inspect `level.dat` and region files, then restart the live server. Expected: restore verifier exits `0` and the live smoke gate passes again after restart.

- [ ] **Step 6: Run a short profiling baseline**

Run a 120-second spark profile during normal spawn activity, then start the test-world Chunky pre-generation at radius 5,000. Record the spark URL, mean tick time, 95th percentile, Java heap, world seed, and Chunky task parameters in the runbook.

- [ ] **Step 7: Final foundation audit**

Verify each acceptance item directly:

```powershell
.\gradlew.bat build
python -m unittest tools.tests.test_server_pack -v
python tools/server_pack.py --manifest server-pack/mods.lock.json validate
python tools/server_pack.py smoke-log --log 'C:\Users\El Hadji\Downloads\STAT_MOD\neoforge-server\logs\latest.log'
```

Also verify: successful client connection, Corpse recovery, one real backup, one restore rehearsal, port 25565 listening, three client-only mods absent from `mods/`, four foundation mods present, and no unknown JAR outside quarantine.

- [ ] **Step 8: Commit the dated rehearsal evidence**

```powershell
git add docs/server/OPERATIONS.md
git commit -m "docs: record server foundation rehearsal"
```

The foundation phase is complete only after every Task 8 check has direct evidence. After completion, begin a separate plan for **Progression of the Five Acts**; do not expand this plan with act logic or quest content.
