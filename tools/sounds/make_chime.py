"""到着チャイムの音を合成して OGG で書き出す。

実在メーカーの音（録音）は使わず、すべてプログラムで合成する。チャイムの種類ごとに、
到着チャイムの上り・下りと、群管理の即時予報（割り当てたときの知らせ。上りも下りも同じ）を作る。
[ ] は和音（同時に鳴らす）。

- チャイム1（間隔0.45秒、ベル風）：予報 G#5、上り G#5 → C6、下り C6 → G#5
- チャイム2（間隔0.25秒、電子音）：予報 [G5, A#5]、上り D#5 → G5、下り G5 → D#5
- チャイム3（間隔0.25秒、電子音）：予報 [G5, A#5]（チャイム2と同じファイルを使う）、上り C5 → F5 → G5 → [A5, C6]、下り C6 → G5 → F5 → C5

音色は2つ。
- ベル風：ベルに似た倍音を持つ正弦波を重ねる。倍音が少なく、丸い。
- 電子音：メロディICのような矩形波を減衰させ、小さいスピーカーから出たように低音と高音を削る。

鳴っている長さ（最後の音までの間隔＋余韻 STRIKE_SECONDS）は、ChimeType#ringTicks と合わせる。

使い方（numpy と soundfile が必要）:
    python make_chime.py <出力先のフォルダ>
"""

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 44100
C_5 = 523.25
D_SHARP_5 = 622.25
F_5 = 698.46
G_5 = 783.99
G_SHARP_5 = 830.61
A_5 = 880.00
A_SHARP_5 = 932.33
C_6 = 1046.50
BELL = "bell"
CHIP = "chip"
# 書き出すファイル名と、(音の間隔 [秒], 鳴らす音の並び, 音色)。並びの1つは、同時に鳴らす基音のタプル
PATTERNS = {
    "chime_up": (0.45, ((G_SHARP_5,), (C_6,)), BELL),
    "chime_down": (0.45, ((C_6,), (G_SHARP_5,)), BELL),
    "chime_forecast": (0.45, ((G_SHARP_5,),), BELL),
    "chime_2_up": (0.25, ((D_SHARP_5,), (G_5,)), CHIP),
    "chime_2_down": (0.25, ((G_5,), (D_SHARP_5,)), CHIP),
    "chime_2_forecast": (0.25, ((G_5, A_SHARP_5),), CHIP),
    "chime_3_up": (0.25, ((C_5,), (F_5,), (G_5,), (A_5, C_6)), CHIP),
    "chime_3_down": (0.25, ((C_6,), (G_5,), (F_5,), (C_5,)), CHIP),
}
STRIKE_SECONDS = 1.6
PEAK = 10 ** (-3 / 20)

# ベル風：(基音に対する周波数比, 振幅, 減衰の速さ [1/s]) の並び
BELL_PARTIALS = [
    (1.0, 1.00, 2.2),
    (2.0, 0.35, 3.5),
    (3.0, 0.12, 5.0),
    (4.16, 0.05, 7.0),
]
BELL_ATTACK_SECONDS = 0.004

# 電子音：矩形波の減衰の速さ [1/s]、立ち上がり [秒]、スピーカー風に残す帯域 [Hz]
CHIP_DECAY = 2.6
CHIP_ATTACK_SECONDS = 0.003
SPEAKER_LOW = 500.0
SPEAKER_HIGH = 3500.0


def fade_out(tone: np.ndarray) -> np.ndarray:
    """終わりを滑らかに 0 にする。"""
    tail = int(0.05 * SAMPLE_RATE)
    tone[-tail:] *= np.linspace(1, 0, tail)
    return tone


def bell(fundamental: float, t: np.ndarray) -> np.ndarray:
    tone = np.zeros_like(t)
    for ratio, amplitude, decay in BELL_PARTIALS:
        tone += amplitude * np.sin(2 * np.pi * fundamental * ratio * t) * np.exp(-decay * t)
    # 打った瞬間のプチッという音を避けるため、立ち上がりを滑らかにする
    tone *= np.clip(t / BELL_ATTACK_SECONDS, 0, 1)
    return fade_out(tone)


def square(fundamental: float, t: np.ndarray) -> np.ndarray:
    """矩形波。折り返しのノイズが出ないよう、ナイキスト周波数の手前までの奇数倍音を足して作る。"""
    out = np.zeros_like(t)
    k = 1
    while fundamental * k < SAMPLE_RATE / 2 * 0.9:
        out += (2 / (k * np.pi)) * np.sin(np.pi * k * 0.5) * np.cos(2 * np.pi * fundamental * k * t - np.pi * k * 0.5)
        k += 1
    return out


def chip(fundamental: float, t: np.ndarray) -> np.ndarray:
    envelope = np.exp(-CHIP_DECAY * t) * np.clip(t / CHIP_ATTACK_SECONDS, 0, 1)
    return square(fundamental, t) * fade_out(envelope)


def lowpass(x: np.ndarray, cutoff: float) -> np.ndarray:
    """1次のローパスフィルター。"""
    a = np.exp(-2 * np.pi * cutoff / SAMPLE_RATE)
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def speaker(x: np.ndarray) -> np.ndarray:
    """小さいスピーカーのように、低音と高音を削る。"""
    filtered = lowpass(lowpass(x, SPEAKER_HIGH), SPEAKER_HIGH)
    return filtered - lowpass(filtered, SPEAKER_LOW)


def chime(interval: float, steps: tuple, timbre: str) -> np.ndarray:
    voice = bell if timbre == BELL else chip
    gap = int(interval * SAMPLE_RATE)
    length = int(STRIKE_SECONDS * SAMPLE_RATE)
    t = np.arange(length) / SAMPLE_RATE
    out = np.zeros(gap * (len(steps) - 1) + length)
    for i, chord in enumerate(steps):
        for fundamental in chord:
            out[i * gap:i * gap + length] += voice(fundamental, t)
    if timbre == CHIP:
        out = speaker(out)
    return out / np.max(np.abs(out)) * PEAK


def main() -> None:
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    # 名前を続けて書けば、その音だけ書き出す
    names = sys.argv[2:] or list(PATTERNS)
    for name in names:
        interval, steps, timbre = PATTERNS[name]
        path = out_dir / f"{name}.ogg"
        sf.write(path, chime(interval, steps, timbre).astype(np.float32), SAMPLE_RATE, format="OGG", subtype="VORBIS")
        print(path, path.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
