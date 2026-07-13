#!/usr/bin/env python3
from __future__ import annotations

import argparse
import ast
import json
import math
import re
from dataclasses import asdict, dataclass
from enum import Enum
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


DUNGEON_PACKAGE = Path("src/main/java/tong/statmod/dungeon")
CANVAS_SIZE = (3840, 2160)
COLORS = {
    "background": (3, 10, 18, 255),
    "panel": (6, 19, 30, 224),
    "panel_edge": (46, 101, 126, 210),
    "grid_minor": (18, 48, 63, 75),
    "grid_major": (29, 70, 88, 105),
    "text": (210, 225, 229, 255),
    "muted": (117, 157, 171, 255),
    "gold": (232, 190, 104, 255),
    "cyan": (59, 211, 224, 255),
    "combat": (72, 156, 202, 255),
    "treasure": (235, 170, 60, 255),
    "boss": (222, 73, 72, 255),
    "safe": (84, 197, 128, 255),
    "pvp": (244, 89, 85, 255),
}
CHAPTER_LABELS = {
    "DECHARNES": "LES DECHARNES",
    "FAUVES": "LES FAUVES",
    "TRIBUS": "LES TRIBUS",
    "LEGION": "LEGION NOIRE",
    "ABYSSES": "LES ABYSSES",
    "MAGES": "CERCLE DES MAGES",
    "MOISSON": "LA MOISSON",
    "FOURNAISE": "LA FOURNAISE",
    "GESTE": "LA GESTE",
    "NEANT": "LE NEANT",
}
CHAPTER_COLORS = (
    (154, 160, 158),
    (92, 170, 101),
    (197, 101, 73),
    (89, 103, 128),
    (61, 171, 190),
    (183, 92, 189),
    (201, 140, 57),
    (220, 80, 44),
    (144, 86, 173),
    (112, 86, 190),
)
SITE_LABELS = {
    "plaza": "Grande Place",
    "guild": "Guilde des Aventuriers",
    "human": "Quartier humain",
    "elven": "Quartier elfique",
    "dwarven": "Quartier nain",
    "beast": "Quartier hommes-betes",
    "market": "Grand marche",
    "artisans": "District des artisans",
    "arena": "Arene",
    "training": "Terrain d'entrainement",
    "sanctuary": "Sanctuaire",
    "gardens": "Jardins suspendus",
    "heroes": "Hall des Heros",
    "portals": "Portails",
    "gate": "Porte du Donjon",
}


class SourceExtractionError(ValueError):
    pass


@dataclass(frozen=True)
class Box:
    x: float
    y: float
    width: float
    height: float


@dataclass(frozen=True)
class CoordinateTransform:
    scale: float
    offset_x: float
    offset_y: float

    def apply(self, x: float, z: float) -> tuple[float, float]:
        return (x * self.scale + self.offset_x, z * self.scale + self.offset_y)


@dataclass(frozen=True)
class CitySite:
    site_id: str
    x: int
    z: int
    radius: int


@dataclass(frozen=True)
class CityModel:
    center_x: int
    center_z: int
    radius: int
    wall_inner: int
    plaza_radius: int
    inner_ring_radius: int
    outer_ring_radius: int
    avenue_half_width: float
    sites: tuple[CitySite, ...]
    spawn: tuple[int, int]
    arena_combat_radius: int


@dataclass(frozen=True)
class FloorRoom:
    index: int
    col: int
    row: int
    min_x: int
    max_x: int
    min_z: int
    max_z: int


@dataclass(frozen=True)
class FloorModel:
    footprint: tuple[int, int]
    grid: tuple[int, int]
    rooms: tuple[FloorRoom, ...]
    offsets: tuple[int, ...]
    representative_floor: int


@dataclass(frozen=True)
class RoleCadence:
    treasure_divisor: int
    boss_divisor: int


class FloorRole(Enum):
    COMBAT = "combat"
    TREASURE = "treasure"
    BOSS = "boss"


@dataclass(frozen=True)
class DungeonModel:
    city: CityModel
    floor: FloorModel
    cadence: RoleCadence
    chapters: tuple[str, ...]
    floor_spacing: int


@dataclass(frozen=True)
class RenderReport:
    city_radius: int
    city_sites: int
    floor_dimensions: tuple[int, int]
    rooms: int
    route_continuous: bool
    representative_floor: int
    profile_range: tuple[int, int]
    chapters: int
    progression_cells: int
    floor_spacing: int
    output_size: tuple[int, int]


