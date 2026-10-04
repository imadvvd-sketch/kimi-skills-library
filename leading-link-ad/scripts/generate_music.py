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
    # drums: four-on-the-floor kick after the hook, claps on 2 and 4, hats
    pump = np.ones(n)
    for i in range(int(LENGTH / BEAT)):
        st = i * BEAT
        s0 = int(st * SR)
        if st >= 3.6:
            m = int(0.35 * SR)
            if s0 + m < n:
                tt = np.arange(m) / SR
                f = 42 + 120 * np.exp(-tt * 28)
                kick = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 9) * 0.5
                kick += rng.standard_normal(m) * np.exp(-tt * 300) * 0.08  # click
                out[s0:s0 + m] += kick[:, None]
                # sidechain: duck the bed right after each kick
                pm = int(0.22 * SR)
                pump[s0:s0 + pm] = np.minimum(pump[s0:s0 + pm], 0.45 + 0.55 * np.linspace(0, 1, pm) ** 0.6)
        if st >= 5.0 and i % 2 == 1:
            m = int(0.18 * SR)
            if s0 + m < n:
                cl = rng.standard_normal(m)
                cl = (cl - lowpass(cl, 0.15)) * np.exp(-np.arange(m) / SR * 28) * 0.16
                out[s0:s0 + m, 0] += cl
                out[s0:s0 + m, 1] += cl * 0.9
        for sub in (0.5,):
            hs = int((st + BEAT * sub) * SR)
            if st >= 3.6:
                m = int(0.05 * SR)
                if hs + m < n:
                    hat = rng.standard_normal(m)
                    hat = (hat - lowpass(hat, 0.3)) * np.exp(-np.arange(m) / SR * 80) * 0.06
                    out[hs:hs + m, 0] += hat * 0.8
                    out[hs:hs + m, 1] += hat
    out *= pump[:, None] ** 0.5
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


def hit():
    """Short cinematic impact: noise transient + pitched-down body."""
    m = int(1.2 * SR)
    tt = np.arange(m) / SR
    f = 30 + 140 * np.exp(-tt * 18)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 4.5)
    noise = rng.standard_normal(m)
    crack = lowpass(noise, 0.5) * np.exp(-tt * 40) * 0.9
    tail = lowpass(noise, 0.05) * np.exp(-tt * 5) * 0.6
    y = body + crack + tail
    y /= np.max(np.abs(y)) / 0.9
    write(ROOT / "public/audio/sfx/hit.mp3", np.stack([y, np.roll(y, 90)], 1))


def boom():
    """Big sub drop for the logo reveals."""
    m = int(2.4 * SR)
    tt = np.arange(m) / SR
    f = 26 + 70 * np.exp(-tt * 6)
    sub = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 1.6)
    noise = rng.standard_normal(m)
    air = lowpass(noise, 0.12) * np.exp(-tt * 3) * 0.5
    crack = noise * np.exp(-tt * 60) * 0.35
    y = sub + air + crack
    y /= np.max(np.abs(y)) / 0.92
    write(ROOT / "public/audio/sfx/boom.mp3", np.stack([y, np.roll(y, 140)], 1))


def riser():
    """1.2 s noise + tone sweep that builds into a reveal."""
    m = int(1.25 * SR)
    tt = np.arange(m) / SR
    u = tt / tt[-1]
    noise = rng.standard_normal(m)
    alphas = 0.01 + 0.5 * u ** 2
    y = np.empty(m)
    acc = 0.0
    for i in range(m):
        acc += alphas[i] * (noise[i] - acc)
        y[i] = acc
    tone = np.sin(2 * np.pi * np.cumsum(180 + 900 * u ** 2) / SR) * 0.25
    y = (y / np.max(np.abs(y)) + tone) * u ** 2.2
    y /= np.max(np.abs(y)) / 0.8
    write(ROOT / "public/audio/sfx/riser.mp3", np.stack([y * (1 - 0.3 * u), y * (0.7 + 0.3 * u)], 1))


if __name__ == "__main__":
    music()
    whoosh()
    hit()
    boom()
    riser()
    print("wrote public/audio/music/bed.mp3 and public/audio/sfx/{whoosh,hit,boom,riser}.mp3")
