import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from PIL import Image

from scripts.render_magic_tree import build_texture_index, load_tree, resolve_texture


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


if __name__ == "__main__":
    unittest.main()
