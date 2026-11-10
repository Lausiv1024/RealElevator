"""到着チャイムの音を合成して OGG で書き出す。

実在メーカーの音（録音）は使わず、すべてプログラムで合成する。チャイムの種類ごとに、
到着チャイムの上り・下りと、群管理の即時予報（割り当てたときの知らせ。上りも下りも同じ）を作る。
[ ] は和音（同時に鳴らす）。

- チャイム1（間隔0.45秒、ベル風）：予報 G#5、上り G#5 → C6、下り C6 → G#5
- チャイム2（間隔0.3秒、電子音）：予報 [G5, A#5]、上り D#5 → G5、下り G5 → D#5
- チャイム3（間隔0.25秒、電子音）：予報 [G5, A#5]（チャイム2と同じファイルを使う）、上り C5 → F5 → G5 → [A5, C6]、下り C6 → G5 → F5 → C5
- チャイム4（間隔0.3秒、電子音）：予報 [G5, A#5]（チャイム2と同じファイル）、上りも下りも C5 → [F5, A4]（下りは上りと同じファイルを使う）

音色は2つ。
- ベル風：ベルに似た倍音を持つ正弦波を重ねる。倍音が少なく、丸い。余韻は1音ごとに BELL_SECONDS。
- 電子音：メロディICのような矩形波を減衰させ、小さいスピーカーから出たように低音と高音を削る。
  余韻は途中の音が CHIP_SECONDS、最後の音（和音なら和音のすべての音）が CHIP_LAST_SECONDS。

どの音にも最後に、実際のエレベーターで鳴っているように聞こえる処理をかける（shaft）。
- 残響：狭い縦穴で短く返る響き。左右の壁で返る初期反射と、短い残響（約0.8秒）と、床と天井のあいだを行き来する反射。
- 丸さ：出だしの立ち上がりをなだらかにし（ATTACK_SECONDS）、2.5kHz あたりから上の高音を和らげる。

鳴っている長さ（最後の音までの間隔＋最後の音の余韻。残響の尾は含めない）は、ChimeType#ringTicks と合わせる。

使い方（numpy と soundfile が必要）:
    python make_chime.py <出力先のフォルダ>
"""

import sys
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 44100
A_4 = 440.00
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
    "chime_2_up": (0.3, ((D_SHARP_5,), (G_5,)), CHIP),
    "chime_2_down": (0.3, ((G_5,), (D_SHARP_5,)), CHIP),
    "chime_2_forecast": (0.25, ((G_5, A_SHARP_5),), CHIP),
    "chime_3_up": (0.25, ((C_5,), (F_5,), (G_5,), (A_5, C_6)), CHIP),
    "chime_3_down": (0.25, ((C_6,), (G_5,), (F_5,), (C_5,)), CHIP),
    "chime_4_up": (0.3, ((C_5,), (F_5, A_4)), CHIP),
}
PEAK = 10 ** (-3 / 20)

# ベル風：(基音に対する周波数比, 振幅, 減衰の速さ [1/s]) の並び
BELL_PARTIALS = [
    (1.0, 1.00, 2.2),
    (2.0, 0.35, 3.5),
    (3.0, 0.12, 5.0),
    (4.16, 0.05, 7.0),
]
BELL_SECONDS = 1.6
# 出だしの立ち上がり [秒]。角を取って丸く聞こえるよう、少しなだらかにする（どちらの音色も同じ）
ATTACK_SECONDS = 0.015

# 電子音：余韻 [秒]（途中の音と最後の音）、余韻1.6秒のときの減衰の速さ [1/s]（余韻が長ければ、そのぶんゆっくり減衰させ、
# 終わりに残る大きさをそろえる）、スピーカー風に残す帯域 [Hz]
CHIP_SECONDS = 2.2
CHIP_LAST_SECONDS = 2.8
CHIP_DECAY_AT_1_6 = 2.6
SPEAKER_LOW = 500.0
SPEAKER_HIGH = 3500.0

# 残響：長さ（−60dB になるまで）[秒]、元の音に足す大きさ、左右の壁で返る初期反射 (遅れ [秒], 大きさ)、
# 残響を作るコムフィルターの遅れ [秒]、床と天井のあいだを行き来する反射の間隔 [秒]、反射ごとに高音を削る強さ
REVERB_SECONDS = 0.8
REVERB_WET = 0.35
EARLY_REFLECTIONS = [(0.006, 0.55), (0.011, 0.45), (0.017, 0.35), (0.024, 0.28), (0.031, 0.2)]
COMB_DELAYS = [0.0213, 0.0261, 0.0297, 0.0337]
FLUTTER_DELAY = 0.019
REVERB_DAMP = 0.35
# 丸さ：最後にかける高音を和らげるフィルター [Hz]（2回かける）
ROUND_CUTOFF = 2500.0


