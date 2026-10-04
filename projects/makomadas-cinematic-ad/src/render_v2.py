"""
Makomadas teaser v2: built on the 8 GPT-Image keyframes in assets/keyframes.

Same voice-over, same timeline and sound design as v1 (docs/TREATMENT.md).
The images supply the photography; this script adds camera moves, light,
ink, dust, the real Arabic typography and the cut points.

    python3 src/render_v2.py                 # -> output/makomadas_teaser_v2.mp4
    python3 src/render_v2.py --still 6.5 10  # -> output/v2_06.50.png ...
"""
import math
import os
import subprocess
import sys
from multiprocessing import Pool

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import gaussian_filter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render as R  # noqa: E402  (fonts, title masks, easing, timings)

W, H, FPS, DUR = R.W, R.H, R.FPS, R.DUR
N = R.N
OUT = R.OUT
KF = os.path.join(R.ROOT, "assets", "keyframes")
sstep, keys, eio, eout, ein, clamp = R.sstep, R.keys, R.eio, R.eout, R.ein, R.clamp
IVORY, WARM, INK = R.IVORY, R.WARM, R.INK
YY, XX = R.YY, R.XX
SRC_SCALE = 1.25  # keyframes are held at 1.25x the output so the camera can move


def load(n, scale=SRC_SCALE):
    im = Image.open(os.path.join(KF, "%02d.png" % n)).convert("RGB")
    return im.resize((int(W * scale), int(H * scale)), Image.LANCZOS)


def view(img, cx, cy, sw, resample=Image.BICUBIC):
    """camera: center (cx, cy) in source px, sw = source width shown across the frame"""
    a = sw / W
    sh = sw * H / W
    return img.transform((W, H), Image.AFFINE, (a, 0, cx - sw / 2, 0, a, cy - sh / 2), resample=resample)


def f32(im):
    return np.asarray(im, np.float32) / 255


# ------------------------------------------------------------------ assets
IMG = {n: load(n) for n in (1, 2, 4, 5, 6, 7, 8)}
SW0 = W * SRC_SCALE  # full source width

# --- scene 2: the word written next to the nib (placed in source space of 02)
CAM2_END = (0.60 * SW0, 0.55 * H * SRC_SCALE, SW0 / 1.16)
WORD_SCREEN = (560.0, 1240.0)  # where "كلماتي" sits on screen at the end of scene 2


def build_word2():
    f = R.font("Amiri-Bold.ttf", 190)
    pad = 40
    w = int(f.getlength("كلماتي", direction="rtl")) + 2 * pad
    m = Image.new("L", (w, 330), 0)
    ImageDraw.Draw(m).text((w / 2, 165), "كلماتي", font=f, fill=255, anchor="mm", direction="rtl")
    # lay it on the receding paper: squash, narrower at the far (top) edge
    tw, th = w, 330
    persp = m.transform((tw, th), Image.QUAD, (int(tw * 0.04), 0, 0, th, tw, th, int(tw * 0.96), 0), Image.BICUBIC)
    persp = persp.resize((tw, int(th * 0.62)), Image.LANCZOS)
    cx, cy, sw = CAM2_END
    k = sw / W  # source px per screen px at the end of the move
    sx = WORD_SCREEN[0] * k + cx - sw / 2
    sy = WORD_SCREEN[1] * k + cy - sw * H / W / 2
    layer = Image.new("L", IMG[2].size, 0)
    pw = persp.resize((int(persp.width * k * 0.85), int(persp.height * k * 0.85)), Image.LANCZOS)
    layer.paste(pw, (int(sx - pw.width / 2), int(sy - pw.height / 2)))
    xs = np.nonzero(np.asarray(layer).max(0) > 20)[0]
    return layer.filter(ImageFilter.GaussianBlur(0.8)), (xs.min(), xs.max())


WORD2, WORD2_X = build_word2()