class JavaRandom:
    _MULTIPLIER = 0x5DEECE66D
    _ADDEND = 0xB
    _MASK = (1 << 48) - 1

    def __init__(self, seed: int):
        self.seed = (seed ^ self._MULTIPLIER) & self._MASK

    def _next(self, bits: int) -> int:
        self.seed = (self.seed * self._MULTIPLIER + self._ADDEND) & self._MASK
        return self.seed >> (48 - bits)

    def next_int(self, bound: int) -> int:
        if bound <= 0:
            raise ValueError("bound must be positive")
        if bound & (bound - 1) == 0:
            return (bound * self._next(31)) >> 31
        while True:
            bits = self._next(31)
            value = bits % bound
            if bits - value + (bound - 1) < (1 << 31):
                return value


def _read(root: Path, relative: Path) -> str:
    path = root / DUNGEON_PACKAGE / relative
    if not path.exists():
        raise SourceExtractionError(f"Missing Java source: {path}")
    return path.read_text(encoding="utf-8")


def _required(pattern: str, text: str, label: str, flags: int = 0) -> re.Match[str]:
    match = re.search(pattern, text, flags)
    if match is None:
        raise SourceExtractionError(f"Missing source pattern: {label}")
    return match


def _eval_integer(expression: str, constants: dict[str, int]) -> int:
    normalized = re.sub(r"(?<=\d)L\b", "", expression.strip())
    tree = ast.parse(normalized, mode="eval")

    def evaluate(node: ast.AST) -> int:
        if isinstance(node, ast.Expression):
            return evaluate(node.body)
        if isinstance(node, ast.Constant) and isinstance(node.value, int):
            return node.value
        if isinstance(node, ast.Name) and node.id in constants:
            return constants[node.id]
        if isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            value = evaluate(node.operand)
            return value if isinstance(node.op, ast.UAdd) else -value
        if isinstance(node, ast.BinOp) and isinstance(
            node.op, (ast.Add, ast.Sub, ast.Mult, ast.FloorDiv)
        ):
            left, right = evaluate(node.left), evaluate(node.right)
            if isinstance(node.op, ast.Add):
                return left + right
            if isinstance(node.op, ast.Sub):
                return left - right
            if isinstance(node.op, ast.Mult):
                return left * right
            return left // right
        raise SourceExtractionError(f"Unsupported Java integer expression: {expression}")

    return evaluate(tree)


def _extract_city(text: str) -> CityModel:
    constants: dict[str, int] = {}
    for name, expression in re.findall(
        r"public static final int\s+([A-Z_]+)\s*=\s*([^;]+);", text
    ):
        constants[name] = _eval_integer(expression, constants)

    required_constants = (
        "CENTER_X",
        "CENTER_Z",
        "RADIUS",
        "WALL_INNER",
        "PLAZA_RADIUS",
        "INNER_RING_RADIUS",
        "OUTER_RING_RADIUS",
    )
    for name in required_constants:
        if name not in constants:
            raise SourceExtractionError(f"Missing CityPlan constant: {name}")

    avenue = float(
        _required(
            r"AVENUE_HALF_WIDTH\s*=\s*([0-9.]+)", text, "AVENUE_HALF_WIDTH"
        ).group(1)
    )
    positions: dict[str, tuple[int, int]] = {}
    method_pattern = re.compile(
        r"public static BlockPos\s+(\w+)\(\)\s*\{\s*return new BlockPos\("
        r"([^,]+),\s*([^,]+),\s*([^)]+)\);\s*\}"
    )
    for name, x_expr, _y_expr, z_expr in method_pattern.findall(text):
        positions[name] = (
            _eval_integer(x_expr, constants),
            _eval_integer(z_expr, constants),
        )

    sites: list[CitySite] = []
    site_pattern = re.compile(
        r'new CitySite\("([^"]+)",\s*(\w+)\(\),\s*([A-Z_]+|-?\d+)\)'
    )
    for site_id, position_name, radius_expr in site_pattern.findall(text):
        if position_name not in positions:
            raise SourceExtractionError(
                f"City site {site_id} references missing position {position_name}"
            )
        x, z = positions[position_name]
        sites.append(CitySite(site_id, x, z, _eval_integer(radius_expr, constants)))
    if not sites:
        raise SourceExtractionError("No CityPlan sites extracted")

    spawn_match = _required(
        r"public static BlockPos playerSpawn\(\)\s*\{\s*return new BlockPos\("
        r"([^,]+),\s*([^,]+),\s*([^)]+)\);",
        text,
        "CityPlan.playerSpawn",
    )
    spawn = (
        _eval_integer(spawn_match.group(1), constants),
        _eval_integer(spawn_match.group(3), constants),
    )
    arena_radius = int(
        _required(
            r"dx \* dx \+ dz \* dz <= (\d+)L \* \1L",
            text,
            "CityPlan arena combat radius",
        ).group(1)
    )
    return CityModel(
        center_x=constants["CENTER_X"],
        center_z=constants["CENTER_Z"],
        radius=constants["RADIUS"],
        wall_inner=constants["WALL_INNER"],
        plaza_radius=constants["PLAZA_RADIUS"],
        inner_ring_radius=constants["INNER_RING_RADIUS"],
        outer_ring_radius=constants["OUTER_RING_RADIUS"],
        avenue_half_width=avenue,
        sites=tuple(sites),
        spawn=spawn,
        arena_combat_radius=arena_radius,
    )


