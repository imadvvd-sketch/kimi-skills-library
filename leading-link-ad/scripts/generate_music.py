#!/usr/bin/env python3
"""Synthesize a license-free background music bed and a whoosh SFX with numpy.

Everything here is generated from scratch (no samples), so there are no rights
issues. To use your own track instead, drop it in public/audio/music/ and point
MUSIC_FILE in src/theme.ts at it.
"""
import subprocess
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parent.parent
SR = 44100
BPM = 112
BEAT = 60 / BPM
LENGTH = 61.0
rng = np.random.default_rng(7)


def note(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def env(n, a, r):
    e = np.ones(n)
    a, r = int(a * SR), int(r * SR)
    e[:a] = np.linspace(0, 1, a)
    e[-r:] *= np.linspace(1, 0, r)
    return e


def lowpass(x, alpha):
    # one-pole low-pass, cheap and good enough for softening pads/noise
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def write(path: Path, stereo: np.ndarray):
    wav = path.with_suffix(".f32")
    stereo.astype(np.float32).tofile(wav)
    subprocess.check_call([
        "ffmpeg", "-y", "-loglevel", "error", "-f", "f32le", "-ar", str(SR), "-ac", "2",
        "-i", str(wav), "-b:a", "192k", str(path)])
    wav.unlink()


def music():
    n = int(LENGTH * SR)
    t = np.arange(n) / SR
    out = np.zeros((n, 2))
    # i - VI - III - VII in A minor: Am, F, C, G (two bars each)
    chords = [[57, 60, 64, 69], [53, 57, 60, 65], [48, 55, 60, 64], [55, 59, 62, 67]]
    bar = BEAT * 4
    seg = bar * 2
    for k in range(int(LENGTH / seg) + 1):
        start = k * seg
        s0 = int(start * SR)
        m = min(int((seg + 0.6) * SR), n - s0)
        if m <= 0:
            break
        tt = np.arange(m) / SR
        e = env(m, 0.8, 0.9)
        chord = chords[k % 4]
        pad = np.zeros(m)
        for midi in chord:
            f = note(midi)
            # detuned saw-ish via a few harmonics, slightly detuned for width
            for det in (-0.12, 0.12):
                ff = f * 2 ** (det / 12)
                pad += sum(np.sin(2 * np.pi * ff * h * tt) / h ** 1.6 for h in (1, 2, 3))
        pad *= e * 0.035
        # sub bass on the root, pulsing on each beat
        root = note(chord[0] - 24)
        pulse = 0.55 + 0.45 * np.cos(2 * np.pi * (tt / BEAT)) ** 2
        bass = np.sin(2 * np.pi * root * tt) * pulse * e * 0.16
        out[s0:s0 + m, 0] += pad + bass
        out[s0:s0 + m, 1] += np.roll(pad, 220) + bass
    # arpeggio pluck on 8th notes, enters after the hook
    for i in range(int(LENGTH / (BEAT / 2))):
        st = i * BEAT / 2
        if st < 5.0:
            continue
        chord = chords[int(st / seg) % 4]
        midi = chord[[0, 2, 1, 3, 2, 1, 3, 2][i % 8]] + 12
        m = int(0.35 * SR)
        s0 = int(st * SR)
        if s0 + m > n:
            break
        tt = np.arange(m) / SR
        pl = np.sin(2 * np.pi * note(midi) * tt) * np.exp(-tt * 11) * 0.05
        pan = 0.5 + 0.35 * np.sin(i * 0.7)
        out[s0:s0 + m, 0] += pl * (1 - pan)
        out[s0:s0 + m, 1] += pl * pan
    # soft kick on beats 1 and 3, hats on off-beats
    for i in range(int(LENGTH / BEAT)):
        st = i * BEAT
        s0 = int(st * SR)
        if st >= 2.5 and i % 2 == 0:
            m = int(0.3 * SR)
            if s0 + m < n:
                tt = np.arange(m) / SR
                f = 45 + 90 * np.exp(-tt * 30)
                kick = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 12) * 0.32
                out[s0:s0 + m] += kick[:, None]
        hs = int((st + BEAT / 2) * SR)
        if st >= 5.0:
            m = int(0.05 * SR)
            if hs + m < n:
                hat = rng.standard_normal(m)
                hat = (hat - lowpass(hat, 0.3)) * np.exp(-np.arange(m) / SR * 80) * 0.045
                out[hs:hs + m, 0] += hat * 0.8
                out[hs:hs + m, 1] += hat
    out *= env(n, 1.5, 3.0)[:, None]
    out /= np.max(np.abs(out)) / 0.89
    write(ROOT / "public/audio/music/bed.mp3", out)


def whoosh():
    m = int(0.7 * SR)
    tt = np.arange(m) / SR
    noise = rng.standard_normal(m)
    # sweep the low-pass cutoff up then down
    alphas = 0.02 + 0.25 * np.sin(np.pi * tt / tt[-1]) ** 2
    y = np.empty(m)
    acc = 0.0
    for i in range(m):
        acc += alphas[i] * (noise[i] - acc)
        y[i] = acc
    y *= np.sin(np.pi * tt / tt[-1]) ** 1.5
    y /= np.max(np.abs(y)) / 0.7
    pan = np.linspace(0.2, 0.8, m)
    write(ROOT / "public/audio/sfx/whoosh.mp3", np.stack([y * (1 - pan), y * pan], 1) * 1.4)


if __name__ == "__main__":
    music()
    whoosh()
    print("wrote public/audio/music/bed.mp3 and public/audio/sfx/whoosh.mp3")
