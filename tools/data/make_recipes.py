"""レシピと、レシピ本で解放する進捗の JSON を書き出す。

- 道具：設置用ブロック、階床登録ツール、保守用レンチ、運転切替盤、仕様書（と仕様書の複製）
- 素材ブロック：作業台で1回8個。ステンレスの仕上げ（ヘアライン・鏡面・エッチング）は石切台で相互に変換
- 進捗：材料を持つとレシピ本に出す（advancement/recipes/）

外部のパッケージはいらない。

使い方:
    python make_recipes.py <データのフォルダ（resources/data/realelevator）>
"""

import json
import sys
from pathlib import Path

root = Path(sys.argv[1])
NS = 'realelevator'

recipes = {}   # 名前 -> (レシピ, レシピ本の分類, 解放する材料)


def item(i):
    return {"item": i if ':' in i else 'minecraft:' + i}


def shaped(name, pattern, key, count=1, category='misc', unlock='iron_ingot', book='misc'):
    recipes[name] = ({
        "type": "minecraft:crafting_shaped",
        "category": category,
        "pattern": pattern,
        "key": {k: item(v) for k, v in key.items()},
        "result": {"id": f"{NS}:{name}", "count": count},
    }, book, unlock)


# 道具
shaped('installer', ["IRI", "PBP", "IRI"], {"I": "iron_ingot", "R": "redstone", "P": "piston", "B": "iron_block"},
       category='redstone', book='redstone')
shaped('floor_marker', ["  R", " I ", "S  "], {"R": "redstone", "I": "iron_ingot", "S": "stick"}, category='equipment', book='tools')
shaped('maintenance_wrench', ["I I", " I ", " I "], {"I": "iron_ingot"}, category='equipment', book='tools')
shaped('power_receiver', ["SSS", "ILI", " R "], {"S": "stone", "I": "iron_ingot", "L": "lever", "R": "redstone"},
       category='redstone', book='redstone', unlock='redstone')
recipes['specification'] = ({
    "type": "minecraft:crafting_shapeless",
    "category": "misc",
    "ingredients": [item('paper'), item('blue_dye')],
    "result": {"id": f"{NS}:specification", "count": 1},
}, 'tools', 'paper')

# 素材ブロック（1回で8個）
for name, outer, center, unlock in [
    ('woodgrain_light', 'birch_planks', 'paper', 'birch_planks'),
    ('woodgrain_medium', 'oak_planks', 'paper', 'oak_planks'),
    ('woodgrain_dark', 'dark_oak_planks', 'paper', 'dark_oak_planks'),
    ('stainless_hairline', 'iron_ingot', 'quartz', 'quartz'),
    ('bronze_hairline', 'copper_ingot', 'iron_ingot', 'copper_ingot'),
    ('vinyl_gray', 'stone', 'gray_dye', 'gray_dye'),
    ('vinyl_beige', 'stone', 'white_dye', 'white_dye'),
]:
    shaped(name, ["XXX", "XCX", "XXX"], {"X": outer, "C": center}, count=8, category='building', unlock=unlock,
           book='building_blocks')

# ステンレスの仕上げは石切台で1個ずつ相互に変換
stainless = ['stainless_hairline', 'stainless_mirror', 'stainless_etched']
for a in stainless:
    for b in stainless:
        if a != b:
            recipes[f'{b}_from_{a}_stonecutting'] = ({
                "type": "minecraft:stonecutting",
                "ingredient": item(f'{NS}:{a}'),
                "result": {"id": f"{NS}:{b}", "count": 1},
            }, 'building_blocks', f'{NS}:{a}')

# 仕様書の複製
special = {"type": f"{NS}:specification_copy", "category": "misc"}

recipe_dir = root / 'recipe'
adv_dir = root / 'advancement' / 'recipes'
recipe_dir.mkdir(parents=True, exist_ok=True)


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')


for name, (data, book, unlock) in recipes.items():
    write(recipe_dir / f'{name}.json', data)
    unlock_id = unlock if ':' in unlock else 'minecraft:' + unlock
    write(adv_dir / book / f'{name}.json', {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": f"{NS}:{name}"}},
            "has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": unlock_id}]}},
        },
        "requirements": [["has_the_recipe", "has_item"]],
        "rewards": {"recipes": [f"{NS}:{name}"]},
    })
write(recipe_dir / 'specification_copy.json', special)
print(len(recipes) + 1, 'recipes')