def _extract_floor(
    architect: str, layout: str, room_chain: str, representative_floor: int
) -> FloorModel:
    dimensions = _required(
        r"static final int HX\s*=\s*(\d+)\s*,\s*HZ\s*=\s*(\d+)",
        architect,
        "DungeonArchitect HX/HZ",
    )
    hx, hz = int(dimensions.group(1)), int(dimensions.group(2))
    cols = int(_required(r"COLS\s*=\s*(\d+)", layout, "DungeonLayout.COLS").group(1))
    rows = int(_required(r"ROWS\s*=\s*(\d+)", layout, "DungeonLayout.ROWS").group(1))
    cell_width = (2 * hx) // cols
    cell_depth = (2 * hz) // rows
    rooms: list[FloorRoom] = []
    for index in range(cols * rows):
        row = index // cols
        position = index % cols
        col = position if row % 2 == 0 else cols - 1 - position
        min_x = -hx + col * cell_width
        min_z = -hz + row * cell_depth
        rooms.append(
            FloorRoom(
                index=index,
                col=col,
                row=row,
                min_x=min_x,
                max_x=-hx + (col + 1) * cell_width - 1,
                min_z=min_z,
                max_z=-hz + (row + 1) * cell_depth - 1,
            )
        )

    seed_rule = _required(
        r"new Random\(floor \* (\d+)L \+ (\d+)L\)",
        room_chain,
        "DungeonRoomChain random seed",
    )
    random_bound = int(
        _required(r"rng\.nextInt\((\d+)\)\s*-\s*1", room_chain, "room offset random bound").group(1)
    )
    step_size = int(
        _required(r"offsets\[i - 1\] \+ step \* (\d+)", room_chain, "room offset step").group(1)
    )
    lower = int(
        _required(r"offsets\[i\] < (-?\d+)\) offsets\[i\] = (-?\d+)", room_chain, "lower offset clamp").group(2)
    )
    upper = int(
        _required(r"offsets\[i\] > (-?\d+)\)\s+offsets\[i\] = (-?\d+)", room_chain, "upper offset clamp").group(2)
    )
    rng = JavaRandom(representative_floor * int(seed_rule.group(1)) + int(seed_rule.group(2)))
    offsets = [0]
    for _index in range(1, cols * rows):
        step = rng.next_int(random_bound) - 1
        offsets.append(max(lower, min(upper, offsets[-1] + step * step_size)))

    return FloorModel(
        footprint=(2 * hx, 2 * hz),
        grid=(cols, rows),
        rooms=tuple(rooms),
        offsets=tuple(offsets),
        representative_floor=representative_floor,
    )


def _extract_cadence(text: str) -> RoleCadence:
    boss = int(
        _required(r"floor % (\d+) == 0\) return SLAY_BOSS", text, "boss cadence").group(1)
    )
    treasure = int(
        _required(r"floor % (\d+) == 0\) return LOOT_VAULT", text, "treasure cadence").group(1)
    )
    return RoleCadence(treasure_divisor=treasure, boss_divisor=boss)


def _extract_chapters(text: str) -> tuple[str, ...]:
    enum_start = text.find("public enum ThemePalette")
    if enum_start < 0:
        raise SourceExtractionError("Missing ThemePalette enum declaration")
    declaration = text[enum_start:]
    chapters = tuple(re.findall(r"^\s{4}([A-Z][A-Z_]+)\s*\{", declaration, re.MULTILINE))
    if not chapters:
        raise SourceExtractionError("No ThemePalette chapters extracted")
    return chapters


def floor_role(floor: int, cadence: RoleCadence) -> FloorRole:
    if floor > 0 and floor % cadence.boss_divisor == 0:
        return FloorRole.BOSS
    if floor > 0 and floor % cadence.treasure_divisor == 0:
        return FloorRole.TREASURE
    return FloorRole.COMBAT


