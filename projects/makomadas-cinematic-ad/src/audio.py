"""
Makomadas — sound design bed, synthesised and mixed UNDER the untouched voice-over.

The VO samples are passed through bit-for-bit (mono -> both channels); only the
bed is shaped, ducked and limited so the sum never clips.

    python3 src/audio.py   -> output/bed.wav, output/mix.wav
"""
import os
import subprocess

import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "output")
SR = 44100
rng = np.random.default_rng(3)


def load_vo():
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", os.path.join(ROOT, "assets", "vo.mp3"),
                          "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"], capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.float32).copy()


VO = load_vo()
N = len(VO)
T = np.arange(N) / SR

# phrase map from the analysis (seconds)
PHRASES = [(0.10, 1.06), (2.23, 3.54), (4.11, 5.37), (5.84, 7.47), (8.01, 9.31), (9.78, 11.36),
           (12.06, 12.86), (13.45, 14.23), (14.78, 16.09), (16.56, 18.22), (18.72, 19.25)]


def db(x):
    return 10 ** (x / 20)


def env(points):
    """linear envelope through (t, gain) points"""
    ts, vs = zip(*points)
    return np.interp(T, ts, vs)


def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], btype="band", fs=SR, output="sos"), x)


def lp(x, f, order=2):
    return sosfilt(butter(order, f, btype="low", fs=SR, output="sos"), x)


def hp(x, f, order=2):
    return sosfilt(butter(order, f, btype="high", fs=SR, output="sos"), x)


def place(buf, sig, t0, gain=1.0):
    i = int(t0 * SR)
    if i >= len(buf):
        return
    n = min(len(sig), len(buf) - i)
    buf[i:i + n] += sig[:n] * gain


def pink(n):
    w = rng.standard_normal(n)
    f = np.fft.rfft(w)
    f /= np.sqrt(np.maximum(np.arange(len(f)), 1))
    p = np.fft.irfft(f, n)
    return p / np.abs(p).max()


def reverb_ir(sec=2.8, decay=3.2, tone=4000):
    n = int(sec * SR)
    ir = rng.standard_normal((n, 2)) * np.exp(-decay * np.arange(n) / SR)[:, None]
    ir = np.stack([lp(ir[:, c], tone) for c in range(2)], -1)
    ir[:int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))[:, None]
    return ir / np.sqrt((ir ** 2).sum(0))


IR = reverb_ir()


def verb(x, wet=0.35):
    y = np.stack([fftconvolve(x, IR[:, c])[:N] for c in range(2)], -1)
    return np.stack([x, x], -1) * (1 - wet) + y * wet


