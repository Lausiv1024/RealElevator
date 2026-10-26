"""かごの外側の亜鉛メッキ調のテクスチャ（16×16 のドット絵）を書き出す。

灰色の地に、明るさの少しずつ違う斑（スパングル）を散らす。同じ結果になるよう乱数の種を固定する。外部のパッケージはいらない。

使い方:
    python make_exterior.py <出力先のフォルダ（textures/block）>
"""

import random
import sys
from pathlib import Path

from make_materials import SIZE, png, rgb

BASE = 0x9AA0A6


def galvanized():
    rnd = random.Random(7)
    px = [[rgb(BASE, rnd.choice([-2, 0, 0, 2])) for _ in range(SIZE)] for _ in range(SIZE)]
    # 斑：大きさの違う塊を、明るい・暗いを交互に置く（端はつなげて継ぎ目を目立たせない）
    for i in range(14):
        cx, cy = rnd.randrange(SIZE), rnd.randrange(SIZE)
        radius = rnd.choice([1, 2, 2, 3])
        delta = 9 if i % 2 == 0 else -8
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if abs(dx) + abs(dy) <= radius + rnd.choice([0, 0, 1]):
                    x, y = (cx + dx) % SIZE, (cy + dy) % SIZE
                    px[y][x] = rgb(BASE, delta + rnd.choice([-1, 0, 1]))
    return px


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    png(out / 'galvanized.png', galvanized())


if __name__ == '__main__':
    main()