def city_to_canvas(city: CityModel, box: Box) -> CoordinateTransform:
    scale = min(box.width, box.height) / (city.radius * 2)
    return CoordinateTransform(
        scale=scale,
        offset_x=box.x + box.width / 2 - city.center_x * scale,
        offset_y=box.y + box.height / 2 - city.center_z * scale,
    )


def extract_dungeon_model(root: Path, representative_floor: int = 37) -> DungeonModel:
    city = _extract_city(_read(root, Path("city/CityPlan.java")))
    floor = _extract_floor(
        _read(root, Path("DungeonArchitect.java")),
        _read(root, Path("DungeonLayout.java")),
        _read(root, Path("DungeonRoomChain.java")),
        representative_floor,
    )
    cadence = _extract_cadence(_read(root, Path("DungeonObjective.java")))
    chapters = _extract_chapters(_read(root, Path("ThemePalette.java")))
    spacing_text = _read(root, Path("DungeonTeleportHandler.java"))
    floor_spacing = int(
        _required(r"FLOOR_SPACING\s*=\s*(\d+)", spacing_text, "FLOOR_SPACING").group(1)
    )
    return DungeonModel(city, floor, cadence, chapters, floor_spacing)


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = (
        Path("C:/Windows/Fonts/bahnschrift.ttf"),
        Path("C:/Windows/Fonts/georgiab.ttf" if bold else "C:/Windows/Fonts/georgia.ttf"),
    )
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


def _blueprint_background() -> Image.Image:
    image = Image.new("RGBA", CANVAS_SIZE, COLORS["background"])
    draw = ImageDraw.Draw(image)
    for x in range(0, CANVAS_SIZE[0], 40):
        color = COLORS["grid_major"] if x % 200 == 0 else COLORS["grid_minor"]
        draw.line((x, 0, x, CANVAS_SIZE[1]), fill=color, width=1)
    for y in range(0, CANVAS_SIZE[1], 40):
        color = COLORS["grid_major"] if y % 200 == 0 else COLORS["grid_minor"]
        draw.line((0, y, CANVAS_SIZE[0], y), fill=color, width=1)
    vignette = Image.radial_gradient("L").resize(CANVAS_SIZE, Image.Resampling.BICUBIC)
    vignette = vignette.point(lambda value: int(value * 0.64))
    return Image.composite(Image.new("RGBA", CANVAS_SIZE, (0, 2, 7, 255)), image, vignette)


def _panel(draw: ImageDraw.ImageDraw, box: Box, title: str, subtitle: str) -> None:
    bounds = (box.x, box.y, box.x + box.width, box.y + box.height)
    draw.rounded_rectangle(bounds, radius=22, fill=COLORS["panel"], outline=COLORS["panel_edge"], width=2)
    draw.text((box.x + 28, box.y + 20), title, font=_font(33, True), fill=COLORS["gold"])
    draw.text((box.x + 30, box.y + 62), subtitle, font=_font(18), fill=COLORS["muted"])
    draw.line((box.x + 28, box.y + 94, box.x + box.width - 28, box.y + 94), fill=COLORS["panel_edge"], width=2)


def _ellipse_bounds(center: tuple[float, float], radius: float) -> tuple[float, float, float, float]:
    return (
        center[0] - radius,
        center[1] - radius,
        center[0] + radius,
        center[1] + radius,
    )


def _draw_dashed_circle(
    draw: ImageDraw.ImageDraw,
    center: tuple[float, float],
    radius: float,
    color: tuple[int, int, int, int],
    width: int = 3,
) -> None:
    bounds = _ellipse_bounds(center, radius)
    for start in range(0, 360, 18):
        draw.arc(bounds, start=start, end=start + 10, fill=color, width=width)


