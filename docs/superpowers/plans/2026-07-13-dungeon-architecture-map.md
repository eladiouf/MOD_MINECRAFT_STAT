# Dungeon Architecture Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate and publish a faithful 3840 x 2160 architectural map of Floor 0, a normal dungeon floor, and the floors 1-100 progression directly from Java source rules.

**Architecture:** A standalone Python/Pillow renderer parses targeted constants, positions, site declarations, room-grid rules, role cadence, and chapter names from the dungeon Java sources. Pure extraction and layout functions are tested before the renderer composes the three-panel blueprint and emits a validation report.

**Tech Stack:** Python 3, Pillow 12.2.0, `ast`, `re`, `unittest`, MkDocs.

## Global Constraints

- Output is exactly 3840 x 2160.
- Geometry and progression values are parsed from Java sources rather than silently duplicated.
- Parsing stops with a named error when an expected source pattern is absent.
- Labels are French and the map does not claim to be a block-by-block world export.
- Existing unrelated working-tree changes are not modified or reverted.

---

### Task 1: Dungeon Source Extractor

**Files:**
- Create: `scripts/render_dungeon_map.py`
- Create: `scripts/tests/test_render_dungeon_map.py`

**Interfaces:**
- Produces: `extract_dungeon_model(root: Path, representative_floor: int) -> DungeonModel`
- Produces: `city_to_canvas(model: CityModel, box: Box) -> CoordinateTransform`
- Produces: `floor_role(floor: int, cadence: RoleCadence) -> FloorRole`

- [ ] **Step 1: Write failing source-extraction tests**

Create compact Java fixtures for `CityPlan`, `DungeonArchitect`, `DungeonLayout`,
`DungeonRoomChain`, `DungeonObjective`, `ThemePalette`, and
`DungeonTeleportHandler`. Assert extraction of city radius 300, 15 declared sites,
normal-floor dimensions 268 x 244, 5 x 4 grid, 20 route rooms, ten chapters, and
300-block floor spacing.

```python
class DungeonSourceExtractionTest(unittest.TestCase):
    def test_extracts_city_floor_and_progression_geometry(self):
        model = extract_dungeon_model(self.root, representative_floor=37)
        self.assertEqual(300, model.city.radius)
        self.assertEqual(15, len(model.city.sites))
        self.assertEqual((268, 244), model.floor.footprint)
        self.assertEqual((5, 4), model.floor.grid)
        self.assertEqual(20, len(model.floor.rooms))
        self.assertEqual(10, len(model.chapters))
        self.assertEqual(300, model.floor_spacing)
```

- [ ] **Step 2: Run tests and verify import failure**

Run: `python -m unittest scripts.tests.test_render_dungeon_map -v`

Expected: failure because `scripts.render_dungeon_map` does not exist.

- [ ] **Step 3: Implement focused Java parsing**

Implement immutable records for city sites, city geometry, floor rooms, cadence,
chapters, and the complete model. Use a restricted arithmetic evaluator that accepts
only integer constants, unary signs, addition, subtraction, multiplication, and
parentheses when evaluating `BlockPos` expressions. Parse site declarations and
fail if a referenced position method is missing.

Implement Java's 48-bit `Random` linear congruential generator so the representative
room profile matches `new Random(floor * 997L + 123L)` and `nextInt(3)` exactly.

- [ ] **Step 4: Add route and cadence tests**

Assert that route indices follow rows `0..4`, `9..5`, `10..14`, `19..15`; every
consecutive pair is cardinally adjacent; floor 5 is treasure; floor 10 is boss;
and floor 11 is combat.

- [ ] **Step 5: Run extractor tests**

Run: `python -m unittest scripts.tests.test_render_dungeon_map -v`

Expected: all extraction, route, Java-random, and cadence tests pass.

- [ ] **Step 6: Commit the extractor**

```bash
git add scripts/render_dungeon_map.py scripts/tests/test_render_dungeon_map.py
git commit -m "feat: extract dungeon architecture from sources"
```

### Task 2: Blueprint Renderer

**Files:**
- Modify: `scripts/render_dungeon_map.py`
- Modify: `scripts/tests/test_render_dungeon_map.py`
- Create: `docs/wiki/assets/screenshots/dungeon-architecture-map.png`