# --- scene 3: Mutanabbi's qasida written on the blank page of 03 (2x output)
PSC = 2.0
PAGE_IMG = load(3, PSC)
PX = PAGE_IMG.width / 941.0  # keyframe px -> page px
CREASE_X = 470 * PX
PAGE_TOP, PAGE_BOT = 300 * PX, 1360 * PX
ROW_Y = [PAGE_TOP + 40 * PX + i * (PAGE_BOT - PAGE_TOP - 80 * PX) / 11 for i in range(12)]
COL_W = 340 * PX
COL_R = CREASE_X + 35 * PX + COL_W / 2
COL_L = CREASE_X - 35 * PX - COL_W / 2


def build_page():
    base = R.font("Amiri-Regular.ttf", 66)
    rowmap = np.full((PAGE_IMG.height, PAGE_IMG.width), 255, np.uint8)
    ink = np.zeros((PAGE_IMG.height, PAGE_IMG.width), np.float32)
    hero_box = None
    for i, (sadr, ajuz) in enumerate(R.VERSES):
        for txt, cx in ((sadr, COL_R), (ajuz, COL_L)):
            f = base
            L = f.getlength(txt, direction="rtl")
            if L > COL_W:  # keep each hemistich inside its column
                f = R.font("Amiri-Regular.ttf", int(66 * COL_W / L))
            m = R.text_mask(PAGE_IMG.width, PAGE_IMG.height, txt, f, (cx, ROW_Y[i]))
            ink = np.maximum(ink, m)
            rowmap[m > 0.02] = i
            if i == R.HERO and txt == ajuz:
                full = f.getlength(ajuz, direction="rtl")
                right = cx + full / 2 - f.getlength("وأسمعتْ ", direction="rtl")
                left = right - f.getlength("كلماتي", direction="rtl")
                hero_box = ((left + right) / 2, ROW_Y[i], right - left)
    return gaussian_filter(ink, 0.7), rowmap, hero_box


PAGE_INK, PAGE_ROWS, HERO_BOX = build_page()
PAGE_INK_IM = Image.fromarray((PAGE_INK * 255).astype(np.uint8), "L")

# dust motes (screen space, drift upward slowly)
DUST = np.random.default_rng(5).uniform(0, 1, (900, 4)).astype(np.float32)


def dust_layer(t, lum, amount):
    """motes that glow only where the image itself is lit (inside beams)"""
    if amount <= 0:
        return 0
    lay = np.zeros((H, W), np.float32)
    x = (DUST[:, 0] * W + 14 * np.sin(t * 0.6 + DUST[:, 2] * 6.28)) % W
    y = (DUST[:, 1] * H - (18 + 30 * DUST[:, 3]) * t) % H
    xi, yi = x.astype(int), y.astype(int)
    np.add.at(lay, (yi, xi), 0.6 + 1.4 * DUST[:, 3])
    lay = gaussian_filter(lay, 1.1) * 6
    beam = gaussian_filter(lum, 12)
    return (lay * clamp(beam * 2.2) * amount)[..., None] * WARM[None, None]


# ---------------------------------------------------------------- scenes
def s1_ink(t):
    """0 - 4.0  هناك لحظات… لا تُعلن عن نفسها"""
    img = IMG[1]
    z = 1 + 0.10 * float(sstep(0.0, 3.9, t))
    cx, cy = 0.51 * img.width, 0.42 * img.height
    col = f32(view(img, cx, cy, SW0 / 1.06 / z))
    # the raking light switches on from the top-left, then retreats the same way
    d = (XX * 0.55 + YY * 0.45) / (W * 0.55 + H * 0.45)
    e_on = keys(t, [0.10, 1.15], [-0.25, 1.4])
    e_off = keys(t, [2.30, 3.65], [1.4, -0.25])
    edge = min(e_on, e_off)
    lit = clamp((edge - d) / 0.30)
    col = col * (0.03 + 0.97 * lit[..., None])
    # glint travelling across the wet ink on "لحظات"
    gp = float(sstep(0.55, 1.6, t))
    gx, gy = 512 + 60 * gp, 778 - 25 * gp
    g = np.exp(-((XX - gx) ** 2 / 90 + (YY - gy) ** 2 / 40)) * float(sstep(0.5, 0.8, t) * (1 - sstep(1.5, 2.2, t)))
    col += (WARM * 0.9)[None, None] * g[..., None] * lit[..., None]
    return col, 0.0