def _draw_city(canvas: Image.Image, model: DungeonModel, panel: Box) -> None:
    draw = ImageDraw.Draw(canvas)
    _panel(
        draw,
        panel,
        "ETAGE 0  |  CITE DES AVENTURIERS",
        "Plan exact de CityPlan.java  |  diametre 600 blocs  |  zone sure hors arene",
    )
    map_box = Box(panel.x + 38, panel.y + 126, 1180, 1180)
    transform = city_to_canvas(model.city, map_box)
    city = model.city
    center = transform.apply(city.center_x, city.center_z)
    scale = transform.scale

    glow = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse(_ellipse_bounds(center, city.radius * scale), outline=(46, 190, 211, 150), width=12)
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(12)))
    draw = ImageDraw.Draw(canvas)

    draw.ellipse(
        _ellipse_bounds(center, city.radius * scale),
        fill=(8, 30, 39, 185),
        outline=COLORS["cyan"],
        width=4,
    )
    draw.ellipse(
        _ellipse_bounds(center, city.wall_inner * scale),
        outline=(85, 155, 170, 240),
        width=3,
    )
    for ring_radius in (city.inner_ring_radius, city.outer_ring_radius):
        _draw_dashed_circle(draw, center, ring_radius * scale, (76, 145, 160, 210), 3)

    for index in range(6):
        angle = math.radians(90 + index * 60)
        start = (
            center[0] + math.cos(angle) * city.plaza_radius * scale,
            center[1] + math.sin(angle) * city.plaza_radius * scale,
        )
        end = (
            center[0] + math.cos(angle) * city.wall_inner * scale,
            center[1] + math.sin(angle) * city.wall_inner * scale,
        )
        draw.line((start, end), fill=(72, 151, 168, 220), width=max(3, round(city.avenue_half_width * scale * 2)))

    for site in city.sites:
        if site.site_id in ("plaza", "gate"):
            continue
        endpoint = transform.apply(site.x, site.z)
        connector_start = (
            center[0] + (endpoint[0] - center[0]) * 0.18,
            center[1] + (endpoint[1] - center[1]) * 0.18,
        )
        connector_end = (
            center[0] + (endpoint[0] - center[0]) * 0.88,
            center[1] + (endpoint[1] - center[1]) * 0.88,
        )
        draw.line((connector_start, connector_end), fill=(46, 105, 121, 180), width=5)

    site_colors = (
        (218, 181, 94), (67, 174, 206), (70, 132, 203), (88, 177, 112),
        (211, 112, 57), (177, 100, 65), (234, 167, 65), (91, 174, 173),
        (218, 76, 73), (103, 160, 215), (121, 201, 142), (112, 185, 131),
        (205, 166, 93), (154, 93, 210), (195, 94, 209),
    )
    for number, (site, rgb) in enumerate(zip(city.sites, site_colors), start=1):
        point = transform.apply(site.x, site.z)
        radius = max(9, site.radius * scale)
        if site.radius > 0:
            draw.ellipse(_ellipse_bounds(point, radius), fill=rgb + (48,), outline=rgb + (235,), width=3)
        badge_radius = 15 if site.site_id != "plaza" else 19
        draw.ellipse(_ellipse_bounds(point, badge_radius), fill=(4, 14, 23, 255), outline=rgb + (255,), width=3)
        draw.text(point, str(number), font=_font(18, True), fill=(245, 239, 220, 255), anchor="mm")

    arena = next(site for site in city.sites if site.site_id == "arena")
    arena_center = transform.apply(arena.x, arena.z)
    _draw_dashed_circle(draw, arena_center, city.arena_combat_radius * scale, COLORS["pvp"], 4)
    spawn = transform.apply(*city.spawn)
    draw.polygon(
        ((spawn[0], spawn[1] - 13), (spawn[0] - 11, spawn[1] + 10), (spawn[0] + 11, spawn[1] + 10)),
        fill=COLORS["cyan"],
    )

    gate_y = center[1] + city.wall_inner * scale
    gate_half_width = 7 * scale
    draw.rectangle((center[0] - gate_half_width, gate_y - 24, center[0] + gate_half_width, gate_y + 16), fill=(5, 18, 28, 255))
    draw.line((center[0] - gate_half_width, gate_y, center[0] + gate_half_width, gate_y), fill=COLORS["gold"], width=5)
    gate = next(site for site in city.sites if site.site_id == "gate")
    gate_point = transform.apply(gate.x, gate.z)
    gate_color = site_colors[-1]
    draw.ellipse(_ellipse_bounds(gate_point, 15), fill=(4, 14, 23, 255), outline=gate_color + (255,), width=3)
    draw.text(gate_point, "15", font=_font(18, True), fill=(245, 239, 220, 255), anchor="mm")

    # North arrow and source-space scale.
    north_x, north_y = map_box.x + 82, map_box.y + 95
    draw.line((north_x, north_y + 50, north_x, north_y - 25), fill=COLORS["text"], width=4)
    draw.polygon(((north_x, north_y - 40), (north_x - 10, north_y - 18), (north_x + 10, north_y - 18)), fill=COLORS["gold"])
    draw.text((north_x, north_y - 72), "N", font=_font(23, True), fill=COLORS["gold"], anchor="mm")
    bar_x, bar_y = map_box.x + 45, map_box.y + map_box.height - 38
    draw.line((bar_x, bar_y, bar_x + 100 * scale, bar_y), fill=COLORS["text"], width=5)
    draw.line((bar_x, bar_y - 8, bar_x, bar_y + 8), fill=COLORS["text"], width=3)
    draw.line((bar_x + 100 * scale, bar_y - 8, bar_x + 100 * scale, bar_y + 8), fill=COLORS["text"], width=3)
    draw.text((bar_x + 100 * scale / 2, bar_y - 32), "100 BLOCS", font=_font(18, True), fill=COLORS["text"], anchor="mm")

    legend_x = panel.x + 1260
    legend_y = panel.y + 145
    draw.text((legend_x, legend_y), "ZONES REELLES", font=_font(25, True), fill=COLORS["gold"])
    for number, (site, rgb) in enumerate(zip(city.sites, site_colors), start=1):
        y = legend_y + 42 + (number - 1) * 55
        draw.ellipse((legend_x, y + 2, legend_x + 24, y + 26), fill=(4, 14, 23, 255), outline=rgb + (255,), width=2)
        draw.text((legend_x + 12, y + 14), str(number), font=_font(14, True), fill=COLORS["text"], anchor="mm")
        draw.text((legend_x + 36, y), SITE_LABELS[site.site_id], font=_font(19), fill=COLORS["text"])
    legend_bottom = legend_y + 42 + len(city.sites) * 55
    draw.line((legend_x, legend_bottom, panel.x + panel.width - 30, legend_bottom), fill=COLORS["panel_edge"], width=2)
    draw.text((legend_x, legend_bottom + 20), "Triangle cyan : apparition", font=_font(17), fill=COLORS["cyan"])
    draw.text((legend_x, legend_bottom + 47), "Pointilles rouges : zone PvP", font=_font(17), fill=COLORS["pvp"])


