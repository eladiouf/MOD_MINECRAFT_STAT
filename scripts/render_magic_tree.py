#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import zipfile
from dataclasses import asdict, dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps


CATEGORY_PATH = Path(
    "src/main/resources/data/statmod/puffish_skills/categories/statmod_magic"
)
BACKGROUND_PATH = Path(
    "src/main/resources/assets/statmod/textures/gui/skill_tree_background.png"
)
CANVAS_SIZE = (3840, 2160)
SCHOOL_COLORS = {
    "fire": (244, 92, 42),
    "water": (72, 177, 255),
    "air": (247, 211, 83),
    "earth": (107, 201, 105),
    "holy": (255, 235, 166),
    "ender": (178, 98, 255),
    "blood": (211, 45, 72),
    "evocation": (74, 224, 211),
    "eldritch": (197, 74, 220),
    "common": (255, 205, 103),
}
SCHOOL_LABELS = {
    "fire": "FEU",
    "water": "EAU / GLACE",
    "air": "AIR / FOUDRE",
    "earth": "TERRE",
    "holy": "SACRE",
    "ender": "ENDER",
    "blood": "SANG",
    "evocation": "EVOCATION",
    "eldritch": "OCCULTE",
}


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


@dataclass(frozen=True)
class TextureSource:
    archive: Path | None
    path: Path | None
    member: str | None


@dataclass(frozen=True)
class Transform:
    scale: float
    offset_x: float
    offset_y: float

    def apply(self, x: float, y: float) -> tuple[float, float]:
        return (x * self.scale + self.offset_x, y * self.scale + self.offset_y)


@dataclass(frozen=True)
class RenderReport:
    nodes_rendered: int
    edges_rendered: int
    icons_found: int
    missing_textures: tuple[str, ...]
    output_size: tuple[int, int]


def _read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as source:
        return json.load(source)


def load_tree(root: Path) -> TreeData:
    category = root / CATEGORY_PATH
    skills = _read_json(category / "skills.json")
    definitions = _read_json(category / "definitions.json")
    connections = _read_json(category / "connections.json")

    nodes: list[Node] = []
    for node_id, skill in skills.items():
        definition = definitions[skill["definition"]]
        icon = definition.get("icon", {})
        texture = None
        if icon.get("type") == "texture":
            texture = icon.get("data", {}).get("texture")
        nodes.append(
            Node(
                node_id=node_id,
                x=float(skill["x"]),
                y=float(skill["y"]),
                size=float(definition.get("size", 1.0)),
                title=str(definition.get("title", node_id)),
                texture=texture,
                root=bool(skill.get("root", False)),
            )
        )

    node_ids = {node.node_id for node in nodes}
    edges: list[tuple[str, str]] = []
    for left, right in connections.get("normal", {}).get("bidirectional", []):
        if left not in node_ids or right not in node_ids:
            raise ValueError(f"Connection references unknown node: {left} -> {right}")
        edges.append((left, right))
    return TreeData(tuple(nodes), tuple(edges))


def _resource_key(member: str) -> str | None:
    normalized = member.replace("\\", "/")
    if not normalized.startswith("assets/") or not normalized.endswith(".png"):
        return None
    parts = normalized.split("/", 2)
    if len(parts) != 3:
        return None
    return f"{parts[1]}:{parts[2]}"


def build_texture_index(root: Path) -> dict[str, TextureSource]:
    index: dict[str, TextureSource] = {}
    resources = root / "src/main/resources/assets"
    if resources.exists():
        for texture in sorted(resources.rglob("*.png")):
            relative = texture.relative_to(root / "src/main/resources").as_posix()
            key = _resource_key(relative)
            if key:
                index[key] = TextureSource(None, texture, None)

    for jar in sorted((root / "libs").glob("*.jar")):
        try:
            with zipfile.ZipFile(jar) as archive:
                for member in archive.namelist():
                    key = _resource_key(member)
                    if key and key not in index:
                        index[key] = TextureSource(jar, None, member)
        except zipfile.BadZipFile:
            continue
    return index


def resolve_texture(
    resource: str, index: dict[str, TextureSource]
) -> Image.Image | None:
    source = index.get(resource)
    if source is None:
        return None
    if source.path is not None:
        with Image.open(source.path) as image:
            return image.convert("RGBA")
    if source.archive is not None and source.member is not None:
        with zipfile.ZipFile(source.archive) as archive:
            with Image.open(io.BytesIO(archive.read(source.member))) as image:
                return image.convert("RGBA")
    return None


