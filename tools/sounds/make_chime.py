"""到着チャイムの音を合成して OGG で書き出す。

実在メーカーの音は使わず、ベルに似た倍音を持つ正弦波を重ねて作る。
上りも下りも2音で、上りは G#5 → C6（上がる）、下りは C6 → G#5（下がる）。

使い方（numpy と soundfile が必要）:
    python make_chime.py <出力先のフォルダ>
"""

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 44100
G_SHARP_5 = 830.61
C_6 = 1046.50
# 書き出すファイル名と、鳴らす音の基音の並び
PATTERNS = {
    "chime_up": (G_SHARP_5, C_6),
    "chime_down": (C_6, G_SHARP_5),
}
# (基音に対する周波数比, 振幅, 減衰の速さ [1/s])
PARTIALS = [
    (1.0, 1.00, 2.2),
    (2.0, 0.35, 3.5),
    (3.0, 0.12, 5.0),
    (4.16, 0.05, 7.0),
]
STRIKE_SECONDS = 1.6
ATTACK_SECONDS = 0.004
# 2音目を鳴らすまでの間隔
REPEAT_SECONDS = 0.45
PEAK = 10 ** (-3 / 20)


def strike(fundamental: float, seconds: float) -> np.ndarray:
    t = np.arange(int(seconds * SAMPLE_RATE)) / SAMPLE_RATE
    tone = np.zeros_like(t)
    for ratio, amplitude, decay in PARTIALS:
        tone += amplitude * np.sin(2 * np.pi * fundamental * ratio * t) * np.exp(-decay * t)
    # 打った瞬間のプチッという音を避けるため、立ち上がりを滑らかにする
    attack = np.clip(t / ATTACK_SECONDS, 0, 1)
    tone *= attack
    # 終わりも滑らかに 0 にする
    tail = int(0.05 * SAMPLE_RATE)
    tone[-tail:] *= np.linspace(1, 0, tail)
    return tone


def chime(fundamentals: tuple) -> np.ndarray:
    gap = int(REPEAT_SECONDS * SAMPLE_RATE)
    length = int(STRIKE_SECONDS * SAMPLE_RATE)
    out = np.zeros(gap * (len(fundamentals) - 1) + length)
    for i, fundamental in enumerate(fundamentals):
        out[i * gap:i * gap + length] += strike(fundamental, STRIKE_SECONDS)
    return out / np.max(np.abs(out)) * PEAK


def main() -> None:
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, fundamentals in PATTERNS.items():
        path = out_dir / f"{name}.ogg"
        sf.write(path, chime(fundamentals).astype(np.float32), SAMPLE_RATE, format="OGG", subtype="VORBIS")
        print(path, path.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
