# Dungeon Architecture Map Design

## Objective

Generate a clean 3840 x 2160 architectural overview of the STAT Mod dungeon from
the geometry and progression rules checked into the Java source. The image is a
technical illustrated map, not a fabricated Minecraft screenshot and not a
block-by-block world export.

## Sources Of Truth

- `CityPlan.java` supplies Floor 0 dimensions, roads, site centers, site radii,
  spawn, gate, and arena combat bounds.
- `DungeonArchitect.java` supplies the normal-floor half dimensions and floor
  roles.
- `DungeonLayout.java` supplies the 5 x 4 grid, 20 rooms, room bounds, and
  serpentine route.
- `DungeonRoomChain.java` supplies deterministic room elevation rules and the
  refuge/terminal-room behavior.
- `DungeonObjective.java` supplies combat, treasure, and boss objective cadence.
- `ThemePalette.java` supplies the ten chapter identifiers.
- `DungeonTeleportHandler.java` supplies the 300-block floor spacing.

The renderer must parse these source constants and rules. Values that determine
geometry or labels must not be duplicated silently in the rendering code.

## Canvas And Visual Direction

The output is a 3840 x 2160 PNG in a dark architectural-blueprint style. Fine
grid lines, restrained texture, warm gold headings, cyan construction lines, and
role-specific accent colors create hierarchy without obscuring geometry.

The canvas contains three coordinated sections:

1. Floor 0 city plan occupies the left side.
2. A normal floor plan and vertical route profile occupy the upper-right side.
3. The 1-100 progression strip spans the lower portion of the canvas.

All labels are in French. A north marker, scale indication, coordinate note, and
legend identify orientation and meaning.

## Floor 0 Plan

The city is rendered as a top-down 600-block-diameter circle. It includes the
outer wall, gate opening, 45-block-radius central plaza, ring roads at radii 100
and 205, six radial avenues, and connectors from the center to district sites.

Every site returned by `CityPlan.sites()` is plotted at its actual relative
coordinate and radius:

- Grande Place
- Guilde
- Quartier humain
- Quartier elfique
- Quartier nain
- Quartier hommes-betes
- Grand marche
- District des artisans
- Arene
- Terrain d'entrainement
- Sanctuaire
- Jardins suspendus
- Hall des Heros
- Portails
- Porte du Donjon

The arena's 26-block combat radius is overlaid separately to show the only PvP
combat zone. Player spawn and the dungeon gate receive distinct symbols.

## Normal Floor Plan

The normal floor is rendered from `DungeonLayout` as a 268 x 244 block footprint,
divided into five columns and four rows. The 20 rooms are numbered in actual
boustrophedon order and connected through their declared exit directions.

The first room is marked as the arrival sanctuary. The middle room is marked as
the safehouse on combat floors. The last room is marked as the terminal objective
and exit. Intermediate rooms are marked as encounter sectors.

A profile below the top-down plan demonstrates deterministic verticality using a
representative floor seed. It displays every room offset and the allowed vertical
range of -6 to +9 blocks. The representative floor number is named in the image;
the diagram must not imply that every floor has identical elevations.

The plan also explains the three actual floor roles:

- Combat: clear the encounter progression.
- Treasure on multiples of five that are not multiples of ten: loot the vault.
- Boss on multiples of ten: defeat the boss in a dedicated open arena.

## Floors 1-100 Progression

The progression strip contains ten chapters of ten floors, named from
`ThemePalette`:

1. Les Decharnes
2. Les Fauves
3. Les Tribus
4. La Legion Noire
5. Les Abysses
6. Le Cercle des Mages
7. La Moisson
8. La Fournaise
9. La Geste
10. Le Neant

Each chapter displays ten cells. Regular cells represent combat, the fifth cell
represents treasure, and the tenth represents a boss. A side note states that
floor anchors are separated by 300 blocks in the world.

## Extraction And Validation

A standalone Python/Pillow renderer under `scripts/` parses the Java sources with
targeted regular expressions. Parsing failures stop generation with a clear error
that names the missing constant or method pattern.

The renderer emits a report containing:

- city radius and number of sites;
- normal-floor dimensions, grid size, and room count;
- route continuity result;
- minimum and maximum representative offsets;
- number of chapters and progression cells;
- output dimensions.

Automated tests verify source extraction, city coordinate transforms, route order,
role cadence, output dimensions, and that every declared city site is represented.
The final PNG is inspected at original resolution before documentation publication.

## Outputs

- `scripts/render_dungeon_map.py`
- `scripts/tests/test_render_dungeon_map.py`
- `docs/wiki/assets/screenshots/dungeon-architecture-map.png`
- Updated dungeon and project overview pages using the generated map

The generated image is uploaded to the Modrinth gallery after source publication
and wiki validation.