def compute_transform(
    tree: TreeData, canvas: tuple[int, int], margin: int
) -> Transform:
    if not tree.nodes:
        raise ValueError("Cannot transform an empty magic tree")
    min_x = min(node.x for node in tree.nodes)
    max_x = max(node.x for node in tree.nodes)
    min_y = min(node.y for node in tree.nodes)
    max_y = max(node.y for node in tree.nodes)
    source_width = max(1.0, max_x - min_x)
    source_height = max(1.0, max_y - min_y)
    available_width = canvas[0] - margin * 2
    available_height = canvas[1] - margin * 2
    scale = min(available_width / source_width, available_height / source_height)
    rendered_width = source_width * scale
    rendered_height = source_height * scale
    offset_x = (canvas[0] - rendered_width) / 2.0 - min_x * scale
    offset_y = (canvas[1] - rendered_height) / 2.0 - min_y * scale
    return Transform(scale, offset_x, offset_y)


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = (
        Path("C:/Windows/Fonts/bahnschrift.ttf"),
        Path("C:/Windows/Fonts/georgiab.ttf" if bold else "C:/Windows/Fonts/georgia.ttf"),
    )
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


def _prepare_background(root: Path, size: tuple[int, int]) -> Image.Image:
    source = root / BACKGROUND_PATH
    if source.exists():
        with Image.open(source) as raw:
            background = ImageOps.fit(raw.convert("RGB"), size, Image.Resampling.LANCZOS)
    else:
        background = Image.new("RGB", size, (4, 9, 18))
    background = ImageEnhance.Color(background).enhance(0.72)
    background = ImageEnhance.Brightness(background).enhance(0.38).convert("RGBA")

    vignette = Image.radial_gradient("L").resize(size, Image.Resampling.BICUBIC)
    vignette = vignette.point(lambda value: int(value * 0.68))
    shade = Image.new("RGBA", size, (0, 3, 9, 255))
    background = Image.composite(shade, background, vignette)
    wash = Image.new("RGBA", size, (1, 8, 18, 70))
    return Image.alpha_composite(background, wash)


def _school(node_id: str) -> str:
    prefix = node_id.split(".", 1)[0]
    return prefix if prefix in SCHOOL_COLORS else "common"


def _edge_school(left: Node, right: Node) -> str:
    left_school = _school(left.node_id)
    return _school(right.node_id) if left_school == "common" else left_school


