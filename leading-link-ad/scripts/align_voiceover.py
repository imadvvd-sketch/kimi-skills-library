#!/usr/bin/env python3
"""Find where every script phrase starts inside one continuous voiceover take
(public/audio/vo/voiceover.mp3) and write src/content/vo-timeline.json, which
drives all scene timing.

How it works: the script phrases are synthesized with Kokoro as a reference
with known phrase boundaries, aligned to the real take with DTW on MFCC
features, then each boundary is snapped to the nearest pause in the take.

Requires: pip install librosa soundfile kokoro-onnx (models in .tts-models/,
downloaded by generate_voiceover.py), ffmpeg.
"""
import json, re, subprocess, sys
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parent.parent
TAKE = ROOT / "public/audio/vo/voiceover.mp3"
LINES = json.loads((ROOT / "src/content/voiceover.json").read_text())["lines"]
SR, HOP = 22050, 256


def pauses(path: Path, noise="-36dB", d=0.08):
    """(start, end) of every pause in the take, via ffmpeg silencedetect."""
    log = subprocess.run(["ffmpeg", "-i", str(path), "-af", f"silencedetect=noise={noise}:d={d}",
                          "-f", "null", "-"], capture_output=True, text=True).stderr
    starts = [float(x) for x in re.findall(r"silence_start: ([\d.]+)", log)]
    ends = [float(x) for x in re.findall(r"silence_end: ([\d.]+)", log)]
    return list(zip(starts, ends))


def main():
    import librosa
    from kokoro_onnx import Kokoro
    k = Kokoro(str(ROOT / ".tts-models/kokoro-v1.0.onnx"), str(ROOT / ".tts-models/voices-v1.0.bin"))
    ref, marks, t = [], [], 0.0
    for line in LINES:
        for ph in line["captions"]:
            spoken = ph.replace("theleadinglink.ae", "theleadinglink dot A E")
            a, s = k.create(spoken, voice="am_michael", speed=1.0, lang="en-us")
            a = librosa.resample(a.astype(np.float32), orig_sr=s, target_sr=SR)
            a, _ = librosa.effects.trim(a, top_db=40)
            marks.append((line["id"], ph, t))
            ref += [a, np.zeros(int(0.15 * SR), np.float32)]
            t += len(a) / SR + 0.15
    take, _ = librosa.load(str(TAKE), sr=SR)

    def mfcc(x):
        m = librosa.feature.mfcc(y=x, sr=SR, n_mfcc=20, hop_length=HOP)[1:]
        return (m - m.mean(1, keepdims=True)) / (m.std(1, keepdims=True) + 1e-6)

    _, wp = librosa.sequence.dtw(X=mfcc(np.concatenate(ref)), Y=mfcc(take), metric="cosine")
    wp = wp[::-1]
    frame = HOP / SR
    gaps = pauses(TAKE)
    duration = len(take) / SR

    phrases = []
    for i, (lid, ph, t0) in enumerate(marks):
        est = wp[np.searchsorted(wp[:, 0], int(t0 / frame)), 1] * frame
        # snap to the end of the nearest pause (speech onset) if one is close
        near = [e for s, e in gaps if abs(e - est) < 0.45]
        start = 0.0 if i == 0 else (min(near, key=lambda e: abs(e - est)) if near else est)
        phrases.append({"id": lid, "text": ph, "start": round(start, 3)})

    lines = []
    for line in LINES:
        ps = [p for p in phrases if p["id"] == line["id"]]
        lines.append({"id": line["id"], "start": ps[0]["start"],
                      "phrases": [{"text": p["text"], "start": p["start"]} for p in ps]})
    # a line ends where the pause before the next line begins
    for i, line in enumerate(lines):
        nxt = lines[i + 1]["start"] if i + 1 < len(lines) else duration
        before = [s for s, e in gaps if line["phrases"][-1]["start"] < s <= nxt]
        line["end"] = round(before[-1] if before else nxt, 3)

    out = {"file": "audio/vo/voiceover.mp3", "durationSec": round(duration, 3), "lines": lines}
    (ROOT / "src/content/vo-timeline.json").write_text(json.dumps(out, indent=2) + "\n")
    for l in lines:
        print(f"{l['start']:6.2f}-{l['end']:6.2f}  {l['id']}")


if __name__ == "__main__":
    sys.exit(main())
