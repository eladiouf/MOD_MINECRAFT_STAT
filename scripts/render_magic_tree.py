#!/usr/bin/env python3
from __future__ import annotations

import io
import json
import zipfile
from dataclasses import dataclass
from pathlib import Path

from PIL import Image


CATEGORY_PATH = Path(
    "src/main/resources/data/statmod/puffish_skills/categories/statmod_magic"
)


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
