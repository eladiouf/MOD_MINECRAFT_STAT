import tempfile
import unittest
from pathlib import Path

from PIL import Image

from scripts.render_dungeon_map import (
    Box,
    FloorRole,
    city_to_canvas,
    extract_dungeon_model,
    floor_role,
    render_dungeon_map,
)


ROOT = Path(__file__).resolve().parents[2]


class DungeonSourceExtractionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.model = extract_dungeon_model(ROOT, representative_floor=37)

    def test_extracts_city_floor_and_progression_geometry(self):
        model = self.model

        self.assertEqual(300, model.city.radius)
        self.assertEqual(15, len(model.city.sites))
        self.assertEqual((268, 244), model.floor.footprint)
        self.assertEqual((5, 4), model.floor.grid)
        self.assertEqual(20, len(model.floor.rooms))
        self.assertEqual(10, len(model.chapters))
        self.assertEqual(300, model.floor_spacing)

    def test_route_is_boustrophedon_and_cardinally_continuous(self):
        rooms = self.model.floor.rooms
        self.assertEqual(
            [(0, 0), (1, 0), (2, 0), (3, 0), (4, 0),
             (4, 1), (3, 1), (2, 1), (1, 1), (0, 1),
             (0, 2), (1, 2), (2, 2), (3, 2), (4, 2),
             (4, 3), (3, 3), (2, 3), (1, 3), (0, 3)],
            [(room.col, room.row) for room in rooms],
        )
        for left, right in zip(rooms, rooms[1:]):
            self.assertEqual(1, abs(left.col - right.col) + abs(left.row - right.row))

    def test_reproduces_java_random_vertical_profile(self):
        self.assertEqual(
            (0, 3, 0, 0, 3, 3, 3, 0, 0, -3,
             0, -3, -6, -6, -6, -6, -6, -6, -3, -6),
            self.model.floor.offsets,
        )

    def test_floor_role_cadence_matches_objectives(self):
        cadence = self.model.cadence
        self.assertEqual(FloorRole.COMBAT, floor_role(1, cadence))
        self.assertEqual(FloorRole.TREASURE, floor_role(5, cadence))
        self.assertEqual(FloorRole.BOSS, floor_role(10, cadence))
        self.assertEqual(FloorRole.COMBAT, floor_role(11, cadence))

    def test_city_transform_preserves_center_and_bounds(self):
        box = Box(100, 200, 1200, 1200)
        transform = city_to_canvas(self.model.city, box)

        self.assertEqual((700.0, 800.0), transform.apply(0, -500))
        self.assertEqual((100.0, 200.0), transform.apply(-300, -800))
        self.assertEqual((1300.0, 1400.0), transform.apply(300, -200))

    def test_render_writes_validated_4k_blueprint(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "dungeon-map.png"

            report = render_dungeon_map(ROOT, output, representative_floor=37)

            self.assertEqual(15, report.city_sites)
            self.assertEqual(20, report.rooms)
            self.assertEqual(100, report.progression_cells)
            self.assertTrue(report.route_continuous)
            self.assertEqual(37, report.representative_floor)
            self.assertEqual((-6, 3), report.profile_range)
            with Image.open(output) as image:
                self.assertEqual((3840, 2160), image.size)


if __name__ == "__main__":
    unittest.main()
