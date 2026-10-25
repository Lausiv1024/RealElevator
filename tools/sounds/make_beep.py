"""非常呼びボタンを押したときの短い音を合成して OGG で書き出す。

実在メーカーの音は使わず、柔らかい正弦波の「ピッ」を1回鳴らす（0.18秒）。

使い方（numpy と soundfile が必要）:
    python make_beep.py <出力先のフォルダ>
"""

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 44100
FREQUENCY = 1320.0
SECONDS = 0.18
FADE_SECONDS = 0.012
PEAK = 10 ** (-6 / 20)


def beep() -> np.ndarray:
    t = np.arange(int(SECONDS * SAMPLE_RATE)) / SAMPLE_RATE
    tone = np.sin(2 * np.pi * FREQUENCY * t) + 0.15 * np.sin(2 * np.pi * FREQUENCY * 2 * t)
    fade = int(FADE_SECONDS * SAMPLE_RATE)
    tone[:fade] *= np.linspace(0, 1, fade)
    tone[-fade:] *= np.linspace(1, 0, fade)
    return tone / np.max(np.abs(tone)) * PEAK


def main() -> None:
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / "emergency_beep.ogg"
    sf.write(path, beep().astype(np.float32), SAMPLE_RATE, format="OGG", subtype="VORBIS")
    print(path, path.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