def _draw_root_icon(canvas: Image.Image, center: tuple[int, int], radius: int) -> None:
    x, y = center
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse(
        (x - radius, y - radius, x + radius, y + radius),
        fill=(133, 80, 255, 210),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(max(5, radius // 2)))
    canvas.alpha_composite(glow)
    draw = ImageDraw.Draw(canvas)
    draw.ellipse(
        (x - radius + 7, y - radius + 7, x + radius - 7, y + radius - 7),
        fill=(19, 10, 42, 255),
        outline=(255, 218, 126, 255),
        width=4,
    )
    eye_w = int(radius * 1.15)
    eye_h = int(radius * 0.58)
    draw.ellipse(
        (x - eye_w, y - eye_h, x + eye_w, y + eye_h),
        fill=(121, 71, 203, 255),
        outline=(238, 214, 255, 255),
        width=3,
    )
    pupil = max(5, radius // 4)
    draw.ellipse((x - pupil, y - pupil, x + pupil, y + pupil), fill=(19, 10, 31, 255))


def _draw_header(canvas: Image.Image, report_text: str) -> None:
    draw = ImageDraw.Draw(canvas)
    title_font = _font(64, bold=True)
    subtitle_font = _font(24)
    legend_font = _font(24, bold=True)
    draw.text((86, 68), "ARBRE MAGIQUE UNIFIE", font=title_font, fill=(242, 219, 172, 255))
    draw.text(
        (91, 144),
        "STAT MOD  |  PROGRESSION COMPLETE",
        font=subtitle_font,
        fill=(142, 187, 211, 255),
    )
    draw.line((90, 188, 670, 188), fill=(207, 159, 73, 180), width=2)

    legend_x = 92
    legend_y = 740
    draw.text((legend_x, legend_y - 52), "NEUF ECOLES", font=_font(30, True), fill=(224, 207, 174, 255))
    for index, school in enumerate(SCHOOL_LABELS):
        y = legend_y + index * 48
        color = SCHOOL_COLORS[school]
        draw.ellipse((legend_x, y + 4, legend_x + 20, y + 24), fill=color + (255,))
        draw.text((legend_x + 36, y), SCHOOL_LABELS[school], font=legend_font, fill=(210, 221, 228, 255))

    draw.text((3090, 1998), report_text, font=subtitle_font, fill=(124, 157, 176, 255))


def render_tree(root: Path, output: Path) -> RenderReport:
    tree = load_tree(root)
    index = build_texture_index(root)
    transform = compute_transform(tree, CANVAS_SIZE, 140)
    canvas = _prepare_background(root, CANVAS_SIZE)
    nodes_by_id = {node.node_id: node for node in tree.nodes}
    positions = {
        node.node_id: tuple(round(value) for value in transform.apply(node.x, node.y))
        for node in tree.nodes
    }

    edge_glow = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(edge_glow)
    core_draw = ImageDraw.Draw(canvas)
    for left_id, right_id in tree.edges:
        left = nodes_by_id[left_id]
        right = nodes_by_id[right_id]
        color = SCHOOL_COLORS[_edge_school(left, right)]
        points = (positions[left_id], positions[right_id])
        glow_draw.line(points, fill=color + (185,), width=14)
        core_draw.line(points, fill=color + (225,), width=3)
    edge_glow = edge_glow.filter(ImageFilter.GaussianBlur(11))
    canvas = Image.alpha_composite(canvas, edge_glow)
    core_draw = ImageDraw.Draw(canvas)
    for left_id, right_id in tree.edges:
        color = SCHOOL_COLORS[_edge_school(nodes_by_id[left_id], nodes_by_id[right_id])]
        core_draw.line((positions[left_id], positions[right_id]), fill=color + (230,), width=3)

    node_glow = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    node_glow_draw = ImageDraw.Draw(node_glow)
    for node in tree.nodes:
        x, y = positions[node.node_id]
        radius = round(19 + node.size * 7)
        color = SCHOOL_COLORS[_school(node.node_id)]
        node_glow_draw.ellipse(
            (x - radius - 8, y - radius - 8, x + radius + 8, y + radius + 8),
            fill=color + (180,),
        )
    canvas = Image.alpha_composite(canvas, node_glow.filter(ImageFilter.GaussianBlur(12)))

    icons_found = 0
    missing: list[str] = []
    for node in tree.nodes:
        x, y = positions[node.node_id]
        radius = round(19 + node.size * 7)
        color = SCHOOL_COLORS[_school(node.node_id)]
        if node.root:
            _draw_root_icon(canvas, (x, y), radius + 5)
            continue

        draw = ImageDraw.Draw(canvas)
        draw.ellipse(
            (x - radius, y - radius, x + radius, y + radius),
            fill=(5, 11, 21, 245),
            outline=color + (255,),
            width=4,
        )
        texture = resolve_texture(node.texture, index) if node.texture else None
        if texture is None:
            if node.texture:
                missing.append(node.texture)
            draw.text(
                (x, y),
                "?",
                font=_font(max(18, radius), True),
                fill=(235, 225, 205, 255),
                anchor="mm",
            )
            continue
        icons_found += 1
        icon_size = max(16, (radius - 7) * 2)
        texture.thumbnail((icon_size, icon_size), Image.Resampling.NEAREST)
        icon_x = x - texture.width // 2
        icon_y = y - texture.height // 2
        canvas.alpha_composite(texture, (icon_x, icon_y))

    unique_missing = tuple(sorted(set(missing)))
    _draw_header(canvas, f"{len(tree.nodes)} NOEUDS  |  {len(tree.edges)} LIENS")
    output.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(output, format="PNG", optimize=True)
    return RenderReport(
        nodes_rendered=len(tree.nodes),
        edges_rendered=len(tree.edges),
        icons_found=icons_found,
        missing_textures=unique_missing,
        output_size=CANVAS_SIZE,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Render the STAT Mod unified magic tree")
    parser.add_argument("--root", type=Path, default=Path(__file__).parents[1])
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = render_tree(args.root.resolve(), args.output.resolve())
    print(json.dumps(asdict(report), indent=2))


if __name__ == "__main__":
    main()