def _route_continuous(rooms: tuple[FloorRoom, ...]) -> bool:
    return all(
        abs(left.col - right.col) + abs(left.row - right.row) == 1
        for left, right in zip(rooms, rooms[1:])
    )


def _draw_floor(canvas: Image.Image, model: DungeonModel, panel: Box) -> None:
    draw = ImageDraw.Draw(canvas)
    floor = model.floor
    _panel(
        draw,
        panel,
        "ETAGE NORMAL  |  PARCOURS REEL",
        f"DungeonLayout.java  |  {floor.footprint[0]} x {floor.footprint[1]} blocs  |  grille {floor.grid[0]} x {floor.grid[1]}",
    )
    plan_box = Box(panel.x + 38, panel.y + 132, 1080, 790)
    cols, rows = floor.grid
    cell_w, cell_h = plan_box.width / cols, plan_box.height / rows
    centers: dict[int, tuple[float, float]] = {}
    for room in floor.rooms:
        x0 = plan_box.x + room.col * cell_w
        y0 = plan_box.y + room.row * cell_h
        centers[room.index] = (x0 + cell_w / 2, y0 + cell_h / 2)
        if room.index == 0:
            color = COLORS["cyan"]
            label = "ARRIVEE"
        elif room.index == len(floor.rooms) // 2:
            color = COLORS["safe"]
            label = "REFUGE"
        elif room.index == len(floor.rooms) - 1:
            color = COLORS["treasure"]
            label = "OBJECTIF"
        else:
            color = COLORS["combat"]
            label = "RENCONTRE"
        draw.rounded_rectangle(
            (x0 + 6, y0 + 6, x0 + cell_w - 6, y0 + cell_h - 6),
            radius=12,
            fill=(color[0], color[1], color[2], 30),
            outline=(color[0], color[1], color[2], 175),
            width=2,
        )
        draw.text((x0 + 17, y0 + 14), f"{room.index + 1:02d}", font=_font(22, True), fill=color)
        draw.text((x0 + cell_w / 2, y0 + cell_h - 31), label, font=_font(14, True), fill=COLORS["muted"], anchor="mm")

    route_glow = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    route_draw = ImageDraw.Draw(route_glow)
    points = [centers[room.index] for room in floor.rooms]
    route_draw.line(points, fill=(54, 216, 231, 190), width=15, joint="curve")
    canvas.alpha_composite(route_glow.filter(ImageFilter.GaussianBlur(10)))
    draw = ImageDraw.Draw(canvas)
    draw.line(points, fill=COLORS["cyan"], width=4, joint="curve")
    for room in floor.rooms:
        point = centers[room.index]
        draw.ellipse(_ellipse_bounds(point, 10), fill=(3, 13, 22, 255), outline=COLORS["cyan"], width=3)
    for left, right in zip(points, points[1:]):
        dx, dy = right[0] - left[0], right[1] - left[1]
        length = math.hypot(dx, dy)
        ux, uy = dx / length, dy / length
        tip = (left[0] + dx * 0.58, left[1] + dy * 0.58)
        side = 8
        back = 15
        draw.polygon(
            (
                tip,
                (tip[0] - ux * back - uy * side, tip[1] - uy * back + ux * side),
                (tip[0] - ux * back + uy * side, tip[1] - uy * back - ux * side),
            ),
            fill=COLORS["cyan"],
        )

    info_x = panel.x + 1165
    info_y = panel.y + 145
    draw.text((info_x, info_y), "LECTURE DU PLAN", font=_font(25, True), fill=COLORS["gold"])
    legend = (
        (COLORS["cyan"], "Salle 01 : sanctuaire d'arrivee"),
        (COLORS["combat"], "18 secteurs intermediaires"),
        (COLORS["safe"], "Salle 11 : refuge de combat"),
        (COLORS["treasure"], "Salle 20 : objectif et sortie"),
    )
    for index, (color, text) in enumerate(legend):
        y = info_y + 48 + index * 48
        draw.rectangle((info_x, y + 3, info_x + 20, y + 23), fill=color)
        draw.text((info_x + 34, y), text, font=_font(18), fill=COLORS["text"])
    role_y = info_y + 270
    draw.text((info_x, role_y), "ROLES D'ETAGE", font=_font(23, True), fill=COLORS["gold"])
    role_lines = (
        (COLORS["combat"], "Combat : eliminer les rencontres"),
        (COLORS["treasure"], "Multiple de 5 : piller la chambre forte"),
        (COLORS["boss"], "Multiple de 10 : vaincre le boss"),
    )
    for index, (color, text) in enumerate(role_lines):
        y = role_y + 44 + index * 48
        draw.ellipse((info_x, y + 4, info_x + 19, y + 23), fill=color)
        draw.text((info_x + 32, y), text, font=_font(18), fill=COLORS["text"])
    metrics_y = role_y + 228
    draw.line((info_x, metrics_y, panel.x + panel.width - 34, metrics_y), fill=COLORS["panel_edge"], width=2)
    draw.text((info_x, metrics_y + 20), f"20 salles  |  route continue : {'OUI' if _route_continuous(floor.rooms) else 'NON'}", font=_font(18, True), fill=COLORS["text"])
    draw.text((info_x, metrics_y + 52), "Etage boss : arene ouverte dediee", font=_font(17), fill=COLORS["muted"])

    # Representative vertical profile.
    chart = Box(panel.x + 38, panel.y + 965, panel.width - 76, 305)
    draw.rounded_rectangle(
        (chart.x, chart.y, chart.x + chart.width, chart.y + chart.height),
        radius=14,
        fill=(3, 14, 23, 220),
        outline=COLORS["panel_edge"],
        width=2,
    )
    draw.text((chart.x + 22, chart.y + 17), f"PROFIL VERTICAL REPRESENTATIF  |  ETAGE {floor.representative_floor}", font=_font(21, True), fill=COLORS["gold"])
    graph_left, graph_right = chart.x + 70, chart.x + chart.width - 28
    graph_top, graph_bottom = chart.y + 70, chart.y + chart.height - 42
    for offset in (-6, -3, 0, 3, 6, 9):
        y = graph_bottom - (offset + 6) / 15 * (graph_bottom - graph_top)
        draw.line((graph_left, y, graph_right, y), fill=COLORS["grid_major"], width=1)
        draw.text((graph_left - 14, y), f"{offset:+d}", font=_font(15), fill=COLORS["muted"], anchor="rm")
    profile_points = []
    for index, offset in enumerate(floor.offsets):
        x = graph_left + index / (len(floor.offsets) - 1) * (graph_right - graph_left)
        y = graph_bottom - (offset + 6) / 15 * (graph_bottom - graph_top)
        profile_points.append((x, y))
    draw.line(profile_points, fill=COLORS["cyan"], width=4, joint="curve")
    for index, point in enumerate(profile_points):
        draw.ellipse(_ellipse_bounds(point, 5), fill=COLORS["cyan"])
        if index % 2 == 0:
            draw.text((point[0], graph_bottom + 13), str(index + 1), font=_font(13), fill=COLORS["muted"], anchor="ma")
    draw.text((graph_right, chart.y + 20), "Plage autorisee : -6 a +9 blocs", font=_font(17), fill=COLORS["muted"], anchor="ra")