def s2_word(t):
    """4.0 - 5.84  تبدأ بكلمة — the word writes itself beside the nib"""
    img = IMG[2]
    cx0, cy0, sw0 = 0.56 * SW0, 0.52 * H * SRC_SCALE, SW0 / 1.02
    cx1, cy1, sw1 = CAM2_END
    e = float(eio((t - 4.0) / 1.84))
    cx, cy, sw = cx0 + (cx1 - cx0) * e, cy0 + (cy1 - cy0) * e, sw0 + (sw1 - sw0) * e
    col = f32(view(img, cx, cy, sw))
    m = f32(view(WORD2, cx, cy, sw))
    # right -> left on the syllables 4.37 / 4.59 / 4.72 / 4.99
    prog = keys(t, [4.11, 4.37, 4.59, 4.72, 4.99, 5.30], [0.0, 0.24, 0.47, 0.63, 0.86, 1.0])
    x0, x1 = WORD2_X
    k = W / sw
    sx0, sx1 = (x0 - (cx - sw / 2)) * k, (x1 - (cx - sw / 2)) * k
    edge = sx1 + 10 - (sx1 - sx0 + 30) * prog
    rev = clamp((XX - edge + 8 * np.sin(YY * 0.07)) / 10) * (prog > 0)
    a = m * rev * 0.92
    col = col * (1 - a[..., None]) + (INK * 0.6)[None, None] * a[..., None]
    fresh = m * clamp(1 - (XX - edge) / 60) * rev * (1 - float(sstep(5.2, 5.9, t)))
    col += (WARM * 0.20)[None, None] * fresh[..., None]
    fade = float(sstep(4.0, 4.35, t))
    return col * fade, 0.0


def page_cam(t):
    """camera over the 2x page: from the word (match-cut) back to the whole page"""
    hx, hy, hw = HERO_BOX
    # start: word fills ~300px at the same screen spot as in scene 2
    sw_start = W * hw / 300.0
    cx_start = hx - (WORD_SCREEN[0] - W / 2) * sw_start / W
    cy_start = hy - (WORD_SCREEN[1] - H / 2) * sw_start / W
    sw_end = PAGE_IMG.width / 1.0
    e = float(eio((t - 5.80) / 1.95))
    sw = math.exp(math.log(sw_start) + (math.log(sw_end) - math.log(sw_start)) * e)
    cx = cx_start + (PAGE_IMG.width / 2 - cx_start) * e
    cy = cy_start + (PAGE_IMG.height / 2 - cy_start) * e
    if t > 7.75:  # breathe: drift up the page toward the light
        d = float(sstep(7.75, 9.6, t))
        cy -= 60 * PX * d
        sw *= 1 - 0.04 * d
    return cx, cy, sw


def s3_page(t):
    """5.8 - 9.6  ثم تتحول إلى فكرة… ثم تصبح…"""
    cx, cy, sw = page_cam(t)
    col = f32(view(PAGE_IMG, cx, cy, sw))
    ink = f32(view(PAGE_INK_IM, cx, cy, sw, Image.BILINEAR))
    rows = np.asarray(view(Image.fromarray(PAGE_ROWS, "L"), cx, cy, sw, Image.NEAREST))
    # reveal: the hero bayt first, then the qasida grows outward row by row
    lut = np.zeros(256, np.float32)
    for i in range(12):
        dist = abs(i - R.HERO)
        start = 5.80 if dist == 0 else 6.15 + 0.12 * dist
        lut[i] = float(sstep(start, start + 0.5, t))
    a = ink * lut[rows] * 0.9
    # "ثم تصبح…": the paper goes out, the ink becomes light
    nt = float(sstep(8.05, 9.2, t))
    page_exp = keys(t, [8.0, 9.1], [1.0, 0.05])
    day = col * (1 - a[..., None]) + (INK * 0.7)[None, None] * a[..., None]
    night = (IVORY * 0.85)[None, None] * (a * keys(t, [8.4, 9.0, 9.6], [0.0, 0.32, 0.10]))[..., None]
    col = day * page_exp + night * nt
    return col, 0.0