def fade_out(tone: np.ndarray) -> np.ndarray:
    """終わりを滑らかに 0 にする。"""
    tail = int(0.05 * SAMPLE_RATE)
    tone[-tail:] *= np.linspace(1, 0, tail)
    return tone


def bell(fundamental: float, t: np.ndarray) -> np.ndarray:
    tone = np.zeros_like(t)
    for ratio, amplitude, decay in BELL_PARTIALS:
        tone += amplitude * np.sin(2 * np.pi * fundamental * ratio * t) * np.exp(-decay * t)
    # 打った瞬間のプチッという音を避け、角を取るため、立ち上がりを滑らかにする
    tone *= np.clip(t / ATTACK_SECONDS, 0, 1)
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
    seconds = len(t) / SAMPLE_RATE
    decay = CHIP_DECAY_AT_1_6 * 1.6 / seconds
    envelope = np.exp(-decay * t) * np.clip(t / ATTACK_SECONDS, 0, 1)
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
    last = len(steps) - 1

    def seconds(i: int) -> float:
        if timbre == BELL:
            return BELL_SECONDS
        return CHIP_LAST_SECONDS if i == last else CHIP_SECONDS

    out = np.zeros(gap * last + int(seconds(last) * SAMPLE_RATE))
    for i, chord in enumerate(steps):
        length = int(seconds(i) * SAMPLE_RATE)
        t = np.arange(length) / SAMPLE_RATE
        for fundamental in chord:
            out[i * gap:i * gap + length] += voice(fundamental, t)
    if timbre == CHIP:
        out = speaker(out)
    return out / np.max(np.abs(out)) * PEAK


def comb(x: np.ndarray, delay: float, seconds: float) -> np.ndarray:
    """減衰付きのフィードバック・コムフィルター。seconds 秒で −60dB。返すたびに高音を少し削る。"""
    d = max(1, int(delay * SAMPLE_RATE))
    g = 10 ** (-3 * delay / seconds)
    y = np.zeros(len(x))
    feedback = np.zeros(d)
    # d サンプルずつまとめて計算する（d サンプル前の出力だけを使うので、ブロックの中は互いに独立）
    for start in range(0, len(x), d):
        end = min(start + d, len(x))
        n = end - start
        block = x[start:end] + g * feedback[:n]
        y[start:end] = block
        smoothed = block.copy()
        smoothed[1:] = (1 - REVERB_DAMP) * block[1:] + REVERB_DAMP * block[:-1]
        feedback = np.zeros(d)
        feedback[:n] = smoothed
    return y


def allpass(x: np.ndarray, delay: float, g: float = 0.5) -> np.ndarray:
    """オールパスフィルター（残響の反射を細かく散らす）。"""
    d = max(1, int(delay * SAMPLE_RATE))
    y = np.zeros(len(x))
    delayed = np.concatenate([np.zeros(d), x])
    for start in range(0, len(x), d):
        end = min(start + d, len(x))
        past = y[start - d:end - d] if start >= d else np.zeros(end - start)
        y[start:end] = -g * x[start:end] + delayed[start:end] + g * past
    return y


def shaft(dry: np.ndarray) -> np.ndarray:
    """狭い縦穴で短く返る残響を足し、高音を和らげて丸くする。残響の尾のぶん長くなる。"""
    x = np.concatenate([dry, np.zeros(int((REVERB_SECONDS + 0.1) * SAMPLE_RATE))])
    early = np.zeros(len(x))
    for delay, gain in EARLY_REFLECTIONS:
        d = int(delay * SAMPLE_RATE)
        early[d:] += gain * x[:len(x) - d]
    late = sum(comb(x, delay, REVERB_SECONDS) for delay in COMB_DELAYS) / len(COMB_DELAYS)
    late += comb(x, FLUTTER_DELAY, REVERB_SECONDS * 0.8) * 0.6
    late = allpass(allpass(late, 0.0051), 0.0017)
    out = x + REVERB_WET * (0.6 * early + late)
    fade_out(out)
    out = out / np.max(np.abs(out)) * PEAK
    out = lowpass(lowpass(out, ROUND_CUTOFF), ROUND_CUTOFF)
    return out / np.max(np.abs(out)) * PEAK


def main() -> None:
    out_dir = Path(sys.argv[1])
    out_dir.mkdir(parents=True, exist_ok=True)
    # 名前を続けて書けば、その音だけ書き出す
    names = sys.argv[2:] or list(PATTERNS)
    for name in names:
        interval, steps, timbre = PATTERNS[name]
        path = out_dir / f"{name}.ogg"
        sf.write(path, shaft(chime(interval, steps, timbre)).astype(np.float32), SAMPLE_RATE, format="OGG", subtype="VORBIS")
        print(path, path.stat().st_size, "bytes")


if __name__ == "__main__":
    main()
