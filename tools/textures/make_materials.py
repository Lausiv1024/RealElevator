"""かご内装のオリジナル素材のテクスチャ（16×16 のドット絵）を書き出す。

縦木目（明るい・中間・濃い）、ステンレス（ヘアライン・鏡面・エッチング）、ブロンズ調ヘアライン、石目調ビニル（灰・ベージュ）。
同じ結果になるよう乱数の種を固定する。外部のパッケージはいらない。

使い方:
    python make_materials.py <出力先のフォルダ（textures/block）>
"""

import random
import struct
import sys
import zlib
from pathlib import Path

SIZE = 16


def png(path: Path, pixels) -> None:
    raw = b''.join(b'\x00' + bytes(c for px in row for c in px) for row in pixels)

    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', SIZE, SIZE, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def rgb(h: int, delta: int = 0):
    r, g, b = (h >> 16) & 255, (h >> 8) & 255, h & 255
    return tuple(max(0, min(255, c + delta)) for c in (r, g, b)) + (255,)


def woodgrain(base: int, seed: int):
    """縦の木目。列ごとに少しずつ明るさを変え、ところどころ細い濃い筋と、ゆるやかに揺れる年輪を入れる。"""
    rnd = random.Random(seed)
    column_shade = [rnd.choice([-6, -3, 0, 0, 3, 5]) for _ in range(SIZE)]
    streaks = rnd.sample(range(SIZE), 3)
    px = [[rgb(base, column_shade[x]) for x in range(SIZE)] for _ in range(SIZE)]
    for x in streaks:
        for y in range(SIZE):
            px[y][x] = rgb(base, -16 + rnd.choice([0, 2, 4]))
    # 年輪：上下でゆるやかに位置がずれる濃い線
    for start in (4, 11):
        for y in range(SIZE):
            x = (start + (1 if (y // 5) % 2 else 0)) % SIZE
            px[y][x] = rgb(base, -11)
    return px


def hairline(base: int, seed: int, strength: int = 6):
    """ヘアライン：縦の細い線を、明暗を細かく変えて並べる。"""
    rnd = random.Random(seed)
    shades = [rnd.randint(-strength, strength) for _ in range(SIZE)]
    return [[rgb(base, shades[x] + rnd.choice([0, 0, 0, 1, -1])) for x in range(SIZE)] for _ in range(SIZE)]


def mirror(base: int):
    """鏡面：平らな明るい面に、斜めの映り込みの帯を2本。"""
    px = [[rgb(base) for _ in range(SIZE)] for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            d = (x + y) % SIZE
            if d in (3, 4):
                px[y][x] = rgb(base, 18)
            elif d in (5, 11):
                px[y][x] = rgb(base, 8)
            elif d in (12, 13, 14):
                px[y][x] = rgb(base, -10)
    return px


def etched(base: int, seed: int):
    """エッチング：ヘアラインの上に、小さなひし形の模様を並べる。"""
    px = hairline(base, seed, 4)
    for cy in (3, 11):
        for cx in (3, 11):
            ox, oy = cx + (4 if cy == 11 else 0), cy
            for dx, dy in ((0, -2), (-1, -1), (1, -1), (-2, 0), (2, 0), (-1, 1), (1, 1), (0, 2)):
                px[(oy + dy) % SIZE][(ox + dx) % SIZE] = rgb(base, -22)
    return px


def vinyl(base: int, seed: int):
    """石目調ビニル：まだらな地に、明るい粒と暗い粒をまく。"""
    rnd = random.Random(seed)
    px = [[rgb(base, rnd.choice([-4, -2, 0, 0, 2, 4])) for _ in range(SIZE)] for _ in range(SIZE)]
    for _ in range(26):
        px[rnd.randrange(SIZE)][rnd.randrange(SIZE)] = rgb(base, rnd.choice([18, 14]))
    for _ in range(22):
        px[rnd.randrange(SIZE)][rnd.randrange(SIZE)] = rgb(base, rnd.choice([-20, -26]))
    return px


TEXTURES = {
    'woodgrain_light': woodgrain(0xC8A57E, 1),
    'woodgrain_medium': woodgrain(0x9A7656, 2),
    'woodgrain_dark': woodgrain(0x5B4232, 3),
    'stainless_hairline': hairline(0xB4B9BE, 4),
    'stainless_mirror': mirror(0xC6CBD0),
    'stainless_etched': etched(0xB0B5BA, 5),
    'bronze_hairline': hairline(0x6A5A48, 6, 5),
    'vinyl_gray': vinyl(0x8E9094, 7),
    'vinyl_beige': vinyl(0xB9AE98, 8),
}


def main() -> None:
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, pixels in TEXTURES.items():
        png(out_dir / f'{name}.png', pixels)
        print(name)


if __name__ == '__main__':
    main()