def s4_light(t):
    """8.9 - 10.3: the single light is born"""
    col = f32(view(IMG[4], 0.5 * SW0, 0.5 * H * SRC_SCALE, SW0 / 1.03))
    exp = keys(t, [8.95, 9.6, 9.78], [0.0, 0.85, 1.0])
    return col * exp, 0.6 * exp


def s5_rows(t):
    """9.78 - 12.06  لحظة لا تُنسى — the page has become the hall"""
    img = IMG[6]
    mx, my = 0.494 * img.width, 0.47 * img.height
    e = float(eout((t - 9.78) / 2.6, 2))
    sw = SW0 / (1.02 + 0.10 * e) / punch(t, 9.78, 0.08)
    cx = 0.5 * img.width + (mx - 0.5 * img.width) * 0.3 * e
    cy = 0.5 * img.height + (my - 0.5 * img.height) * 0.25 * e
    col = f32(view(img, cx, cy, sw))
    # light pours down from the beam: reveal from the top
    edge = keys(t, [9.78, 10.35], [-300.0, H + 700.0])
    rev = clamp((edge - YY) / 650)
    col = col * (0.04 + 0.96 * rev[..., None])
    return col, 1.0


def s6_hall(t):
    """12.06 - 13.45  استعدوا — down the aisle"""
    img = IMG[5]
    e = float(eio((t - 12.06) / 1.45))
    sw = SW0 / (1.02 + 0.16 * e) / punch(t, 12.06, 0.12)
    cy = 0.5 * img.height + (0.47 * img.height - 0.5 * img.height) * e
    col = f32(view(img, 0.5 * img.width, cy, sw))
    # a band of light runs down the aisle toward the stage
    ys = keys(t, [12.06, 12.95], [H + 200.0, 950.0])
    band = np.exp(-((YY - ys) / 150) ** 2) * float(1 - sstep(12.9, 13.3, t))
    col = col * (1 + 1.6 * band[..., None])
    return col, 0.8


def s7_mic(t):
    """13.45 - 14.23  استعدوا (rising) — rush toward the microphone"""
    img = IMG[7]
    e = float(ein((t - 13.45) / 0.8, 2.2))
    sw = SW0 / (1.03 + 0.38 * e) / punch(t, 13.45, 0.14)
    cy = 0.5 * img.height + (0.41 * img.height - 0.5 * img.height) * e
    col = f32(view(img, 0.5 * img.width, cy, sw))
    exp = 1.0 + 0.45 * e + 0.35 * float(sstep(13.45, 13.5, t) * (1 - sstep(13.5, 13.8, t)))
    col *= exp
    col *= 1 - float(sstep(14.17, 14.24, t))
    return col, 1.2 * (1 - float(sstep(14.17, 14.24, t)))


# titles from v1 (masks, timings), composited over the stone keyframe
R.TIT["stone_alb"] = np.zeros((H, W), np.float32)


