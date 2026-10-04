#!/usr/bin/env python3
"""Generate one voiceover file per scene into public/audio/vo/ and write
measured durations to src/content/vo-durations.json.

Engine selection:
  * ELEVENLABS_API_KEY set  -> ElevenLabs API (ELEVENLABS_VOICE_ID optional)
  * otherwise               -> Kokoro (local, open-weight, Apache-2.0)
      models are read from .tts-models/ (downloaded automatically if missing)
      KOKORO_VOICE (default af_heart), KOKORO_SPEED (default 1.08)
"""
import json, os, subprocess, sys, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LINES = json.loads((ROOT / "src/content/voiceover.json").read_text())["lines"]
OUT = ROOT / "public/audio/vo"
OUT.mkdir(parents=True, exist_ok=True)
MODELS = ROOT / ".tts-models"
KOKORO_URLS = {
    "kokoro-v1.0.onnx": "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/kokoro-v1.0.onnx",
    "voices-v1.0.bin": "https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/voices-v1.0.bin",
}


def duration(path: Path) -> float:
    out = subprocess.check_output([
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=nw=1:nk=1", str(path)])
    return float(out.strip())


def to_mp3(wav: Path, mp3: Path):
    # Trim leading/trailing silence so the line starts right at its scene cue.
    subprocess.check_call([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(wav),
        "-af", "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
               "silenceremove=start_periods=1:start_threshold=-45dB,areverse,"
               "loudnorm=I=-16:TP=-1.5:LRA=11",
        "-ar", "44100", "-ac", "2", "-b:a", "192k", str(mp3)])
    wav.unlink()


def elevenlabs(text: str, dest: Path):
    voice = os.environ.get("ELEVENLABS_VOICE_ID", "pNInz6obpgDQGcFmaJgB")  # "Adam"
    req = urllib.request.Request(
        f"https://api.elevenlabs.io/v1/text-to-speech/{voice}",
        data=json.dumps({
            "text": text, "model_id": "eleven_multilingual_v2",
            "voice_settings": {"stability": 0.45, "similarity_boost": 0.8, "style": 0.35},
        }).encode(),
        headers={"xi-api-key": os.environ["ELEVENLABS_API_KEY"],
                 "Content-Type": "application/json", "Accept": "audio/mpeg"})
    raw = dest.with_suffix(".raw.mp3")
    raw.write_bytes(urllib.request.urlopen(req).read())
    wav = dest.with_suffix(".wav")
    subprocess.check_call(["ffmpeg", "-y", "-loglevel", "error", "-i", str(raw), str(wav)])
    raw.unlink()
    to_mp3(wav, dest)


def kokoro_engine():
    import soundfile as sf
    from kokoro_onnx import Kokoro
    MODELS.mkdir(exist_ok=True)
    for name, url in KOKORO_URLS.items():
        if not (MODELS / name).exists():
            print(f"downloading {name} ...")
            urllib.request.urlretrieve(url, MODELS / name)
    k = Kokoro(str(MODELS / "kokoro-v1.0.onnx"), str(MODELS / "voices-v1.0.bin"))
    voice = os.environ.get("KOKORO_VOICE", "af_heart")
    speed = float(os.environ.get("KOKORO_SPEED", "1.08"))

    def synth(text: str, dest: Path, line_id: str):
        # The closing CTA is read slower and more deliberately.
        s = speed * 0.9 if line_id == "cta" else speed
        samples, sr = k.create(text, voice=voice, speed=s, lang="en-us")
        wav = dest.with_suffix(".wav")
        sf.write(wav, samples, sr)
        to_mp3(wav, dest)
    return synth, f"Kokoro ({voice}, speed {speed})"


def main():
    if os.environ.get("ELEVENLABS_API_KEY"):
        synth, engine = (lambda t, d, _id: elevenlabs(t, d)), "ElevenLabs"
    else:
        synth, engine = kokoro_engine()
    print(f"voice engine: {engine}")
    durations = {}
    for i, line in enumerate(LINES, 1):
        dest = OUT / f"{i:02d}-{line['id']}.mp3"
        synth(line["tts"], dest, line["id"])
        durations[line["id"]] = round(duration(dest), 3)
        print(f"  {dest.name}: {durations[line['id']]:.2f}s")
    (ROOT / "src/content/vo-durations.json").write_text(
        json.dumps({"engine": engine, "seconds": durations}, indent=2) + "\n")


if __name__ == "__main__":
    sys.exit(main())
