import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from PIL import Image

from scripts.render_magic_tree import (
    build_texture_index,
    compute_transform,
    load_tree,
    render_tree,
    resolve_texture,
)


class ResourceLoadingTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        category = (
            self.root
            / "src/main/resources/data/statmod/puffish_skills/categories/statmod_magic"
        )
        category.mkdir(parents=True)
        (self.root / "libs").mkdir()

        skills = {
            "root": {"x": 10, "y": 20, "definition": "root", "root": True},
            "fire.node": {"x": 30, "y": 40, "definition": "fire.node"},
        }
        definitions = {
            "root": {
                "title": "Root",
                "icon": {"type": "item", "data": {"item": "minecraft:ender_pearl"}},
                "size": 2.8,
            },
            "fire.node": {
                "title": "Fire Node",
                "icon": {
                    "type": "texture",
                    "data": {"texture": "example:textures/gui/icon.png"},
                },
                "size": 1.5,
            },
        }
        connections = {"normal": {"bidirectional": [["root", "fire.node"]]}}
        for name, payload in (
            ("skills.json", skills),
            ("definitions.json", definitions),
            ("connections.json", connections),
        ):
            (category / name).write_text(json.dumps(payload), encoding="utf-8")

        icon_path = self.root / "icon.png"
        Image.new("RGBA", (16, 16), "#ff6600").save(icon_path)
        with zipfile.ZipFile(self.root / "libs/example.jar", "w") as archive:
            archive.write(icon_path, "assets/example/textures/gui/icon.png")

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_loads_nodes_and_edges(self):
        tree = load_tree(self.root)

        self.assertEqual(2, len(tree.nodes))
        self.assertEqual((("root", "fire.node"),), tree.edges)
        self.assertTrue(tree.nodes[0].root)
        self.assertEqual("example:textures/gui/icon.png", tree.nodes[1].texture)

    def test_indexes_and_resolves_namespaced_texture_from_jar(self):
        index = build_texture_index(self.root)

        self.assertIn("example:textures/gui/icon.png", index)
        image = resolve_texture("example:textures/gui/icon.png", index)
        self.assertIsNotNone(image)
        self.assertEqual((16, 16), image.size)

    def test_transform_fits_all_nodes_inside_margin(self):
        tree = load_tree(self.root)

        transform = compute_transform(tree, (3840, 2160), 140)
        points = [transform.apply(node.x, node.y) for node in tree.nodes]

        self.assertTrue(all(140 <= x <= 3700 for x, _ in points))
        self.assertTrue(all(140 <= y <= 2020 for _, y in points))
        self.assertLess(points[0][0], points[1][0])
        self.assertLess(points[0][1], points[1][1])

    def test_render_reports_content_and_writes_4k_png(self):
        output = self.root / "render.png"

        report = render_tree(self.root, output)

        self.assertEqual(2, report.nodes_rendered)
        self.assertEqual(1, report.edges_rendered)
        self.assertEqual(1, report.icons_found)
        self.assertEqual((), report.missing_textures)
        with Image.open(output) as image:
            self.assertEqual((3840, 2160), image.size)


if __name__ == "__main__":
    unittest.main()
