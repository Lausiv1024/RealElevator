"""ガラスのテクスチャ（16×16 のドット絵、半透明）を書き出す。

- wired_glass：防犯窓の網入りガラス。うっすら青みのある半透明の地に、4pxおきの斜めの網（両方向）。
- clear_glass：展望仕様のガラス。ほぼ透明の地に、斜めの映り込みの筋を1本。

外部のパッケージはいらない。

使い方:
    python make_glass.py <出力先のフォルダ（textures/block）>
"""

import sys
from pathlib import Path

from make_materials import SIZE, png

GLASS = (196, 220, 228, 72)
WIRE = (92, 100, 106, 200)
CLEAR = (210, 232, 240, 40)
GLARE = (240, 250, 255, 90)


def wired_glass():
    px = [[GLASS for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                px[y][x] = WIRE
    return px


def clear_glass():
    px = [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if (x + y) in (9, 10):
                px[y][x] = GLARE
    return px


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    png(out / 'wired_glass.png', wired_glass())
    png(out / 'clear_glass.png', clear_glass())


if __name__ == '__main__':
    main()