# ------------------------------------------------------------ instruments
def piano(freq, dur=6.0, vel=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    B = 0.00035
    for k in range(1, 14):
        fk = freq * k * np.sqrt(1 + B * k * k)
        if fk > 12000:
            break
        amp = (1 / k ** 1.25) * (1 + 0.6 * vel * (k > 3))
        dec = 0.55 + 0.45 * k ** 1.1
        for det in (-0.6, 0.6):  # two strings, slight beating
            out += amp * np.sin(2 * np.pi * (fk + det * k * 0.15) * t + rng.uniform(0, 6.28)) * np.exp(-dec * t * (0.9 if det < 0 else 1.1))
    att = np.minimum(1, t / 0.004)
    hammer = bp(rng.standard_normal(n), 1500, 5000) * np.exp(-t * 90) * 0.25
    out = (out * att + hammer) * np.exp(-t * 0.15)
    out = lp(out, 2500 + 2500 * vel)
    return out / np.abs(out).max() * vel


def note(name):
    names = {"C": -9, "D": -7, "E": -5, "F": -4, "G": -2, "A": 0, "B": 2}
    return 440 * 2 ** ((names[name[0]] + 12 * (int(name[-1]) - 4)) / 12)


def nib_scratch(dur, density=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    grains = (rng.random(n) < 0.004 * density).astype(float)
    grains = np.convolve(grains, np.exp(-np.arange(80) / 12), "same")
    body = bp(rng.standard_normal(n), 2500, 7500) * (0.4 + grains)
    shape = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 0.6
    return body * shape


def tick(freq=2600, decay=60):
    n = int(0.25 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * freq * t) * np.exp(-t * decay) + 0.5 * np.sin(2 * np.pi * freq * 1.51 * t) * np.exp(-t * decay * 1.6)
    click = bp(rng.standard_normal(n), 3000, 9000) * np.exp(-t * 900) * 0.6
    return s + click


def impact(f0=58, f1=30, dur=3.0, noise=0.6):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t * 4)
    ph = 2 * np.pi * np.cumsum(f) / SR
    sub = np.sin(ph) * np.exp(-t * 1.4)
    thump = lp(rng.standard_normal(n), 220) * np.exp(-t * 9) * noise * 4
    air = bp(rng.standard_normal(n), 200, 2000) * np.exp(-t * 3) * noise * 0.4
    s = sub + thump + air
    return s / np.abs(s).max()


def whoosh(dur, rise=True, lo=300, hi=5000):
    n = int(dur * SR)
    x = rng.standard_normal(n)
    out = np.zeros(n)
    blocks = 40
    for b in range(blocks):  # time-varying band sweep
        a, z = b * n // blocks, (b + 1) * n // blocks
        p = b / (blocks - 1)
        p = p if rise else 1 - p
        c = lo * (hi / lo) ** p
        out[a:z] = bp(x[max(0, a - 2000):z], c * 0.6, min(c * 1.6, 18000))[-(z - a):]
    e = np.linspace(0, 1, n) ** (2.2 if rise else 0.4)
    if not rise:
        e = e[::-1] ** 1.0
    return out * e


# ------------------------------------------------------------- the bed
def build():
    L = np.zeros(N)  # mono elements, spatialised by the reverb
    dry_st = np.zeros((N, 2))

    # room tone: pink, darkened. true silence in the black before the reveal
    rt = lp(pink(N), 2500) * db(-54)
    rt *= env([(0, 0), (0.08, 1), (14.15, 1.2), (14.24, 0.0), (14.76, 0.0), (15.2, 0.7), (19.1, 0.7), (19.48, 0)])
    dry_st += np.stack([rt, np.roll(rt, 431)], -1)

    # low drone on D, breathing with the light
    dr = np.zeros(N)
    for f, a in ((36.71, 1.0), (73.42, 0.8), (110.0, 0.35), (146.83, 0.18), (220.0, 0.06)):
        lfo = 1 + 0.003 * np.sin(2 * np.pi * 0.11 * T + f)
        dr += a * np.sin(2 * np.pi * f * np.cumsum(lfo) / SR)
    dr += lp(rng.standard_normal(N), 160) * 0.8
    dr = dr / np.abs(dr).max()
    dr *= env([(0, 0), (0.1, 0.22), (1.5, 0.30), (2.6, 0.30), (3.8, 0.06), (4.11, 0.10), (5.84, 0.30), (7.6, 0.42),
               (8.1, 0.18), (9.3, 0.05), (9.78, 0.5), (11.4, 0.55), (12.06, 0.62), (13.45, 0.7), (14.2, 1.0),
               (14.235, 0.0), (16.5, 0.0), (16.56, 0.55), (18.2, 0.42), (19.0, 0.25), (19.48, 0.0)])
    dry_st += np.stack([dr, dr], -1) * db(-24)

    # pen: the first touch of the nib, and the word being written
    place(L, tick(4200, 120) * 0.5, 0.09, db(-30))
    place(L, nib_scratch(0.42, 0.6), 0.11, db(-38))
    for t0, d in ((4.11, 0.24), (4.37, 0.2), (4.59, 0.12), (4.72, 0.24), (4.99, 0.3)):
        place(L, nib_scratch(d, 1.4), t0, db(-34))
    # glint on wet ink: a faint high shimmer
    sh = hp(rng.standard_normal(int(0.9 * SR)), 7000) * np.sin(np.linspace(0, np.pi, int(0.9 * SR))) ** 2
    place(L, sh, 0.55, db(-50))

    # paper: the page appears as the camera pulls back
    pr = bp(rng.standard_normal(int(1.6 * SR)), 600, 4500)
    pr *= np.sin(np.linspace(0, np.pi, len(pr))) ** 1.5 * (0.6 + 0.4 * np.abs(np.sin(np.linspace(0, 23, len(pr)))))
    place(L, pr, 5.84, db(-38))

    # sparse piano (D dorian), never on top of a consonant cluster
    for t0, nm, v, g in ((4.11, "D4", 0.45, -27), (6.97, "F4", 0.4, -29), (7.02, "A3", 0.35, -31),
                         (9.78, "D2", 0.8, -22), (9.79, "A2", 0.6, -25), (9.80, "D3", 0.5, -27),
                         (10.88, "E5", 0.35, -33),
                         (16.56, "D2", 0.9, -21), (16.57, "A2", 0.7, -24), (16.58, "F3", 0.55, -27),
                         (18.72, "A5", 0.3, -32)):
        place(L, piano(note(nm), 6.0, v), t0, db(g))

    # the tilt into the hall: an airy whoosh, then the moment lands
    place(L, whoosh(1.4, True, 200, 3000), 8.6, db(-44))
    place(L, impact(52, 30, 3.5, 0.4), 9.76, db(-20))
    # light sweeps on each "استعدوا"
    place(L, whoosh(0.9, False, 3000, 400), 12.04, db(-36))
    place(L, whoosh(0.8, True, 300, 6000), 13.45, db(-33))
    # rising sub under the second "استعدوا", cut dead at the black
    n = int((14.235 - 13.0) * SR)
    tt = np.arange(n) / SR
    rumble = np.sin(2 * np.pi * np.cumsum(28 + 18 * (tt / tt[-1]) ** 2) / SR) * (tt / tt[-1]) ** 2.5
    rumble += lp(rng.standard_normal(n), 120) * (tt / tt[-1]) ** 3 * 0.8
    place(L, rumble, 13.0, db(-20))

    # title: light slit shimmer, main impact, dots landing
    sl = hp(rng.standard_normal(int(1.3 * SR)), 6000) * np.sin(np.linspace(0, np.pi, int(1.3 * SR))) ** 3
    place(L, sl, 14.78, db(-50))
    place(L, impact(60, 28, 3.2, 0.7), 16.54, db(-14))
    for t0 in (17.62, 17.69, 17.77):
        place(L, tick(2300 + 300 * (t0 > 17.7), 70), t0, db(-34))

    # ducking under the voice (bed only): -5 dB while words are spoken
    duck = np.ones(N)
    for a, b in PHRASES:
        duck -= (1 - db(-5)) * np.clip(np.minimum((T - a + 0.05) / 0.08, (b + 0.12 - T) / 0.15), 0, 1)
    bed = verb(L, 0.38) + dry_st
    bed *= duck[:, None]
    # end: everything settles to silence on the last sample
    bed *= env([(0, 1), (19.1, 1), (DUR, 0.0)])[:, None]
    # the black before the reveal is TRUE silence: cut the reverb tails too
    bed *= env([(0, 1), (14.232, 1), (14.242, 0), (14.80, 0), (14.95, 1), (DUR, 1)])[:, None]
    return bed


DUR = N / SR


def main():
    os.makedirs(OUT, exist_ok=True)
    bed = build()
    vo = np.stack([VO, VO], -1)
    # make room: if the sum would exceed -1 dBFS, pull the BED down locally (never the VO)
    mix = vo + bed
    over = np.max(np.abs(mix), 1)
    g = np.ones(N)
    bad = over > db(-1)
    if bad.any():
        need = np.where(bad, (db(-1) - np.abs(VO)) / np.maximum(np.max(np.abs(bed), 1), 1e-9), 1.0)
        need = np.clip(need, 0, 1)
        # smooth gain reduction (look-ahead 10 ms, release 150 ms)
        k = np.exp(-np.arange(int(0.15 * SR)) / (0.05 * SR))
        red = 1 - need
        red = np.maximum.accumulate(red[::-1])[::-1] * 0 + red
        red = np.convolve(red, k / k.sum() * 3, "same")
        g = np.clip(1 - np.maximum(red, 1 - need), 0, 1)
    mix = vo + bed * g[:, None]
    print("peak mix %.2f dBFS, VO peak %.2f dBFS, bed rms %.1f dBFS" % (
        20 * np.log10(np.abs(mix).max()), 20 * np.log10(np.abs(VO).max()), 20 * np.log10(np.sqrt((bed ** 2).mean()))))
    import wave
    for name, data in (("bed.wav", bed), ("mix.wav", mix)):
        pcm = (np.clip(data, -1, 1) * 32767).astype("<i2")
        with wave.open(os.path.join(OUT, name), "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
    # proof that the VO is intact: subtract the bed, compare
    resid = mix - bed * g[:, None] - vo
    print("VO integrity: max deviation %.2e (float path)" % np.abs(resid).max(), "samples", N, "dur %.3f s" % DUR)


if __name__ == "__main__":
    main()