def s8_title(t):
    img = IMG[8]
    z = 1.0 + 0.05 * float(sstep(14.78, 19.5, t))
    bg = f32(view(img, 0.47 * img.width, 0.5 * img.height, SW0 / 1.02 / z))
    exp = keys(t, [14.6, 15.4, 16.0, 16.52, 16.58, 17.6, 19.05], [0.0, 0.45, 0.45, 0.12, 0.85, 0.55, 0.55])
    # the light on the stone travels left -> right while the title is read
    lx = keys(t, [14.78, 19.4], [-200.0, 700.0])
    sweep = 0.55 + 0.45 * np.exp(-((XX - lx) / 600) ** 2)
    bg = bg * (exp * sweep)[..., None]
    # keep the title zone calm
    calm = 1 - 0.55 * np.exp(-((YY - 960) / 380) ** 2)
    bg *= calm[..., None]
    ti = R.scene_titles(t)
    sl = slam(t)
    if sl > 1.0005:  # the name lands: snap from slightly larger
        cx, cy = W / 2, 935.0
        uu = (XX - cx) / sl + cx
        vv = (YY - cy) / sl + cy
        t8 = (clamp(ti / 1.6) * 255).astype(np.uint8)
        ti = R.sample(t8, uu.ravel(), vv.ravel())[0].reshape(H, W, 3) * 1.6
    if t < 16.56:  # "الملتقى الوطني" holds its breath
        ti = ti * (1 - 0.45 * float(sstep(16.1, 16.5, t)))
    alpha = clamp(ti.max(-1) / 0.85)[..., None]
    fade = 1 - float(sstep(19.12, DUR, t)) * 0.97
    flash = float(sstep(16.55, 16.57, t) * (1 - sstep(16.57, 16.75, t)))
    return bg * (1 - alpha) * fade + ti + (WARM * 0.22 * flash)[None, None], 0.0


# --------------------------------------------------------------- edit
def compose(t):
    if t < 4.0:
        return s1_ink(t)
    if t < 5.80:
        return s2_word(t)
    if t < 6.05:  # match dissolve: the written word -> the same word on the page
        a = float(sstep(5.80, 6.05, t))
        c2, _ = s2_word(min(t, 5.84))
        c3, _ = s3_page(t)
        return c2 * (1 - a) + c3 * a, 0.0
    if t < 8.95:
        return s3_page(t)
    if t < 9.78:
        a = float(sstep(8.95, 9.55, t))
        c3, _ = s3_page(t)
        c4, d = s4_light(t)
        return c3 * (1 - a) + c4 * a, d * a
    if t < 10.3:  # the beam of 04 continues into the beam of 06
        a = float(sstep(9.78, 10.3, t))
        c4, _ = s4_light(t)
        c5, d = s5_rows(t)
        return c4 * (1 - a) + c5 * a, d
    if t < 12.06:
        return s5_rows(t)
    if t < 13.45:
        return s6_hall(t)
    if t < 14.24:
        return s7_mic(t)
    if t < 14.78:
        return np.zeros((H, W, 3), np.float32), 0.0
    return s8_title(t)


def punch(t, t0, amt):
    """zoom snap on a cut: starts amt larger and settles in ~0.3s"""
    if t < t0:
        return 1.0
    return 1 + amt * (1 - float(eout((t - t0) / 0.32, 3)))


def slam(t):
    if t < 16.56:
        return 1.0
    return 1 + 0.12 * (1 - float(eout((t - 16.56) / 0.22, 3)))


# subliminal glimpses of what is coming, inside the pauses of the voice
FLASHES = [  # (start, keyframe, frames, focus x, focus y, zoom)
    (1.62, 7, 3, 0.50, 0.42, 1.35),   # the microphone, before anything is explained
    (3.80, 5, 2, 0.50, 0.52, 1.10),   # the hall with its arch
    (7.62, 6, 3, 0.50, 0.70, 1.25),   # the paper rows — the page will become a hall
    (11.62, 7, 2, 0.50, 0.40, 1.60),  # the grille of the microphone
]
IMPACTS = [1.62, 3.80, 7.62, 9.78, 11.62, 12.06, 13.45, 16.56]
BEATS = [1.95, 5.50, 8.25, 8.85, 9.35, 11.95, 12.95, 13.25]


