"""頂部の制御盤のテクスチャ（16×16 のドット絵）を書き出す。

ステンレスのヘアラインの箱。正面（扉）は、下のブロックに取っ手と鍵、下の方に吸気のスリット。上のブロックには、ブレーキのときに
回収した電力を熱にして捨てる抵抗器が入っている設定なので、排熱のスリットを多めに並べる。扉は幅12px（列 2〜13）に描く。

外部のパッケージはいらない。

使い方:
    python make_controller.py <出力先のフォルダ（textures/block）>
"""

import sys
from pathlib import Path

from make_materials import SIZE, hairline, png, rgb

BASE = 0xB4B9BE
EDGE = rgb(BASE, -42)
SEAM = rgb(BASE, -30)
SLIT = (44, 46, 50, 255)
SLIT_LIP = rgb(BASE, 18)
HANDLE = (70, 72, 78, 255)
HANDLE_LIGHT = (130, 134, 140, 255)
KEY = (60, 60, 64, 255)
LEFT, RIGHT = 2, 13


def body():
    """側面・上下：扉と同じステンレスのヘアライン。"""
    return hairline(BASE, 11)


def slit(px, y, x0, x1):
    """横長のスリット（暗い穴と、その下の明るい縁）。"""
    for x in range(x0, x1 + 1):
        px[y][x] = SLIT
        if y + 1 < SIZE:
            px[y + 1][x] = SLIT_LIP


def frame(px, top, bottom):
    """扉の左右の縁（と、top・bottom なら上・下の縁）。"""
    for y in range(SIZE):
        px[y][LEFT] = EDGE
        px[y][RIGHT] = EDGE
    for x in range(LEFT, RIGHT + 1):
        if top:
            px[0][x] = EDGE
        if bottom:
            px[SIZE - 1][x] = EDGE


def lower():
    """下のブロックの正面：取っ手と鍵、下の方に吸気のスリット。"""
    px = hairline(BASE, 12)
    frame(px, False, True)
    # 扉の合わせ目の内側の線（扉の縁から1px）
    for y in range(SIZE - 1):
        px[y][LEFT + 1] = SEAM
    # 取っ手（右寄り、縦長）
    for y in range(4, 9):
        px[y][11] = HANDLE
        px[y][10] = HANDLE_LIGHT if y in (4, 8) else px[y][10]
    px[10][11] = KEY
    # 吸気のスリット
    for y in (11, 13):
        slit(px, y, 4, 9)
    return px


def upper():
    """上のブロックの正面：抵抗器の排熱のスリットを、上から下まで並べる。"""
    px = hairline(BASE, 13)
    frame(px, True, False)
    for y in range(SIZE):
        px[y][LEFT + 1] = SEAM
    for y in range(2, 14, 2):
        slit(px, y, 4, 11)
    return px


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    png(out / 'controller_side.png', body())
    png(out / 'controller_front_lower.png', lower())
    png(out / 'controller_front_upper.png', upper())


if __name__ == '__main__':
    main()
