"""仕様書のアイテムのテクスチャ（16×16 のドット絵）を書き出す。

クリップで留めた紙。上に青い表題の帯、その下に文字の行、右下にエレベーターの扉（中央開き）の小さな図。

外部のパッケージはいらない。

使い方:
    python make_specification.py <出力先のフォルダ（textures/item）>
"""

import sys
from pathlib import Path

from make_materials import SIZE, png

CLEAR = (0, 0, 0, 0)
OUTLINE = (70, 70, 78, 255)
PAPER = (238, 236, 226, 255)
PAPER_SHADE = (214, 210, 198, 255)
TITLE = (60, 104, 178, 255)
TEXT = (150, 150, 158, 255)
DOOR = (120, 128, 140, 255)
CLIP = (150, 154, 160, 255)
CLIP_DARK = (90, 94, 100, 255)


def specification():
    px = [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]
    # 紙（x 2〜13、y 2〜14）と、右と下の影
    for y in range(2, 15):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 14)
            px[y][x] = OUTLINE if edge else PAPER
    for y in range(3, 14):
        px[y][12] = PAPER_SHADE
    for x in range(3, 13):
        px[13][x] = PAPER_SHADE
    # クリップ
    for x in range(6, 10):
        px[1][x] = CLIP_DARK
        px[2][x] = CLIP
    px[3][6] = CLIP_DARK
    px[3][9] = CLIP_DARK
    # 表題の帯
    for x in range(4, 12):
        px[4][x] = TITLE
    # 文字の行
    for y, end in ((6, 11), (8, 10), (10, 5), (12, 4)):
        for x in range(4, end + 1):
            px[y][x] = TEXT
    # 扉の図（中央開き）
    for y in range(9, 13):
        for x in range(7, 12):
            px[y][x] = DOOR if x in (7, 9, 11) or y == 9 else PAPER
    return px


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    png(out / 'specification.png', specification())


if __name__ == '__main__':
    main()
