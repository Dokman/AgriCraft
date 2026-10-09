"""Validate the actual multipart tank assets, including every horizontal join and fill level."""
import itertools
import json
from pathlib import Path
import unittest


ASSETS = Path(__file__).resolve().parents[2] / "main/resources/assets/agricraft"


def matches(condition, state):
    if "OR" in condition:
        return any(matches(child, state) for child in condition["OR"])
    if "AND" in condition:
        return all(matches(child, state) for child in condition["AND"])
    return all(state[key] == value for key, value in condition.items())


def rectangles(state, block="irrigation_tank"):
    parts = json.loads((ASSETS / f"blockstates/{block}.json").read_text())["multipart"]
    for part in parts:
        if not matches(part.get("when", {}), state):
            continue
        apply = part["apply"]
        name = apply["model"].split(":")[1]
        model = json.loads((ASSETS / f"models/{name}.json").read_text())
        for element in model["elements"]:
            low, high = element["from"], element["to"]
            points = list(itertools.product([low[0], high[0]], [low[2], high[2]]))
            for _ in range(apply.get("y", 0) // 90):
                points = [(16 - z, x) for x, z in points]
            xs, zs = zip(*points)
            yield min(xs), max(xs), min(zs), max(zs), low[1], high[1], element["faces"]["up"]["texture"]


class TankModelsTest(unittest.TestCase):
    def test_channel_water_closes_height_steps(self):
        # A thin surface leaves an air gap where a full valve channel meets an
        # almost-empty channel. Water must extend down to the channel floor.
        for kind in ("arm", "channel"):
            for level in range(1, 5):
                model = json.loads((ASSETS / f"models/block/irrigation/{kind}_water_{level}.json").read_text())
                for element in model["elements"]:
                    self.assertAlmostEqual(element["from"][1], 6.01)
                    self.assertEqual(element["to"][1], 6 + level)
                    for side in ("north", "south", "east", "west"):
                        self.assertEqual(element["faces"][side]["texture"], "#water")
                        self.assertNotIn("cullface", element["faces"][side])

    def test_all_connections_and_fill_levels(self):
        for connections in itertools.product([False, True], repeat=4):
            north, east, south, west = connections
            for fill in range(1, 17):
                state = dict(zip(["north", "east", "south", "west"], map(lambda value: str(value).lower(), connections)))
                state.update(water=str(fill), down="false")
                geometry = list(rectangles(state))
                for x, z in itertools.product(range(16), repeat=2):
                    point_x, point_z = x + .5, z + .5
                    covered = [r for r in geometry if r[0] <= point_x < r[1] and r[2] <= point_z < r[3]]
                    wall = any(r[4] <= 8 < r[5] and r[6] == "#wood" for r in covered)
                    water = any(r[4] == 2 + 12 * fill / 16 and r[6] == "#water" for r in covered)
                    expected_wall = (not north and z < 2) or (not east and x >= 14) or (not south and z >= 14) or (not west and x < 2)
                    self.assertEqual(wall, expected_wall, (state, x, z, "wall"))
                    self.assertEqual(water, not expected_wall, (state, x, z, "water"))

    def test_stacked_tank_has_no_intermediate_floor(self):
        state = dict(north="true", east="true", south="true", west="true", down="true", water="0")
        self.assertEqual(list(rectangles(state)), [])

    def test_connected_channels_have_no_internal_crossbars(self):
        for mask in range(16):
            north, east, south, west = (bool(mask & (1 << i)) for i in range(4))
            state = dict(zip(["north", "east", "south", "west"], [str(value).lower() for value in [north, east, south, west]]))
            state.update(water="0", valve="false", closed="false", active="false")
            geometry = list(rectangles(state, "irrigation_channel"))
            for x, z in itertools.product(range(16), repeat=2):
                floor = (5 <= x < 11 and 5 <= z < 11) or (north and 5 <= x < 11 and z < 5) or (east and x >= 11 and 5 <= z < 11) or (south and 5 <= x < 11 and z >= 11) or (west and x < 5 and 5 <= z < 11)
                opening = (6 <= x < 10 and 6 <= z < 10) or (north and 6 <= x < 10 and z < 6) or (east and x >= 10 and 6 <= z < 10) or (south and 6 <= x < 10 and z >= 10) or (west and x < 6 and 6 <= z < 10)
                wall = any(r[0] <= x+.5 < r[1] and r[2] <= z+.5 < r[3] and r[4] <= 8 < r[5] and r[6] == "#wood" for r in geometry)
                self.assertEqual(wall, floor and not opening, (mask, x, z))

    def test_sprinkler_attachment_reaches_channel(self):
        model = json.loads((ASSETS / "models/block/irrigation/sprinkler.json").read_text())
        self.assertEqual(model["elements"][0]["to"][1], 16 + 5)

    def test_sprinkler_rotating_head_is_not_duplicated_in_static_model(self):
        folder = ASSETS / "models/block/irrigation"
        mount = json.loads((folder / "sprinkler.json").read_text())
        head = json.loads((folder / "sprinkler_head.json").read_text())
        item = json.loads((folder / "sprinkler_item.json").read_text())
        self.assertEqual(len(mount["elements"]), 1)
        self.assertEqual(len(head["elements"]), 3)
        self.assertEqual(item["elements"], mount["elements"] + head["elements"])
        for element in head["elements"]:
            self.assertTrue(all(face["texture"] == "#metal" for face in element["faces"].values()))

    def test_extended_sprinkler_attachment_uv_stays_inside_wood_texture(self):
        model = json.loads((ASSETS / "models/block/irrigation/sprinkler.json").read_text())
        attachment = model["elements"][0]
        # Auto-generated vertical UVs include 16 - maxY = -5 for this extended piece,
        # which samples neighboring sprites in the atlas. Every face needs explicit UVs.
        for direction, face in attachment["faces"].items():
            self.assertEqual(face["texture"], "#wood")
            self.assertIn("uv", face, direction)
            self.assertTrue(all(0 <= coordinate <= 16 for coordinate in face["uv"]), direction)
            self.assertLess(face["uv"][0], face["uv"][2])
            self.assertLess(face["uv"][1], face["uv"][3])


if __name__ == "__main__":
    unittest.main()