def _draw_progression(canvas: Image.Image, model: DungeonModel, panel: Box) -> None:
    draw = ImageDraw.Draw(canvas)
    _panel(
        draw,
        panel,
        "PROGRESSION  |  ETAGES 1 A 100",
        f"10 arcs de 10 etages  |  tresor au x5  |  boss au x10  |  ancres espacees de {model.floor_spacing} blocs",
    )
    content_x = panel.x + 28
    content_y = panel.y + 122
    gap = 12
    chapter_width = (panel.width - 56 - gap * 9) / 10
    for chapter_index, (chapter, rgb) in enumerate(zip(model.chapters, CHAPTER_COLORS)):
        x = content_x + chapter_index * (chapter_width + gap)
        draw.rounded_rectangle(
            (x, content_y, x + chapter_width, content_y + 235),
            radius=11,
            fill=(rgb[0], rgb[1], rgb[2], 20),
            outline=rgb + (170,),
            width=2,
        )
        draw.text((x + chapter_width / 2, content_y + 20), f"{chapter_index + 1:02d}", font=_font(17, True), fill=rgb + (255,), anchor="mm")
        draw.text((x + chapter_width / 2, content_y + 52), CHAPTER_LABELS.get(chapter, chapter), font=_font(16, True), fill=COLORS["text"], anchor="mm")
        cell_gap = 4
        cell_width = (chapter_width - 22 - cell_gap * 4) / 5
        for relative in range(10):
            floor_number = chapter_index * 10 + relative + 1
            role = floor_role(floor_number, model.cadence)
            role_color = COLORS[role.value]
            row, col = divmod(relative, 5)
            cell_x = x + 11 + col * (cell_width + cell_gap)
            cell_y = content_y + 84 + row * 57
            draw.rounded_rectangle(
                (cell_x, cell_y, cell_x + cell_width, cell_y + 45),
                radius=6,
                fill=(role_color[0], role_color[1], role_color[2], 80),
                outline=role_color,
                width=2,
            )
            draw.text((cell_x + cell_width / 2, cell_y + 23), str(floor_number), font=_font(15, True), fill=(245, 241, 225, 255), anchor="mm")

    legend_y = content_y + 264
    legend_items = (
        (COLORS["combat"], "COMBAT"),
        (COLORS["treasure"], "TRESOR"),
        (COLORS["boss"], "BOSS"),
    )
    legend_x = panel.x + 34
    for color, label in legend_items:
        draw.rectangle((legend_x, legend_y, legend_x + 22, legend_y + 22), fill=color)
        draw.text((legend_x + 33, legend_y - 1), label, font=_font(17, True), fill=COLORS["text"])
        legend_x += 155
    draw.text(
        (panel.x + panel.width - 34, legend_y),
        "Au-dela de 100 : les arcs recommencent selon ThemePalette.forFloor",
        font=_font(17),
        fill=COLORS["muted"],
        anchor="ra",
    )


