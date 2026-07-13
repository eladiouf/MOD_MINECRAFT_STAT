# Magic Tree Render Design

## Objective

Generate a clean 4K promotional image of the STAT Mod unified magic tree directly
from the resources shipped by the project and its bundled dependencies. The image
must represent the real tree rather than recreate it by hand or through generative
image synthesis.

## Sources Of Truth

- `skills.json` supplies node identifiers, coordinates, sizes, and the root node.
- `connections.json` supplies the edges between nodes.
- `definitions.json` supplies node titles and icon resource locations.
- `skill_tree_background.png` supplies the existing visual background.
- Mod JARs in `libs/` supply spell icon textures referenced by definitions.

The current generated resources contain 246 nodes across nine magic schools, plus
the common central root. The renderer must derive these values at runtime rather
than hard-code a documented count.

## Rendering

The renderer produces a 3840 x 2160 PNG. It preserves the source coordinate
geometry, scales the complete tree to the available canvas, and adds enough outer
margin to prevent edge nodes from being clipped.

Connections are drawn first with school-colored glow and a narrower bright core.
Nodes are drawn over the connections using their real icon textures. Node size is
derived from each definition's `size` value. The central root receives a distinct
frame so the origin of all schools remains obvious.

The existing background texture is scaled and darkened behind a restrained radial
vignette. A compact title and nine-school legend may occupy otherwise unused outer
space but must not cover tree nodes.

## Asset Resolution

Texture resource locations are resolved by namespace. The renderer searches the
project resources first, then the JARs under `libs/`. Missing icons receive a
visible neutral placeholder and are reported; they must not silently disappear.
Item-based icons, such as the root item, use the corresponding bundled Minecraft
texture when available and otherwise receive a dedicated root glyph.

## Outputs

- Reproducible renderer under `scripts/`.
- Final PNG under `docs/wiki/assets/screenshots/`.
- A short machine-readable or console validation summary containing node, edge,
  icon-found, and icon-missing counts.

The generated image will replace the in-game magic-tree screenshot in the wiki and
project description after visual verification.

## Validation

- Every source node is rendered exactly once.
- Every declared connection with valid endpoints is rendered exactly once.
- The output is exactly 3840 x 2160.
- No rendered node intersects the image boundary.
- Missing texture resources are listed explicitly.
- The PNG is inspected visually before publication.
