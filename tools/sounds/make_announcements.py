"""同梱するアナウンス音声を VOICEVOX で生成し、OGG で書き出す。

ローカルで起動している VOICEVOX エンジンの HTTP API（audio_query → synthesis）を使う。
クリップの名前と文は、core の AnnouncementScript の決まりと合わせる。

  - floor_1 … floor_50   「◯階です。」
  - floor_b1 … floor_b5  「地下◯階です。」
  - floor_r              「屋上です。」
  - dir_up / dir_down    「上へ参ります。」「下へ参ります。」
  - door_close           「ドアが閉まります。」

書き出すもの（<assets> は assets/realelevator）:
  - <assets>/sounds/announce/<クリップ>.ogg
  - <assets>/announcements.json   各クリップの長さ [秒]。クライアントがつなげて鳴らすときに使う
  - <assets>/sounds.json          announce.<クリップ> のサウンドイベントを追記する（ほかのエントリは残す）

使い方（numpy と soundfile が必要。VOICEVOX を起動しておく）:
    python make_announcements.py <assets/realelevator のパス> [話者名] [スタイル名]

音声の利用規約は話者ごとに異なる。生成した音声を配布するときは、話者の規約に従ってクレジットを表示すること。
"""

import io
import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path

import numpy as np
import soundfile as sf

ENGINE = "http://127.0.0.1:50021"
DEFAULT_SPEAKER = "冥鳴ひまり"
DEFAULT_STYLE = "ノーマル"
MAX_FLOOR = 50
MAX_BASEMENT = 5
# 文の前後の無音 [秒]。つなげたときに間が空きすぎないよう短めにする
PRE_SILENCE = 0.05
POST_SILENCE = 0.15
PEAK = 10 ** (-3 / 20)


def clips() -> list:
    result = [(f"floor_{n}", f"{n}階です。") for n in range(1, MAX_FLOOR + 1)]
    result += [(f"floor_b{n}", f"地下{n}階です。") for n in range(1, MAX_BASEMENT + 1)]
    result += [
        ("floor_r", "屋上です。"),
        ("dir_up", "上へ参ります。"),
        ("dir_down", "下へ参ります。"),
        ("door_close", "ドアが閉まります。"),
    ]
    return result


def request(path: str, data: bytes = None, content_type: str = None) -> bytes:
    req = urllib.request.Request(ENGINE + path, data=data, method="POST" if data is not None else "GET")
    if content_type:
        req.add_header("Content-Type", content_type)
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()


def find_style(speaker_name: str, style_name: str) -> int:
    for speaker in json.loads(request("/speakers")):
        if speaker["name"] == speaker_name:
            for style in speaker["styles"]:
                if style["name"] == style_name:
                    return style["id"]
    raise SystemExit(f"話者が見つかりません: {speaker_name}（{style_name}）")


def synthesize(text: str, style_id: int) -> tuple:
    query_path = "/audio_query?" + urllib.parse.urlencode({"text": text, "speaker": style_id})
    query = json.loads(request(query_path, data=b""))
    query["prePhonemeLength"] = PRE_SILENCE
    query["postPhonemeLength"] = POST_SILENCE
    wav = request("/synthesis?" + urllib.parse.urlencode({"speaker": style_id}),
                  data=json.dumps(query).encode("utf-8"), content_type="application/json")
    audio, rate = sf.read(io.BytesIO(wav), dtype="float32")
    if audio.ndim > 1:
        audio = audio.mean(axis=1)
    return audio, rate


def main() -> None:
    assets = Path(sys.argv[1])
    speaker_name = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_SPEAKER
    style_name = sys.argv[3] if len(sys.argv) > 3 else DEFAULT_STYLE
    style_id = find_style(speaker_name, style_name)
    out_dir = assets / "sounds" / "announce"
    out_dir.mkdir(parents=True, exist_ok=True)

    synthesized = {}
    for name, text in clips():
        synthesized[name] = synthesize(text, style_id)
        print(name, text, f"{len(synthesized[name][0]) / synthesized[name][1]:.2f}s")

    # 音量はすべてのクリップで同じ倍率にそろえる（つなげたときに大きさが変わらないよう）
    loudest = max(np.max(np.abs(audio)) for audio, _ in synthesized.values())
    durations = {}
    for name, (audio, rate) in synthesized.items():
        sf.write(out_dir / f"{name}.ogg", (audio / loudest * PEAK).astype(np.float32), rate, format="OGG", subtype="VORBIS")
        durations[name] = round(len(audio) / rate, 3)

    (assets / "announcements.json").write_text(
        json.dumps({"speaker": f"VOICEVOX:{speaker_name}", "durations": durations}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")

    sounds_path = assets / "sounds.json"
    sounds = json.loads(sounds_path.read_text(encoding="utf-8")) if sounds_path.exists() else {}
    for name, _ in clips():
        sounds[f"announce.{name}"] = {
            "subtitle": "subtitles.realelevator.announcement",
            "sounds": [f"realelevator:announce/{name}"],
        }
    sounds_path.write_text(json.dumps(sounds, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{len(durations)} clips, credit: VOICEVOX:{speaker_name}")


if __name__ == "__main__":
    main()
