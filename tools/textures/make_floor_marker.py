"""階床登録ツールのアイテムのテクスチャ（16×16 のドット絵）を書き出す。

手に持つ道具（左下から右上へ斜め）。黒い握りの柄の先に、ステンレスの小さな端末を付け、その黒い画面に▲と▼を表示器の色（アンバー）で描く。

外部のパッケージはいらない。

使い方:
    python make_floor_marker.py <出力先のフォルダ（textures/item）>
"""

import sys
from pathlib import Path

from make_materials import SIZE, png

CLEAR = (0, 0, 0, 0)
OUTLINE = (40, 42, 46, 255)
GRIP = (58, 60, 66, 255)
GRIP_LIGHT = (92, 95, 102, 255)
BODY = (180, 185, 190, 255)
BODY_SHADE = (140, 145, 150, 255)
SCREEN = (24, 24, 28, 255)
AMBER = (255, 168, 40, 255)


def floor_marker():
    px = [[CLEAR for _ in range(SIZE)] for _ in range(SIZE)]
    # 柄：左下から右上へ、幅2pxの斜めの棒
    for i in range(6):
        x, y = 1 + i, 14 - i
        px[y][x] = GRIP if i % 2 else GRIP_LIGHT
        px[y][x + 1] = OUTLINE
    px[14][1] = OUTLINE
    # 端末：右上の、縁取りしたステンレスの箱（x 6〜14、y 0〜10）
    for y in range(0, 11):
        for x in range(6, 15):
            edge = x in (6, 14) or y in (0, 10)
            px[y][x] = OUTLINE if edge else (BODY_SHADE if x == 13 or y == 9 else BODY)
    # 画面（x 8〜12、y 2〜8）に、5ドット幅の▲と▼
    for y in range(2, 9):
        for x in range(8, 13):
            px[y][x] = SCREEN
    for y, (x0, x1) in ((2, (10, 10)), (3, (9, 11)), (4, (8, 12)), (6, (8, 12)), (7, (9, 11)), (8, (10, 10))):
        for x in range(x0, x1 + 1):
            px[y][x] = AMBER
    return px


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    png(out / 'floor_marker.png', floor_marker())


if __name__ == '__main__':
    main()