def flash_frame(t):
    for t0, n, nf, fx, fy, z in FLASHES:
        if t0 <= t < t0 + nf / FPS:
            img = IMG[n]
            k = (t - t0) * FPS  # frame index inside the flash
            col = f32(view(img, fx * img.width, fy * img.height, SW0 / (z + 0.04 * k)))
            col = clamp((col - 0.03) * 1.25)
            if k < 1:
                col = col + 0.10 * WARM[None, None]
            return col
    return None


def shake(t):
    dx = dy = 0.0
    for t0 in IMPACTS:
        if 0 <= t - t0 < 0.5:
            a = 7 * math.exp(-(t - t0) * 9)
            dx += a * math.sin((t - t0) * 83)
            dy += a * math.cos((t - t0) * 71)
    return int(round(dx)), int(round(dy))


def pulse(t):
    p = 0.0
    for b in BEATS:
        if 0 <= t - b < 0.4:
            p += math.exp(-(t - b) * 14) + 0.6 * math.exp(-max(0, t - b - 0.17) * 16) * (t - b > 0.17)
    return 1 + 0.10 * p


def finish(col, fi, dust):
    t = fi / FPS
    col = np.maximum(col, 0)
    if dust:
        col = col + dust_layer(t, col.mean(-1), dust)
    hi = np.maximum(col - 0.6, 0)
    if hi.max() > 0:
        small = R.down(hi, 4)
        small = gaussian_filter(small, (6, 6, 0))
        col = col + R.up(small, H, W) * np.array([0.9, 0.75, 0.55])[None, None]
    vig = 1 - 0.25 * (((XX - W / 2) / (W * 0.8)) ** 2 + ((YY - H / 2) / (H * 0.65)) ** 2)
    col = col * clamp(vig, 0.5, 1)[..., None]
    lum = col.mean(-1, keepdims=True)
    g = R.up(np.random.default_rng(999 + fi).standard_normal((H // 2, W // 2)).astype(np.float32), H, W)
    col = col + (g * (0.014 + 0.014 * (1 - clamp(lum[..., 0]))))[..., None]
    return (clamp(col) * 255 + 0.5).astype(np.uint8)


def frame(fi):
    t = fi / FPS
    fl = flash_frame(t)
    if fl is not None:
        col, dust = fl, 0.0
    else:
        col, dust = compose(t)
        if t < 14.2:
            col = col * pulse(t)
    dx, dy = shake(t)
    if dx or dy:
        col = np.roll(col, (dy, dx), axis=(0, 1))
    return finish(col.astype(np.float32), fi, dust)


def main():
    os.makedirs(OUT, exist_ok=True)
    if "--still" in sys.argv:
        for a in sys.argv[sys.argv.index("--still") + 1:]:
            t = float(a)
            Image.fromarray(frame(int(round(t * FPS)))).save(os.path.join(OUT, "v2_%05.2f.png" % t))
        return
    mix = os.path.join(OUT, "mix_v2.wav")  # python3 src/audio.py --tension
    out = os.path.join(OUT, "makomadas_teaser_v3.mp4")
    ff = subprocess.Popen(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
                           "-s", "%dx%d" % (W, H), "-r", str(FPS), "-i", "-", "-i", mix,
                           "-map", "0:v", "-map", "1:a", "-c:v", "libx264", "-preset", "slow",
                           "-b:v", "11M", "-maxrate", "14M", "-bufsize", "22M", "-profile:v", "high",
                           "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "320k", "-t", "%.3f" % DUR,
                           "-movflags", "+faststart", out], stdin=subprocess.PIPE)
    with Pool(int(os.environ.get("JOBS", os.cpu_count()))) as pool:
        for i, fr in enumerate(pool.imap(frame, range(N), chunksize=3)):
            ff.stdin.write(fr.tobytes())
            if i % 60 == 0:
                print("frame %d/%d" % (i, N), file=sys.stderr, flush=True)
    ff.stdin.close()
    ff.wait()


if __name__ == "__main__":
    main()