**Interfaces:**
- Consumes: `DungeonModel` from Task 1.
- Produces: `render_dungeon_map(root: Path, output: Path, representative_floor: int = 37) -> RenderReport`

- [ ] **Step 1: Write failing render-contract tests**

Render fixture data and assert a 3840 x 2160 PNG, 15 plotted city sites, 20 plotted
rooms, 100 progression cells, route continuity, and a report naming the chosen
representative floor.

- [ ] **Step 2: Run tests and verify missing renderer failure**

Run: `python -m unittest scripts.tests.test_render_dungeon_map -v`

Expected: failure because `render_dungeon_map` is absent.

- [ ] **Step 3: Implement the architectural canvas**

Build a dark navy canvas with procedural blueprint grid, vignette, panel borders,
gold headings, cyan geometry, and compact legends. Render:

- Floor 0 as a scaled circle with wall ring, gate, two ring roads, six avenues,
  district connectors, site-radius footprints, spawn, PvP arena radius, north arrow,
  and a 100-block scale bar.
- A normal floor as a 5 x 4 grid with 20 numbered rooms, route arrows, arrival,
  encounters, safehouse, objective, exit, dimensions, and a representative vertical
  profile derived from Java Random.
- A ten-chapter strip with 100 cells colored by combat, treasure, or boss role,
  chapter names parsed from `ThemePalette`, and the 300-block world-spacing note.

- [ ] **Step 4: Add CLI and validation report**

Expose `--root`, `--output`, and `--representative-floor`. Print JSON containing
city radius, site count, floor dimensions, room count, route continuity, profile
range, chapter count, progression cell count, floor spacing, and output size.

- [ ] **Step 5: Run tests and generate production PNG**

Run:

```powershell
python -m unittest scripts.tests.test_render_dungeon_map -v
python scripts/render_dungeon_map.py --output docs/wiki/assets/screenshots/dungeon-architecture-map.png
```

Expected: tests pass; report contains 15 sites, 20 rooms, 10 chapters, 100 cells,
300 spacing, and output size 3840 x 2160.

- [ ] **Step 6: Inspect at original resolution**

Verify every label is legible, no site or room is clipped, district labels do not
collide, the route direction is unambiguous, treasure and boss cadence is visible,
and the map clearly distinguishes exact geometry from representative profile data.
Adjust presentation constants only, rerun tests, and regenerate if needed.

- [ ] **Step 7: Commit renderer and image**

```bash
git add scripts/render_dungeon_map.py scripts/tests/test_render_dungeon_map.py docs/wiki/assets/screenshots/dungeon-architecture-map.png
git commit -m "feat: render dungeon architecture map"
```

### Task 3: Documentation And Publication

**Files:**
- Modify: `docs/wiki/dungeon.md`
- Modify: `docs/wiki/index.md`
- Modify: `MODRINTH_DESCRIPTION.md`

**Interfaces:**
- Consumes: generated 4K dungeon map.
- Produces: updated public wiki, project description, and Modrinth gallery.

- [ ] **Step 1: Add the generated map to documentation**

Place the map near the architecture explanation in `dungeon.md`, add a compact
home-page gallery card, and embed the raw GitHub image in the Modrinth description.
Describe the map as source-generated and distinguish Floor 0 from floors 1+.

- [ ] **Step 2: Validate references and documentation**

Run: `python -m mkdocs build --strict --site-dir <temporary-directory>`

Expected: exit code 0 and no missing-image warning.

- [ ] **Step 3: Commit and integrate**

```bash
git add docs/wiki/dungeon.md docs/wiki/index.md MODRINTH_DESCRIPTION.md
git commit -m "docs: publish dungeon architecture map"
```

- [ ] **Step 4: Push and deploy**

Push the integrated `neoforge-1.21.1` branch, deploy MkDocs to `wiki-public`, and
run `modrinthSyncBody` using the locally stored Modrinth token without printing it.

- [ ] **Step 5: Upload and verify the gallery image**

Upload the PNG to project `s6kZZpsn` with title `Dungeon Architecture Map` and an
accurate description. Verify HTTP 200 for the raw GitHub image and wiki page, and
verify the authenticated Modrinth project response contains exactly one gallery
entry with that title.
