# Magic Tree Render Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate and publish a faithful 3840 x 2160 image of the unified magic tree from the project's Puffish Skills JSON and bundled spell textures.

**Architecture:** A standalone Python/Pillow renderer reads the generated tree resources, indexes PNG assets inside `libs/*.jar`, resolves each texture resource location, and renders edges then nodes onto a 4K canvas. Pure layout and resource-resolution functions are unit-tested separately from the final binary render.

**Tech Stack:** Python 3, Pillow 12.2.0, `json`, `zipfile`, `unittest`, MkDocs.

## Global Constraints

- The PNG output is exactly 3840 x 2160.
- Node coordinates, sizes, root status, connections, and icon locations come from the checked-in JSON resources.
- The renderer searches project resources before bundled JARs.
- Missing icons are visible in the image and listed in the validation report.
- The existing background texture is used, darkened, and vignetted.
- No generative image model is used.

---

### Task 1: Resource Model And Resolver

**Files:**
- Create: `scripts/render_magic_tree.py`
- Create: `scripts/tests/test_render_magic_tree.py`

**Interfaces:**
- Produces: `load_tree(root: Path) -> TreeData`
- Produces: `build_texture_index(root: Path) -> dict[str, TextureSource]`
- Produces: `resolve_texture(resource: str, index: dict[str, TextureSource]) -> Image.Image | None`

- [ ] **Step 1: Write failing loader and resolver tests**

Create temporary JSON fixtures containing two nodes and one connection. Create a
temporary JAR containing `assets/example/textures/gui/icon.png`. Assert that
`load_tree` returns two nodes and one edge, and that `build_texture_index` exposes
the key `example:textures/gui/icon.png`.

```python
class ResourceLoadingTest(unittest.TestCase):
    def test_loads_nodes_and_edges(self):
        tree = load_tree(self.root)
        self.assertEqual(2, len(tree.nodes))
        self.assertEqual([("root", "fire.node")], tree.edges)

    def test_indexes_namespaced_texture_from_jar(self):
        index = build_texture_index(self.root)
        self.assertIn("example:textures/gui/icon.png", index)
```

- [ ] **Step 2: Run tests and verify they fail**

Run: `python -m unittest scripts.tests.test_render_magic_tree -v`

Expected: import failure because `scripts.render_magic_tree` does not exist.

- [ ] **Step 3: Implement immutable tree records and deterministic loading**

Implement `Node`, `TreeData`, and `TextureSource` dataclasses. Parse
`skills.json`, `definitions.json`, and `connections.json`; preserve JSON property
order and reject connections whose endpoints do not exist. Convert a texture
location such as `irons_spellbooks:textures/gui/spell_icons/firebolt.png` to the
JAR entry `assets/irons_spellbooks/textures/gui/spell_icons/firebolt.png`.

```python
@dataclass(frozen=True)
class Node:
    node_id: str
    x: float
    y: float
    size: float
    title: str
    texture: str | None
    root: bool

@dataclass(frozen=True)
class TreeData:
    nodes: tuple[Node, ...]
    edges: tuple[tuple[str, str], ...]
```

- [ ] **Step 4: Run resource tests**

Run: `python -m unittest scripts.tests.test_render_magic_tree -v`

Expected: both resource tests pass.

- [ ] **Step 5: Commit the resource layer**

```bash
git add scripts/render_magic_tree.py scripts/tests/test_render_magic_tree.py
git commit -m "feat: load magic tree render resources"
```

### Task 2: 4K Renderer And Validation

**Files:**
- Modify: `scripts/render_magic_tree.py`
- Modify: `scripts/tests/test_render_magic_tree.py`
- Create: `docs/wiki/assets/screenshots/magic-tree-render.png`

**Interfaces:**
- Consumes: `TreeData`, texture index, and Pillow images from Task 1.
- Produces: `compute_transform(tree: TreeData, canvas: tuple[int, int], margin: int) -> Transform`
- Produces: `render_tree(root: Path, output: Path) -> RenderReport`

- [ ] **Step 1: Write failing geometry tests**

Assert that transformed fixture nodes stay within a 140-pixel margin and preserve
their relative positions. Assert that a report records rendered nodes, edges,
found icons, and missing icons.

```python
def test_transform_fits_all_nodes_inside_margin(self):
    transform = compute_transform(self.tree, (3840, 2160), 140)
    points = [transform.apply(node.x, node.y) for node in self.tree.nodes]
    self.assertTrue(all(140 <= x <= 3700 for x, _ in points))
    self.assertTrue(all(140 <= y <= 2020 for _, y in points))
```

