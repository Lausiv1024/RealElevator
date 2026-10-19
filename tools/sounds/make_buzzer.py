"""ナッジング（強制戸閉）のブザー音を合成して OGG で書き出す。

実在メーカーの音は使わず、矩形波に近い倍音を重ねた音に、ブザーらしいざらつき（細かい振幅のゆれ）を加えて作る。
0.5秒の1回分で、かごの側で1秒ごとに鳴らして断続音にする。

使い方（numpy と soundfile が必要）:
    python make_buzzer.py <出力先のフォルダ>
"""

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 44100
FUNDAMENTAL = 620.0
# 奇数倍音（矩形波に近づける）。高い倍音ほど弱くして、耳に刺さりすぎないようにする
HARMONICS = [(1, 1.0), (3, 0.45), (5, 0.22), (7, 0.12), (9, 0.06)]
# ブザーのざらつき（振幅のゆれ）の速さと深さ
FLUTTER_HZ = 55.0
FLUTTER_DEPTH = 0.35
BUZZ_SECONDS = 0.5
FADE_SECONDS = 0.01
PEAK = 10 ** (-4 / 20)


def buzz() -> np.ndarray:
    t = np.arange(int(BUZZ_SECONDS * SAMPLE_RATE)) / SAMPLE_RATE
    tone = np.zeros_like(t)
    for harmonic, amplitude in HARMONICS:
        tone += amplitude * np.sin(2 * np.pi * FUNDAMENTAL * harmonic * t)
    tone *= 1 - FLUTTER_DEPTH * (0.5 + 0.5 * np.sin(2 * np.pi * FLUTTER_HZ * t))
    # 始まりと終わりのプチッという音を避ける
    fade = int(FADE_SECONDS * SAMPLE_RATE)
    tone[:fade] *= np.linspace(0, 1, fade)
    tone[-fade:] *= np.linspace(1, 0, fade)
    return tone / np.max(np.abs(tone)) * PEAK


def main() -> None:
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / "nudge_buzzer.ogg"
    sf.write(path, buzz().astype(np.float32), SAMPLE_RATE, format="OGG", subtype="VORBIS")
    print(path, path.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
