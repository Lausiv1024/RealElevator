"""圧縮レッドストーン（ブロック）とパワーモジュール（アイテム）のテクスチャ（16×16 のドット絵）を書き出す。

- 圧縮レッドストーン：レッドストーンブロックを3×3に詰めた見た目。暗い継ぎ目で9つの区画に分け、区画ごとに明るい粒を散らす。
- パワーモジュール：濃い灰色の筐体の中央の窓に、光るレッドストーンの芯。上下にブレイズのような橙の帯、下に金の端子。

外部のパッケージはいらない。

使い方:
    python make_power_module.py <出力先のフォルダ（textures）>
"""

import random
import sys
from pathlib import Path

from make_materials import SIZE, png

CLEAR = (0, 0, 0, 0)

# 圧縮レッドストーン
RED = (176, 24, 12, 255)
RED_DARK = (120, 12, 6, 255)
RED_LIGHT = (226, 52, 30, 255)
RED_SPARK = (255, 110, 80, 255)
SEAM = (70, 6, 4, 255)

# パワーモジュール
OUTLINE = (30, 30, 34, 255)
CASE = (70, 72, 78, 255)
CASE_LIGHT = (104, 107, 114, 255)
CASE_SHADE = (52, 54, 58, 255)
WINDOW = (40, 8, 6, 255)
CORE = (232, 40, 24, 255)
CORE_LIGHT = (255, 120, 90, 255)
BLAZE = (240, 150, 30, 255)
BLAZE_LIGHT = (255, 214, 90, 255)
GOLD = (250, 210, 60, 255)
GOLD_SHADE = (190, 140, 30, 255)


def compressed_redstone():
    rng = random.Random(7)
    px = [[RED for _ in range(SIZE)] for _ in range(SIZE)]
    # 3×3 の区画の境（幅1px の継ぎ目）
    seams = (5, 10)
    for y in range(SIZE):
        for x in range(SIZE):
            if x in seams or y in seams:
                px[y][x] = SEAM
                continue
            # 区画の中の左上を明るく、右下を暗くして、1つずつのブロックに見せる
            left = x in (0, 6, 11)
            top = y in (0, 6, 11)
            right = x in (4, 9, 15)
            bottom = y in (4, 9, 15)
            if left or top:
                px[y][x] = RED_LIGHT
            elif right or bottom:
                px[y][x] = RED_DARK
            else:
                r = rng.random()
                px[y][x] = RED_SPARK if r < 0.12 else RED_LIGHT if r < 0.3 else RED_DARK if r < 0.45 else RED
    return px


def power_module():
    px = [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]
    # 筐体（x 2〜13、y 1〜12）
    for y in range(1, 13):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (1, 12)
            if edge:
                px[y][x] = OUTLINE
            elif x == 3 or y == 2:
                px[y][x] = CASE_LIGHT
            elif x == 12 or y == 11:
                px[y][x] = CASE_SHADE
            else:
                px[y][x] = CASE
    # 上下の橙の帯（ブレイズ）
    for x in range(4, 12):
        px[3][x] = BLAZE_LIGHT if x % 3 == 0 else BLAZE
        px[10][x] = BLAZE if x % 3 == 0 else BLAZE_LIGHT
    # 中央の窓と、光るレッドストーンの芯
    for y in range(5, 9):
        for x in range(5, 11):
            px[y][x] = WINDOW
    for y in range(6, 8):
        for x in range(6, 10):
            px[y][x] = CORE
    px[6][6] = CORE_LIGHT
    px[6][7] = CORE_LIGHT
    # クォーツの白い留め具（窓の左右）
    for y in (5, 8):
        px[y][4] = (236, 232, 224, 255)
        px[y][11] = (236, 232, 224, 255)
    # 下の金の端子
    for x in (4, 6, 9, 11):
        px[13][x] = GOLD
        px[14][x] = GOLD_SHADE
    return px


def main() -> None:
    out = Path(sys.argv[1])
    (out / 'block').mkdir(parents=True, exist_ok=True)
    (out / 'item').mkdir(parents=True, exist_ok=True)
    png(out / 'block' / 'compressed_redstone.png', compressed_redstone())
    png(out / 'item' / 'power_module.png', power_module())


if __name__ == '__main__':
    main()