- [ ] **Step 2: Run geometry tests and verify they fail**

Run: `python -m unittest scripts.tests.test_render_magic_tree -v`

Expected: failure because `compute_transform` and `render_tree` are absent.

- [ ] **Step 3: Implement the renderer**

Scale and crop the existing background to 3840 x 2160, darken it, and apply a
radial vignette. Compute a uniform transform from source bounds. Draw each edge
with a school-colored blurred glow plus a narrow bright core. Draw each node as a
framed medallion using the real texture, with a neutral question-mark medallion
for unresolved textures. Draw the root with a larger gold frame. Add a title and
a compact legend for Fire, Ice/Water, Lightning/Air, Earth, Holy, Ender, Blood,
Evocation, and Eldritch in unused outer space.

Expose a CLI:

```python
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).parents[1])
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = render_tree(args.root.resolve(), args.output.resolve())
    print(json.dumps(asdict(report), indent=2))
```

- [ ] **Step 4: Run all renderer unit tests**

Run: `python -m unittest scripts.tests.test_render_magic_tree -v`

Expected: all tests pass.

- [ ] **Step 5: Generate the production PNG**

Run:

```powershell
python scripts/render_magic_tree.py --output docs/wiki/assets/screenshots/magic-tree-render.png
```

Expected: JSON report with `nodes_rendered: 246`, no invalid edges, and output size
`3840x2160`. Any missing textures are listed by resource location.

- [ ] **Step 6: Inspect and refine the image**

Open the generated PNG at original resolution. Verify that the complete tree fits,
icons remain recognizable, branch colors are distinct, labels do not cover nodes,
and there is no clipping. Adjust only renderer constants, rerun tests, and render
again if any check fails.

- [ ] **Step 7: Commit the renderer and generated image**

```bash
git add scripts/render_magic_tree.py scripts/tests/test_render_magic_tree.py docs/wiki/assets/screenshots/magic-tree-render.png
git commit -m "feat: render unified magic tree from resources"
```

### Task 3: Documentation Integration

**Files:**
- Modify: `docs/wiki/magic/index.md`
- Modify: `docs/wiki/index.md`
- Modify: `MODRINTH_DESCRIPTION.md`

**Interfaces:**
- Consumes: `docs/wiki/assets/screenshots/magic-tree-render.png` from Task 2.
- Produces: Wiki and Modrinth pages using the clean generated tree image.

- [ ] **Step 1: Replace screenshot references**

Replace the dedicated magic-tree screenshot reference with
`assets/screenshots/magic-tree-render.png` in wiki Markdown and with the raw GitHub
asset URL in `MODRINTH_DESCRIPTION.md`. Keep gameplay screenshots elsewhere.

- [ ] **Step 2: Correct the generated node count**

Where the pages describe the current tree as 249 nodes, use the generated resource
count of 246 nodes. Keep the school count at nine.

- [ ] **Step 3: Validate documentation**

Run: `python -m mkdocs build --strict`

Expected: exit code 0 with no missing image references.

- [ ] **Step 4: Commit documentation integration**

```bash
git add docs/wiki/magic/index.md docs/wiki/index.md MODRINTH_DESCRIPTION.md
git commit -m "docs: publish generated magic tree artwork"
```

### Task 4: Publication Verification

**Files:**
- No source changes expected.

**Interfaces:**
- Consumes: committed documentation and generated image.
- Produces: updated GitHub Pages and Modrinth description/gallery.

- [ ] **Step 1: Push committed source**

Run: `git push origin neoforge-1.21.1`

Expected: remote branch advances to the documentation integration commit.

- [ ] **Step 2: Deploy the wiki**

Run: `python -m mkdocs gh-deploy --force --remote-name wiki-public`

Expected: the `gh-pages` branch is updated successfully.

- [ ] **Step 3: Synchronize the Modrinth description**

Run: `.\gradlew.bat modrinthSyncBody`

Expected: successful Gradle task and updated project body.

- [ ] **Step 4: Upload the generated image to the Modrinth gallery**

Upload `magic-tree-render.png` with the title `Unified Magic Tree`, an accurate
description stating that it is rendered from the in-game tree resources, and set
it as the featured image if the API accepts it.

- [ ] **Step 5: Verify public assets**

Verify that the raw GitHub image URL returns HTTP 200, the wiki page references the
generated image, and the Modrinth gallery includes the titled 4K render.
