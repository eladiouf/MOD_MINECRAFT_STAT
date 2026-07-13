#!/usr/bin/env python3
from __future__ import annotations

import ast
import re
from dataclasses import dataclass
from enum import Enum
from pathlib import Path


DUNGEON_PACKAGE = Path("src/main/java/tong/statmod/dungeon")


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