def render_dungeon_map(
    root: Path, output: Path, representative_floor: int = 37
) -> RenderReport:
    model = extract_dungeon_model(root, representative_floor)
    canvas = _blueprint_background()
    draw = ImageDraw.Draw(canvas)
    draw.text((72, 47), "TRIAL DUNGEON  |  PLAN ARCHITECTURAL", font=_font(53, True), fill=COLORS["gold"])
    draw.text(
        (75, 112),
        "GEOMETRIE EXTRAITE DIRECTEMENT DES SOURCES JAVA DU MOD",
        font=_font(22),
        fill=COLORS["cyan"],
    )
    draw.text((3765, 72), "STAT MOD", font=_font(27, True), fill=COLORS["muted"], anchor="ra")
    draw.line((72, 151, 3768, 151), fill=COLORS["gold"], width=2)

    _draw_city(canvas, model, Box(70, 180, 1750, 1430))
    _draw_floor(canvas, model, Box(1850, 180, 1920, 1430))
    _draw_progression(canvas, model, Box(70, 1640, 3700, 460))

    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(output, format="PNG", optimize=True)
    return RenderReport(
        city_radius=model.city.radius,
        city_sites=len(model.city.sites),
        floor_dimensions=model.floor.footprint,
        rooms=len(model.floor.rooms),
        route_continuous=_route_continuous(model.floor.rooms),
        representative_floor=representative_floor,
        profile_range=(min(model.floor.offsets), max(model.floor.offsets)),
        chapters=len(model.chapters),
        progression_cells=len(model.chapters) * 10,
        floor_spacing=model.floor_spacing,
        output_size=CANVAS_SIZE,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Render the STAT Mod dungeon architecture map")
    parser.add_argument("--root", type=Path, default=Path(__file__).parents[1])
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--representative-floor", type=int, default=37)
    args = parser.parse_args()
    report = render_dungeon_map(
        args.root.resolve(), args.output.resolve(), args.representative_floor
    )
    print(json.dumps(asdict(report), indent=2))


if __name__ == "__main__":
    main()
