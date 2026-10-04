"""
Makomadas — cinematic teaser renderer.

Everything is procedural: paper, ink, the hall, light, stone and type are
generated here and timed to the voice-over analysis in docs/TREATMENT.md.

    python3 src/render.py               # full render -> output/frames.mp4 (video only)
    python3 src/render.py --still 10.5  # single frame -> output/still_10.50.png
"""
import math
import os
import subprocess
import sys
from multiprocessing import Pool

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy.ndimage import gaussian_filter, label

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONTS = os.path.join(ROOT, "fonts")
OUT = os.path.join(ROOT, "output")

W, H, FPS = 1080, 1920, 30
DUR = 19.487  # exact voice-over length
N = int(math.ceil(DUR * FPS))

# palette (display-referred 0..1)
BLACK = np.array([0.020, 0.020, 0.024], np.float32)
IVORY = np.array([0.93, 0.89, 0.81], np.float32)
PAPER = np.array([0.90, 0.86, 0.78], np.float32)
INK = np.array([0.030, 0.026, 0.026], np.float32)
FLOOR = np.array([0.050, 0.046, 0.043], np.float32)
BRONZE = np.array([0.80, 0.62, 0.40], np.float32)
WARM = np.array([1.00, 0.86, 0.66], np.float32)

rng = np.random.default_rng(7)


# ----------------------------------------------------------------- helpers
def clamp(x, a=0.0, b=1.0):
    return np.minimum(np.maximum(x, a), b)


def sstep(a, b, t):
    x = clamp((t - a) / (b - a))
    return x * x * (3 - 2 * x)


def eio(x):
    x = clamp(x)
    return np.where(x < 0.5, 4 * x ** 3, 1 - (-2 * x + 2) ** 3 / 2) if isinstance(x, np.ndarray) else (
        4 * x ** 3 if x < 0.5 else 1 - (-2 * x + 2) ** 3 / 2)


def eout(x, p=3):
    x = clamp(x)
    return 1 - (1 - x) ** p


def ein(x, p=3):
    return clamp(x) ** p


def keys(t, ts, vs):
    """piecewise smooth interpolation through keyframes"""
    if t <= ts[0]:
        return vs[0]
    for i in range(len(ts) - 1):
        if t <= ts[i + 1]:
            return vs[i] + (vs[i + 1] - vs[i]) * float(sstep(ts[i], ts[i + 1], t))
    return vs[-1]


def font(name, size, wght=None):
    f = ImageFont.truetype(os.path.join(FONTS, name), size, layout_engine=ImageFont.Layout.RAQM)
    if wght is not None:
        f.set_variation_by_axes([wght])
    return f


def text_mask(w, h, txt, fnt, xy, anchor="mm"):
    im = Image.new("L", (w, h), 0)
    ImageDraw.Draw(im).text(xy, txt, font=fnt, fill=255, anchor=anchor, direction="rtl")
    return np.asarray(im, np.float32) / 255


def fbm(h, w, scales, seed):
    r = np.random.default_rng(seed)
    acc = np.zeros((h, w), np.float32)
    for s, a in scales:
        n = gaussian_filter(r.standard_normal((h, w)).astype(np.float32), s)
        acc += a * n / (n.std() + 1e-6)
    return acc


def down(img, k):
    """box downsample by integer factor"""
    h, w = img.shape[:2]
    h2, w2 = h // k * k, w // k * k
    s = img[:h2, :w2].reshape(h2 // k, k, w2 // k, k, *img.shape[2:]).mean((1, 3))
    return s


def up(img, h, w):
    """bilinear upsample of float image to (h,w)"""
    if img.ndim == 2:
        return np.asarray(Image.fromarray(img.astype(np.float32), "F").resize((w, h), Image.BILINEAR))
    return np.stack([up(img[..., c], h, w) for c in range(img.shape[2])], -1)


def soft_blur(img, sigma):
    """cheap large blur: downsample, blur, upsample"""
    if sigma < 1.5:
        return gaussian_filter(img, (sigma, sigma, 0) if img.ndim == 3 else sigma)
    k = int(max(1, min(8, sigma // 2)))
    small = down(img, k)
    s = sigma / k
    small = gaussian_filter(small, (s, s, 0) if img.ndim == 3 else s)
    return up(small, img.shape[0], img.shape[1])


def paper_maps(h, w, seed, fibers=2500, fiber_len=60):
    """albedo + relief shading for an old paper sheet"""
    r = np.random.default_rng(seed)
    blot = fbm(h, w, [(w / 6, 1.0), (w / 18, 0.5)], seed)
    tooth = fbm(h, w, [(1.2, 0.6), (3, 0.5), (9, 0.4)], seed + 1)
    fib = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(fib)
    for _ in range(fibers):
        x, y = r.uniform(0, w), r.uniform(0, h)
        a = r.uniform(0, math.pi)
        L = r.uniform(0.3, 1) * fiber_len
        pts = []
        for k in range(6):
            a += r.normal(0, 0.25)
            x += math.cos(a) * L / 6
            y += math.sin(a) * L / 6
            pts.append((x, y))
        d.line(pts, fill=int(r.uniform(40, 120)), width=1)
    fib = gaussian_filter(np.asarray(fib, np.float32) / 255, 0.6)
    height = tooth * 0.5 + fib * 2.2
    gy, gx = np.gradient(height)
    shade = 1 + 0.55 * (-gx * 0.7 - gy * 0.7)
    albedo = 1 - 0.07 * blot / 3 - 0.10 * fib
    return albedo.astype(np.float32), clamp(shade, 0.6, 1.4).astype(np.float32)


def sample(tex, u, v):
    """bilinear gather. tex (h,w,c) uint8, u=col, v=row -> float (n,c), valid"""
    h, w, c = tex.shape
    valid = (u >= 0) & (u < w - 1) & (v >= 0) & (v < h - 1)
    u = np.clip(u, 0, w - 1.001)
    v = np.clip(v, 0, h - 1.001)
    x0 = u.astype(np.int32)
    y0 = v.astype(np.int32)
    fx = (u - x0)[:, None].astype(np.float32)
    fy = (v - y0)[:, None].astype(np.float32)
    flat = tex.reshape(-1, c)
    i = y0 * w + x0
    a = flat[i].astype(np.float32)
    b = flat[i + 1].astype(np.float32)
    cc = flat[i + w].astype(np.float32)
    dd = flat[i + w + 1].astype(np.float32)
    out = (a + (b - a) * fx) * (1 - fy) + (cc + (dd - cc) * fx) * fy
    return out / 255.0, valid


# ------------------------------------------------------- global geometry
YY, XX = np.mgrid[0:H, 0:W].astype(np.float32)

# --- the page (world plane z=0). u = page column, v = height from page bottom.
PW, PH = 3600, 4400
ROWS = 12
ROW_V = [PH - 520 - i * 300 for i in range(ROWS)]
HERO = 6
GAP = 420  # the gap between hemistichs = the aisle
COLW = 1280
UC = PW / 2
U_RIGHT = UC + GAP / 2 + COLW / 2
U_LEFT = UC - GAP / 2 - COLW / 2

# Al-Mutanabbi, "Wa-harra qalbah" (public domain). Line 7 is the hero bayt.
VERSES = [
    ("واحر قلباه ممن قلبه شبم", "ومن بجسمي وحالي عنده سقم"),
    ("ما لي أكتم حبا قد برى جسدي", "وتدعي حب سيف الدولة الأمم"),
    ("يا أعدل الناس إلا في معاملتي", "فيك الخصام وأنت الخصم والحكم"),
    ("وما انتفاع أخي الدنيا بناظره", "إذا استوت عنده الأنوار والظلم"),
    ("سيعلم الجمع ممن ضم مجلسنا", "بأنني خير من تسعى به قدم"),
    ("أعيذها نظرات منك صادقة", "أن تحسب الشحم فيمن شحمه ورم"),
    ("أنا الذي نظرَ الأعمى إلى أدبي", "وأسمعتْ كلماتي مَن بهِ صَمَمُ"),
    ("أنام ملء جفوني عن شواردها", "ويسهر الخلق جراها ويختصم"),
    ("الخيل والليل والبيداء تعرفني", "والسيف والرمح والقرطاس والقلم"),
    ("إذا رأيت نيوب الليث بارزة", "فلا تظنن أن الليث يبتسم"),
    ("إن كان سركم ما قال حاسدنا", "فما لجرح إذا أرضاكم ألم"),
    ("يا من يعز علينا أن نفارقهم", "وجداننا كل شيء بعدكم عدم"),
]

# stage + light (world units = page px)
STAGE_V = PH - 180
MIC_V = PH + 230
STAGE_H = 110
MIC_H = 560
SPOT_SRC = np.array([UC, MIC_V - 350, 2700.0])
SEAT_H = 200  # height of a verse line once it stands up as a seat back
EYE_Z = 820.0
EYE_V = 150.0
ROW_DIV = 6  # seats per hemistich
FOC = 1500.0


def build_page():
    """channels: 0 hero word, 1 rest of hero bayt, 2 other rows, 3 paper shade, 4 paper albedo"""
    fnt = font("Amiri-Regular.ttf", 112)
    img = np.zeros((PH, PW, 5), np.uint8)
    hero_word = np.zeros((PH, PW), np.float32)
    hero_rest = np.zeros((PH, PW), np.float32)
    others = np.zeros((PH, PW), np.float32)
    word_box = None
    for i, (sadr, ajuz) in enumerate(VERSES):
        row = PH - ROW_V[i]
        ms = text_mask(PW, PH, sadr, fnt, (U_RIGHT, row))
        ma = text_mask(PW, PH, ajuz, fnt, (U_LEFT, row))
        if i == HERO:
            # isolate "كلماتي" inside the left hemistich by measuring the run
            full = fnt.getlength(ajuz, direction="rtl")
            before = fnt.getlength("وأسمعتْ ", direction="rtl")
            word = fnt.getlength("كلماتي", direction="rtl")
            right = U_LEFT + full / 2 - before
            left = right - word
            cols = np.arange(PW)
            sel = ((cols > left - 6) & (cols < right + 6))[None, :]
            hero_word += ma * sel
            hero_rest += ma * (~sel) + ms
            word_box = (left, right, row)
        else:
            others += ms + ma
    albedo, shade = paper_maps(PH, PW, 11, fibers=26000, fiber_len=90)
    img[..., 0] = (clamp(hero_word) * 255).astype(np.uint8)
    img[..., 1] = (clamp(hero_rest) * 255).astype(np.uint8)
    img[..., 2] = (clamp(others) * 255).astype(np.uint8)
    img[..., 3] = (clamp(shade / 2) * 255).astype(np.uint8)
    img[..., 4] = (clamp(albedo / 1.2) * 255).astype(np.uint8)
    mips = [img]
    for _ in range(4):
        mips.append(down(mips[-1].astype(np.float32), 2).round().astype(np.uint8))
    return mips, word_box


def build_titles():
    T = {}
    T["mult"] = text_mask(W, H, "الملتقى الوطني", font("IBMPlexSansArabic-Light.ttf", 62), (W / 2, 735))
    name_f = font("ReemKufi[wght].ttf", 196, 620)
    T["name"] = text_mask(W, H, "ماكوماداس", name_f, (W / 2, 935))
    sub_f = font("ReemKufi[wght].ttf", 112, 520)
    with_dots = text_mask(W, H, "للشعر", sub_f, (W / 2, 1150))
    dotless = text_mask(W, H, "للسعر", sub_f, (W / 2, 1150))
    dots = clamp(with_dots - dotless)
    lab, n = label(dots > 0.5)
    comps = []
    for k in range(1, n + 1):
        m = gaussian_filter((lab == k).astype(np.float32), 0.0)
        ys, xs = np.nonzero(m)
        comps.append((ys.mean(), xs.mean(), m))
    # land order: top dot first, then right->left
    comps.sort(key=lambda c: (round(c[0] / 10), -c[1]))
    # the dots carry the anti-aliased ring too
    ring = clamp(dots - sum(c[2] for c in comps))
    T["sub_base"] = dotless
    T["dots"] = [(c[0], c[1], clamp(gaussian_filter(c[2], 0.6) * 1.0 + ring * (gaussian_filter(c[2], 2) > 0.05)))
                 for c in comps]
    T["soon"] = text_mask(W, H, "قريبًا", font("IBMPlexSansArabic-Light.ttf", 50), (W / 2, 1418))
    for k in ("mult", "name", "sub_base", "soon"):
        ys, xs = np.nonzero(T[k] > 0.1)
        T[k + "_box"] = (xs.min(), xs.max(), ys.min(), ys.max())
    # ancient stone with the Roman name of the city carved in it
    cin = font("Cinzel[wght].ttf", 300, 500)
    lat = Image.new("L", (W * 2, H), 0)
    ImageDraw.Draw(lat).text((W, 960), "MACOMADES", font=cin, fill=255, anchor="mm")
    lat = np.asarray(lat, np.float32)[:, W // 2: W // 2 + W] / 255
    stone_h = fbm(H, W, [(1.5, 0.35), (5, 0.5), (22, 0.7), (90, 0.9)], 21)
    pits = (fbm(H, W, [(2.5, 1.0)], 22) > 2.2).astype(np.float32)
    carve = gaussian_filter(lat, 2.2) * 9.0
    height = stone_h - carve - pits * 1.2
    gy, gx = np.gradient(gaussian_filter(height, 0.8))
    T["stone_gx"] = gx.astype(np.float32)
    T["stone_gy"] = gy.astype(np.float32)
    T["stone_alb"] = (1 + 0.12 * fbm(H, W, [(60, 1.0)], 23) / 3 - 0.25 * gaussian_filter(lat, 1.0)).astype(np.float32)
    return T


def build_macro():
    alb, sh = paper_maps(int(H * 1.3), int(W * 1.3), 3, fibers=5000, fiber_len=110)
    return alb, sh, fbm(int(H * 1.3), int(W * 1.3), [(2, 0.6), (6, 1.0)], 4)


def load_assets():
    import pickle
    cache = os.path.join(OUT, ".cache", "assets.pkl")
    if os.path.exists(cache) and "--rebuild" not in sys.argv:
        with open(cache, "rb") as f:
            return pickle.load(f)
    print("building assets…", file=sys.stderr)
    a = (build_page(), build_titles(), build_macro())
    os.makedirs(os.path.dirname(cache), exist_ok=True)
    with open(cache, "wb") as f:
        pickle.dump(a, f, protocol=4)
    return a


(MIPS, WORD_BOX), TIT, (MACRO_ALB, MACRO_SH, MACRO_N) = load_assets()
ROW_SRC = None  # built lazily after functions are defined
DUST = rng.uniform(0, 1, (420, 4)).astype(np.float32)  # particles in the beam
MOTES = rng.uniform(0, 1, (60, 4)).astype(np.float32)  # floating paper dust (macro)
GRAIN_SEED = 1234


# ----------------------------------------------------- camera / projection
def row_rise(t, i):
    """0 = verse line lying on the page, 1 = standing seat back. far rows first."""
    return sstep(10.05 + 0.075 * i, 10.95 + 0.075 * i, t)


def build_row_sources():
    """per row/side: ink crop of the hemistich in the seat-back frame (200 x COLW)"""
    ink = np.maximum(np.maximum(MIPS[0][..., 0], MIPS[0][..., 1]), MIPS[0][..., 2]).astype(np.float32) / 255
    srcs = []
    for i in range(ROWS):
        top = int(PH - (ROW_V[i] + 90))
        row = []
        for side in (-1, 1):
            cl = int(UC + GAP / 2) if side > 0 else int(UC - GAP / 2 - COLW)
            row.append(ink[top:top + SEAT_H, cl:cl + COLW].copy())
        srcs.append(row)
    return srcs


def homography(dst, src):
    A, b = [], []
    for (x, y), (X, Y) in zip(dst, src):
        A.append([x, y, 1, 0, 0, 0, -X * x, -X * y]); b.append(X)
        A.append([0, 0, 0, x, y, 1, -Y * x, -Y * y]); b.append(Y)
    return np.linalg.solve(np.array(A, float), np.array(b, float))


def draw_rows(col, t, C, th, Isp, sweep_fn):
    divs = np.zeros(COLW, np.float32)
    for k in range(1, ROW_DIV):
        divs[int(k * COLW / ROW_DIV) - 3:int(k * COLW / ROW_DIV) + 3] = 1
    for i in range(ROWS):  # i=0 is the far row -> draw far to near
        rr = float(row_rise(t, i))
        if rr <= 0:
            continue
        phi = math.radians(90) * float(eio(rr))
        vb = ROW_V[i] - 110
        lightrow = math.exp(-max(0, MIC_V - vb) / 2300) * Isp
        sw = float(sweep_fn(np.array([ROW_V[i]], np.float32))[0])
        txt_l = 0.16 + 1.15 * lightrow + sw
        back_a = float(sstep(0.12, 0.55, rr))
        for s_i, side in enumerate((-1, 1)):
            cl = UC + GAP / 2 if side > 0 else UC - GAP / 2 - COLW
            cr = cl + COLW
            hh = SEAT_H
            topv, topz = vb + hh * math.cos(phi), hh * math.sin(phi)
            world = [(cl, topv, topz), (cr, topv, topz), (cr, vb, 0.0), (cl, vb, 0.0)]
            pr = [project(np.array(p), C, th) for p in world]
            if min(p[2] for p in pr) < 60:
                continue
            xs = [p[0] for p in pr]; ys = [p[1] for p in pr]
            x0, x1 = int(math.floor(min(xs))), int(math.ceil(max(xs)))
            y0, y1 = int(math.floor(min(ys))), int(math.ceil(max(ys)))
            if x1 < 0 or x0 >= W or y1 < 0 or y0 >= H or x1 - x0 < 1 or y1 - y0 < 1:
                continue
            # clip the patch to the frame
            cx0, cx1, cy0, cy1 = max(x0, 0), min(x1, W), max(y0, 0), min(y1, H)
            ink = ROW_SRC[i][s_i]
            scale = max(abs(xs[1] - xs[0]), abs(xs[2] - xs[3])) / COLW
            sw_, sh_ = COLW, SEAT_H
            if scale < 0.6:
                sw_ = max(8, int(COLW * scale * 1.5)); sh_ = max(4, int(SEAT_H * scale * 1.5))
                ink = np.asarray(Image.fromarray(ink).resize((sw_, sh_), Image.BOX))
            fy = sh_ / SEAT_H
            # vertical shading of the seat back + rim on top, dividers between seats
            yy = np.linspace(0, 1, sh_)[:, None]
            dv = np.asarray(Image.fromarray(divs[None, :].repeat(2, 0)).resize((sw_, 1), Image.BOX))
            back = (0.030 + 0.10 * lightrow * (1 - yy) ** 2) * (1 - 0.6 * dv)
            rim = np.exp(-(yy * sh_ / max(1.0, 4 * fy)) ** 2) * (0.35 + 1.1 * lightrow + 0.5 * sw) * back_a * (1 - 0.7 * dv)
            txt = ink * txt_l
            rgba = np.zeros((sh_, sw_, 4), np.float32)
            a = clamp(np.maximum(back_a * (1 - 0.0 * yy), ink * 0.95) + rim * 0)
            emit = (IVORY * 0.9)[None, None] * txt[..., None] + (WARM * 0.9)[None, None] * rim[..., None]
            rgb = back[..., None] * back_a * (1 - ink[..., None]) + emit
            rgba[..., :3] = rgb
            rgba[..., 3] = clamp(a + rim)
            src = [(0, 0), (sw_, 0), (sw_, sh_), (0, sh_)]
            dst = [(xs[k] - cx0, ys[k] - cy0) for k in range(4)]
            coef = homography(dst, src)
            chans = []
            for c in range(4):
                im = Image.fromarray(rgba[..., c].astype(np.float32), "F")
                chans.append(np.asarray(im.transform((cx1 - cx0, cy1 - cy0), Image.PERSPECTIVE, tuple(coef), Image.BILINEAR)))
            patch = np.stack(chans, -1)
            # out-of-quad pixels: PIL fills with 0 -> fully transparent; good.
            depth = float(np.mean([p[2] for p in pr]))
            coc = min(1.0, abs(1 / depth - 1 / 5200.0) * 9000)
            if coc > 0.15:
                patch = gaussian_filter(patch, (coc * 3.5, coc * 3.5, 0))
            pa = clamp(patch[..., 3:4])
            region = col[cy0:cy1, cx0:cx1]
            col[cy0:cy1, cx0:cx1] = region * (1 - pa) + patch[..., :3] * (pa > 0) + 0 * region
    return col


# ----------------------------------------------------- camera / projection (cont.)
def basis(theta):
    return (np.array([1.0, 0, 0]), np.array([0, math.sin(theta), -math.cos(theta)]),
            np.array([0, math.cos(theta), math.sin(theta)]))


def project(P, C, theta):
    r, fwd, upv = basis(theta)
    p = np.asarray(P, np.float64) - C
    z = p @ fwd
    return W / 2 + FOC * (p @ r) / z, H / 2 - FOC * (p @ upv) / z, z


def camera(t):
    """returns camera center C (3,), pitch theta"""
    uw = (WORD_BOX[0] + WORD_BOX[1]) / 2
    vw = PH - WORD_BOX[2]
    if t < 5.84:  # macro on the word
        z = keys(t, [4.11, 5.84], [600.0, 680.0])
        return np.array([uw + 30 * sstep(4.11, 5.84, t), vw - 10, z]), 0.0
    if t < 8.01:  # pull back: word -> bayt -> page
        e = eio((t - 5.84) / (7.75 - 5.84))
        e = float(e)
        z = math.exp(math.log(680) + (math.log(4400) - math.log(680)) * e)
        cx = uw + 30 + (UC - uw - 30) * e
        cy = vw - 10 + (2250 - vw + 10) * e
        return np.array([cx, cy, z]), 0.0
    if t < 9.78:  # stillness, page drifts towards the light
        e = float(sstep(8.01, 9.9, t))
        return np.array([UC, 2250 + 450 * e, 4400 + 160 * e]), 0.0
    # crane down into the hall: orbit around a look-at target
    e = float(eio((t - 9.78) / (11.95 - 9.78)))
    th = math.radians(80.0) * e
    T0 = np.array([UC, 2700.0, 0.0])
    th_end = math.radians(80.0)
    dist_end = EYE_Z / math.cos(th_end)
    T1 = np.array([UC, EYE_V + dist_end * math.sin(th_end), 0.0])
    dist = math.exp(math.log(4560) + (math.log(dist_end) - math.log(4560)) * e)
    T = T0 + (T1 - T0) * e
    fwd = np.array([0, math.sin(th), -math.cos(th)])
    C = T - fwd * dist
    if t > 12.06:  # "استعدوا" x2 — dolly down the aisle
        dy = keys(t, [12.06, 12.86, 13.45], [0.0, 520.0, 640.0])
        if t > 13.45:
            dy = 640 + 2300 * float(ein((t - 13.45) / (14.30 - 13.45), 2.4))
        C = C + np.array([0, dy, -40 * sstep(12.06, 14.3, t)])
        th = th + math.radians(3.0) * float(sstep(12.06, 14.3, t))
    return C, th


def spot_intensity(t):
    return keys(t, [8.9, 9.65, 9.78, 10.9, 12.06, 12.25, 13.45, 13.6, 14.23],
                [0.0, 0.32, 0.32, 1.0, 1.0, 1.18, 1.18, 1.38, 1.62])


# ------------------------------------------------------------- scene: macro
DOT_X, DOT_Y = 540.0, 880.0


def scene_macro(t):
    # slow push-in on the paper
    s = 1.0 + 0.07 * float(sstep(0.0, 3.8, t))
    h0, w0 = MACRO_ALB.shape
    cw, ch = W / s, H / s
    x0 = (w0 - cw) / 2 + 20 * t
    y0 = (h0 - ch) / 2 - 8 * t
    u = x0 + XX / s
    v = y0 + YY / s
    tex = np.stack([MACRO_ALB, MACRO_SH, MACRO_N], -1)
    tex8 = (clamp(tex / np.array([1.2, 2.0, 8.0]) + np.array([0, 0, 0.5])) * 255).astype(np.uint8)
    smp, _ = sample(tex8, u.ravel(), v.ravel())
    alb = smp[:, 0].reshape(H, W) * 1.2
    sh = smp[:, 1].reshape(H, W) * 2.0
    nz = (smp[:, 2].reshape(H, W) - 0.5) * 8.0

    # light: a narrow raking pool. rises with the first word, dies on "نفسها"
    L = keys(t, [0.0, 0.10, 0.9, 2.5, 3.75], [0.05, 0.05, 1.0, 1.0, 0.0])
    dx, dy = (XX - DOT_X), (YY - DOT_Y)
    pool = np.exp(-(dx * 0.8 + dy * 0.6) ** 2 / (2 * 330 ** 2) - (-dx * 0.6 + dy * 0.8) ** 2 / (2 * 620 ** 2))
    floor_glow = 0.04
    light = (floor_glow + 0.62 * pool) * L
    light = light * (1 + 0.25 * np.clip(-(dx + dy) / 700, -1, 1))  # raking from top-left
    col = PAPER[None, None] * (alb * sh * light)[..., None]

    # the calligrapher's point (نقطة): a rhombus pressed by the nib
    grow = float(eout((t - 0.12) / 0.55, 3))
    R = 70 * grow
    absorb = float(sstep(2.23, 3.5, t))
    if R > 0.5:
        ang = math.radians(-14)
        xr = (dx * math.cos(ang) + dy * math.sin(ang)) / s
        yr = (-dx * math.sin(ang) + dy * math.cos(ang)) / s
        # pen-made rhombus: slightly elongated, rounded, heavier at the entry corner
        ax_, ay_ = np.abs(xr - 0.22 * yr) / 0.92, np.abs(yr) / 1.18
        dist = (ax_ ** 1.06 + ay_ ** 1.06) ** (1 / 1.06) * (1 + 0.06 * np.tanh(xr / 40)) - 2.5 * np.exp(-((xr + R * 0.8) ** 2 + yr ** 2) / 400)
        edge = R + nz * (0.45 + 4.0 * absorb) + 6.0 * absorb
        a = clamp((edge - dist) / (1.1 + 4.0 * absorb))
        # feathered bleed into fibres while it is absorbed
        bleed = clamp((edge + 14 * absorb - dist) / 14) * 0.25 * absorb
        a = clamp(a + bleed * (nz > 0))
        wet = (1 - absorb) * float(sstep(0.12, 0.45, t))
        ink = INK * (1 + 0.6 * absorb)
        col = col * (1 - a[..., None]) + (ink * (0.5 + light[..., None] * 0.9)) * a[..., None]
        # glint travelling across the wet surface on "لحظات"
        gp = float(sstep(0.5, 1.6, t))
        gx = -R * 0.55 + R * 1.0 * gp
        gy = -R * 0.35 + R * 0.25 * gp
        spec = np.exp(-((xr - gx) ** 2 + (yr - gy) ** 2 * 3) / (2 * (R * 0.2 + 1) ** 2))
        g_amt = wet * (0.25 + 0.75 * float(sstep(0.5, 0.85, t))) * L
        col += (WARM * 0.75)[None, None] * (spec * a * g_amt)[..., None]
        # thin meniscus rim
        rim = np.exp(-((dist - edge + 2) ** 2) / 4) * wet * 0.12 * L
        col += WARM[None, None] * rim[..., None]

    # a single paper mote drifting through the light
    for m in MOTES[:14]:
        px = (m[0] * W + 25 * t * (m[2] - 0.5)) % W
        py = (m[1] * H - 18 * t) % H
        rr = 1.2 + 2.5 * m[3]
        d2 = (XX - px) ** 2 + (YY - py) ** 2
        if d2.min() < 400:
            col += (IVORY * 0.35)[None, None] * (np.exp(-d2 / (2 * rr ** 2)) * pool * L * 0.6)[..., None]
    dof = soft_blur(col, 5.0)
    focus = np.exp(-((XX - DOT_X) ** 2 + (YY - DOT_Y) ** 2 * 0.6) / (2 * 520 ** 2))
    col = col * focus[..., None] + dof * (1 - focus[..., None])
    return col


# ---------------------------------------------------- scene: page / hall
def scene_page(t):
    C, th = camera(t)
    r, fwd, upv = basis(th)
    X = XX - W / 2
    Y = H / 2 - YY
    dx = X
    dy = FOC * math.sin(th) + Y * math.cos(th)
    dz = -FOC * math.cos(th) + Y * math.sin(th)
    hit = dz < -1e-3
    tt = np.where(hit, -C[2] / np.where(hit, dz, -1), 1e9)
    u = C[0] + tt * dx
    v = C[1] + tt * dy
    depth = tt * FOC
    # texture footprint for mip selection
    fp_u = tt
    fp_v = np.abs(np.gradient(v, axis=0))
    lod = np.log2(np.maximum(np.maximum(fp_u, fp_v), 1.0))
    lod = np.where(hit, np.clip(lod, 0, len(MIPS) - 1.001), 0)

    flat_lod = lod.ravel()
    uu, vv = u.ravel(), (PH - v).ravel()
    smp = np.zeros((uu.size, 5), np.float32)
    inside = np.zeros(uu.size, bool)
    base = np.floor(flat_lod).astype(int)
    for k in range(len(MIPS) - 1):
        idx = np.nonzero(base == k)[0]
        if idx.size == 0:
            continue
        f = (flat_lod[idx] - k)[:, None]
        a, va = sample(MIPS[k], uu[idx] / 2 ** k, vv[idx] / 2 ** k)
        b, _ = sample(MIPS[k + 1], uu[idx] / 2 ** (k + 1), vv[idx] / 2 ** (k + 1))
        smp[idx] = a * (1 - f) + b * f
        inside[idx] = va
    smp[~inside] = np.array([0, 0, 0, 0.5, 1 / 1.2])
    smp = smp.reshape(H, W, 5)
    word, rest, others = smp[..., 0], smp[..., 1], smp[..., 2]
    shade = smp[..., 3] * 2.0
    alb = smp[..., 4] * 1.2
    pagein = inside.reshape(H, W) & hit

    # ---- reveal logic
    wl, wr, wrow = WORD_BOX
    # "تبدأ بكلمة": the word writes itself right->left on the syllables
    prog = keys(t, [4.11, 4.37, 4.59, 4.72, 4.99, 5.30], [0.0, 0.24, 0.47, 0.63, 0.86, 1.0])
    xedge = wr - (wr - wl + 30) * prog
    wnoise = 10 * np.sin(v * 0.11 + u * 0.03)
    wa = clamp((u - xedge + wnoise) / 16) * (prog > 0)
    word_a = word * wa
    fresh = clamp(1 - (u - xedge) / 90) * word_a * (1 - float(sstep(5.2, 6.3, t)))
    # "ثم تتحول إلى فكرة": the bayt grows outward from the word, then the qasida
    uw = (wl + wr) / 2
    grow_r = 2600 * float(eout((t - 5.90) / 1.0, 2))
    rest_a = rest * clamp((grow_r - np.abs(u - uw)) / 260)
    row_idx = np.clip(np.round((PH - 520 - v) / 300), 0, ROWS - 1)
    dist_rows = np.abs(row_idx - HERO)
    row_start = 6.25 + 0.13 * dist_rows
    others_a = others * clamp((t - row_start) / 0.55)
    ink = clamp(word_a + rest_a + others_a)

    # ---- day (paper) look
    pool = np.exp(-((XX - 540) ** 2) / (2 * 620 ** 2) - ((YY - 900) ** 2) / (2 * 900 ** 2))
    page_L = keys(t, [4.0, 4.35, 7.6, 8.4, 9.3], [0.0, 0.78, 0.78, 0.45, 0.10])
    pool = pool ** 1.6
    dayL = (0.05 + 0.95 * pool) * page_L * (1 + 0.18 * np.clip((XX - YY * 0.3) / 900, -1, 1))
    paper_col = PAPER[None, None] * (alb * shade * dayL * pagein)[..., None]
    day = paper_col * (1 - ink[..., None]) + (INK * dayL[..., None] * 1.5) * ink[..., None]
    day += (WARM * 0.22)[None, None] * (fresh * dayL)[..., None]  # wet ink sheen

    # ---- night (hall) look: the ink becomes light, the paper becomes the floor
    nt = float(sstep(8.05, 9.3, t))
    Isp = spot_intensity(t)
    pool_w = np.exp(-((u - UC) ** 2 + ((v - MIC_V) * 1.0) ** 2) / (2 * 430 ** 2)) * Isp
    spill = np.exp(-np.clip(MIC_V - v, 0, None) / 2300) * Isp * 0.55

    def sweep_fn(vv):
        out = 0.0 * vv
        if t > 12.0:
            cy = C[1]
            s1 = cy + (MIC_V - cy) * float(eio((t - 12.06) / 1.1))
            out = out + np.exp(-((vv - s1) / 320) ** 2) * 1.1 * float(sstep(12.06, 12.2, t) * (1 - sstep(13.0, 13.4, t)))
            s2 = cy + (MIC_V - cy) * float(eio((t - 13.45) / 0.75))
            out = out + np.exp(-((vv - s2) / 360) ** 2) * 1.4 * float(sstep(13.45, 13.55, t))
        return out

    sweep = sweep_fn(v)
    stood = row_rise(t, row_idx)
    ink = ink * (1 - sstep(0.0, 0.12, stood))
    floor_tex = (0.65 + 0.35 * alb * shade)
    edge_in = clamp(np.minimum(np.minimum(u, PW - u), np.minimum(v, PH - v)) / 500)
    floor = FLOOR[None, None] * (floor_tex * ((0.10 + 0.9 * spill ** 1.5) * edge_in + 6.0 * pool_w))[..., None] * hit[..., None]
    ember = keys(t, [8.0, 9.0, 9.78], [0.0, 0.10, 0.10])
    text_light = 0.04 + ember + 0.55 * spill + 1.4 * pool_w + sweep
    night = floor + (IVORY * 0.9)[None, None] * (ink * text_light)[..., None]
    col = day * (1 - nt) + night * nt

    # ---- hall geometry: each verse line stands up and becomes a row of seats
    stage_on = float(sstep(10.6, 11.6, t))
    if th > 0.12:
        col = draw_rows(col, t, C, th, Isp, sweep_fn)
        over = Image.new("RGBA", (W * 2, H * 2), (0, 0, 0, 0))
        dr = ImageDraw.Draw(over)

        def P2(p):
            x, y, z = project(p, C, th)
            return (x * 2, y * 2), z

        if stage_on > 0:
            q = [(UC - 1750, STAGE_V, 0), (UC + 1750, STAGE_V, 0), (UC + 1750, STAGE_V, STAGE_H), (UC - 1750, STAGE_V, STAGE_H)]
            pts = [P2(p) for p in q]
            if min(z for _, z in pts) > 50:
                dr.polygon([p for p, _ in pts], fill=(5, 5, 6, int(255 * stage_on)))
                dr.line([pts[3][0], pts[2][0]], fill=(255, 220, 170, int(140 * stage_on * min(1, Isp))), width=3)
            # microphone on its stand
            base = np.array([UC, MIC_V, STAGE_H])
            top = base + np.array([0, -30, MIC_H])
            (bx, by), bz = P2(base)
            (tx, ty), tz = P2(top)
            if bz > 50:
                sc = 2 * FOC / bz
                a = int(255 * stage_on)
                dr.ellipse([bx - 80 * sc, by - 16 * sc, bx + 80 * sc, by + 16 * sc], fill=(14, 13, 12, a))
                dr.ellipse([bx - 80 * sc, by - 16 * sc, bx + 80 * sc, by + 2 * sc], outline=(200, 170, 130, a // 3), width=max(1, int(2 * sc)))
                dr.line([(bx, by), (tx, ty)], fill=(22, 20, 18, a), width=max(2, int(11 * sc)))
                ang = math.radians(-24)
                hx, hy = math.sin(ang) * 95 * sc, -math.cos(ang) * 95 * sc
                cx2, cy2 = tx + hx * 0.55, ty + hy * 0.55
                hd = (cx2 + hx * 0.45, cy2 + hy * 0.45)
                dr.line([(tx, ty), hd], fill=(26, 24, 22, a), width=max(3, int(40 * sc)))
                dr.ellipse([hd[0] - 20 * sc, hd[1] - 20 * sc, hd[0] + 20 * sc, hd[1] + 20 * sc], fill=(26, 24, 22, a))
                g = int(min(255, 230 * Isp * stage_on))
                dr.ellipse([hd[0] - 12 * sc, hd[1] - 17 * sc, hd[0] + 8 * sc, hd[1] - 6 * sc], fill=(255, 236, 205, g))
                dr.line([(tx + 6 * sc, ty + 4 * sc), (hd[0] + 10 * sc, hd[1] - 4 * sc)], fill=(255, 226, 185, g // 2), width=max(1, int(3 * sc)))
                dr.line([(bx + 4 * sc, by - 4), (tx + 4 * sc, ty)], fill=(255, 220, 170, int(min(255, 90 * Isp * stage_on))), width=max(1, int(2 * sc)))
        ov = np.asarray(over.resize((W, H), Image.LANCZOS), np.float32) / 255
        a = ov[..., 3:4]
        col = col * (1 - a) + ov[..., :3] * a

    # ---- volumetric spot cone (low-res, smoke-modulated)
    if Isp > 0:
        lw, lh = W // 4, H // 4
        ly, lx = np.mgrid[0:lh, 0:lw].astype(np.float32) * 4
        (sx, sy, sz) = project(SPOT_SRC, C, th)
        (bx, by, bz) = project(np.array([UC, MIC_V, STAGE_H]), C, th)
        if bz > 50:
            if sz <= 50:  # source behind camera: start the beam above the frame
                sx, sy = bx, -400.0
            ax, ay = bx - sx, by - sy
            L2 = ax * ax + ay * ay + 1e-6
            pr = clamp(((lx - sx) * ax + (ly - sy) * ay) / L2, 0, 1.15)
            px, py = sx + pr * ax, sy + pr * ay
            dperp = np.sqrt((lx - px) ** 2 + (ly - py) ** 2)
            rad_bot = 520 * FOC / bz
            rad = 12 + pr * rad_bot
            cone = np.exp(-(dperp / rad) ** 2 * 2.2) * (pr < 1.0) * (0.35 + 0.65 * pr)
            smoke = 0.75 + 0.25 * np.sin(lx * 0.013 + t * 0.6 + np.sin(ly * 0.009 - t * 0.4) * 2)
            vis = float(clamp((th - 0.1) / 0.8)) * 0.55 + 0.12
            beam = cone * smoke * Isp * vis * nt
            beam = gaussian_filter(beam, 2)
            col += (WARM * 0.32)[None, None] * up(beam, H, W)[..., None]
            haze = np.exp(-((lx - bx) ** 2) / (2 * (rad_bot * 1.6) ** 2) - ((ly - by) ** 2) / (2 * (rad_bot * 0.9) ** 2))
            col += (WARM * 0.07 * Isp * nt)[None, None] * up(gaussian_filter(haze, 3), H, W)[..., None]
            # dust in the beam
            dust = np.zeros((H, W), np.float32)
            for p in DUST:
                ph = p[2] * 6.28
                ww = (p[0] - 0.5) * 2
                hgt = p[1]
                P = np.array([UC + ww * 420 * (0.3 + 0.7 * (1 - hgt)) + 30 * math.sin(t * 0.5 + ph),
                              MIC_V + (p[3] - 0.5) * 500, STAGE_H + hgt * 2000 + 40 * math.sin(t * 0.3 + ph)])
                x, y, z = project(P, C, th)
                if z < 80 or not (0 <= x < W and 0 <= y < H):
                    continue
                inten = float(np.interp(y, ly[:, 0], beam[:, min(lw - 1, int(x / 4))]))
                if inten < 0.02:
                    continue
                xi, yi = int(x), int(y)
                dust[yi, xi] += inten * (0.6 + 0.8 * p[3]) * min(3.0, 3000 / z)
            dust = gaussian_filter(dust, 1.3) * 9
            col += (WARM * 0.9)[None, None] * dust[..., None]

    # ---- depth of field
    if th > 0.05:
        focus_d = 5200.0
        coc = np.clip(np.abs(1 / np.maximum(depth, 1) - 1 / focus_d) * 9000, 0, 1) * hit
        coc = np.maximum(coc, (~hit) * 0.6)
        blurred = soft_blur(col, 6)
        col = col * (1 - coc[..., None]) + blurred * coc[..., None]
    else:
        # macro shallow focus on the word (top-down)
        z = C[2]
        amt = float(clamp((1200 - z) / 600))
        if amt > 0:
            fo = np.exp(-((YY - 960) ** 2) / (2 * 330 ** 2))
            blurred = soft_blur(col, 7)
            k = (1 - fo) * amt
            col = col * (1 - k[..., None]) + blurred * k[..., None]

    # cut to black at the end of the second "استعدوا"
    col *= 1 - float(sstep(14.17, 14.30, t))
    return col


# ----------------------------------------------------------- scene: titles
def scene_titles(t):
    # stone surface: dark, only a slow raking light reveals the Roman name
    lx_pos = keys(t, [14.6, 16.5, 19.5], [1300.0, 1100.0, -300.0])
    Lst = keys(t, [14.6, 15.2, 16.5, 16.6, 18.4, 19.2], [0.0, 0.05, 0.05, 0.16, 0.13, 0.06])
    raking = np.exp(-((XX - lx_pos) ** 2) / (2 * 520 ** 2)) * 0.85 + 0.15
    sh = 1 + 2.4 * (-TIT["stone_gx"] * 0.9 + TIT["stone_gy"] * 0.2)
    stone = (np.array([0.62, 0.58, 0.52]) * 0.5)[None, None] * (TIT["stone_alb"] * clamp(sh, 0.2, 2.2) * raking * Lst)[..., None]
    col = stone.astype(np.float32)

    def paint(mask, amount, color, blur=0.0, dy=0.0):
        nonlocal col
        m = mask
        if dy:
            m = np.roll(m, int(round(dy)), axis=0)
        if blur > 0.3:
            m = gaussian_filter(m, blur)
        a = clamp(m * amount)[..., None]
        col = col * (1 - a) + color[None, None] * a

    # "الملتقى الوطني": light-slit reveal right -> left
    x0, x1, _, _ = TIT["mult_box"]
    pr = keys(t, [14.78, 15.35, 16.05], [0.0, 0.55, 1.0])
    edge = x1 + 40 - (x1 - x0 + 120) * pr
    rev = clamp((XX - edge) / 70)
    hot = np.exp(-((XX - edge) / 45) ** 2) * (pr < 1) * (pr > 0)
    mult_a = keys(t, [14.78, 16.1, 16.55, 17.2], [1.0, 1.0, 0.72, 0.62])
    paint(TIT["mult"] * rev, mult_a, IVORY, blur=2.5 * (1 - pr), dy=-12 * float(sstep(16.1, 17.5, t)))
    col += (WARM * 0.8)[None, None] * (gaussian_filter(TIT["mult"], 1.5) * hot * 0.9)[..., None]
    # slit of light itself (thin vertical glint)
    if 0 < pr < 1:
        slit = np.exp(-((XX - edge) / 3.0) ** 2) * np.exp(-((YY - 735) / 60) ** 2) * 0.35
        col += WARM[None, None] * slit[..., None]

    # ---- MAIN REVEAL: ماكوماداس
    if t >= 16.5:
        nx0, nx1, ny0, ny1 = TIT["name_box"]
        pr = keys(t, [16.56, 16.69, 16.88, 17.01, 17.19, 17.48], [0.0, 0.16, 0.40, 0.60, 0.82, 1.0])
        edge = nx1 + 30 - (nx1 - nx0 + 90) * pr
        rev = clamp((XX - edge) / 55)
        hot = np.exp(-((XX - edge) / 60) ** 2) * (pr < 1)
        settle = 1 + 0.045 * (1 - float(eout((t - 16.56) / 2.2, 3)))
        m = TIT["name"]
        if settle != 1:
            # scale about the name centre
            cy, cx = 935.0, W / 2
            uu = (XX - cx) / settle + cx
            vv = (YY - cy) / settle + cy
            m8 = (m * 255).astype(np.uint8)[..., None]
            m = sample(m8, uu.ravel(), vv.ravel())[0].reshape(H, W)
        blur = 6 * clamp((XX - edge) / -260 + 1) * (pr < 1)
        sharp = m * rev
        soft = soft_blur(sharp, 4)
        mm = sharp * (1 - clamp(blur / 6)) + soft * clamp(blur / 6)
        paint(mm, 1.0, IVORY)
        col += (WARM * 1.1)[None, None] * (soft_blur(m, 3) * hot)[..., None]
        # metallic sheen passing over the finished name
        sp = keys(t, [17.6, 18.9], [nx1 + 200.0, nx0 - 200.0])
        sheen = np.exp(-((XX + (YY - 935) * 0.35 - sp) / 70) ** 2) * float(sstep(17.55, 17.7, t))
        col += (BRONZE * 0.35)[None, None] * (m * sheen)[..., None]
        # impact bloom behind the name
        flash = float(sstep(16.54, 16.62, t) * (1 - sstep(16.62, 17.6, t)))
        if flash > 0:
            halo = np.exp(-((XX - W / 2) ** 2) / (2 * 420 ** 2) - ((YY - 935) ** 2) / (2 * 180 ** 2))
            col += (WARM * 0.10 * flash)[None, None] * halo[..., None]

    # ---- للشعر: dotless body first, then the three dots land on the "ش" sound
    if t >= 17.5:
        a = float(sstep(17.52, 17.80, t))
        paint(TIT["sub_base"], a * 0.92, IVORY, blur=3 * (1 - a), dy=14 * (1 - a))
        land = [17.62, 17.69, 17.77]
        for k, (cy, cx, dm) in enumerate(TIT["dots"]):
            tl = land[k] if k < 3 else land[-1]
            p = float(sstep(tl - 0.10, tl, t))
            if p <= 0:
                continue
            drop = -38 * (1 - p) ** 2
            paint(dm, p * 0.95, IVORY, dy=drop)
            # the last dot is the same wet point from the opening: a brief glint
            if k == len(TIT["dots"]) - 1:
                g = float(sstep(tl, tl + 0.05, t) * (1 - sstep(tl + 0.08, tl + 0.6, t)))
                col += (WARM * 0.9 * g)[None, None] * (gaussian_filter(dm, 3) * 1.4)[..., None]

    # bronze hairline + "قريبًا"
    if t >= 18.2:
        ln = float(eout((t - 18.25) / 0.5))
        hl = (np.abs(YY - 1300) < 0.9) * (np.abs(XX - W / 2) < 70 * ln)
        col += (BRONZE * 0.55)[None, None] * gaussian_filter(hl.astype(np.float32), 0.5)[..., None]
    if t >= 18.7:
        a = float(sstep(18.72, 19.02, t))
        paint(TIT["soon"], a * 0.82, IVORY, blur=3 * (1 - a))

    # breathe, then fade out
    col *= 1 - float(sstep(19.12, DUR, t)) * 0.97
    col *= float(sstep(14.70, 14.80, t)) * 0.0 + 1.0
    return col


# ------------------------------------------------------------- finishing
def finish(col, t, fi):
    col = np.maximum(col, 0)
    # halation / bloom (quarter res)
    hi = np.maximum(col - 0.55, 0)
    if hi.max() > 0:
        small = down(hi, 4)
        small = gaussian_filter(small, (7, 7, 0)) * 0.9 + gaussian_filter(small, (2, 2, 0)) * 0.6
        col = col + up(small, H, W) * np.array([1.0, 0.82, 0.62])[None, None]
    # vignette
    vig = 1 - 0.38 * (((XX - W / 2) / (W * 0.75)) ** 2 + ((YY - H / 2) / (H * 0.62)) ** 2)
    col = col * clamp(vig, 0.4, 1)[..., None]
    # filmic tone: soft shoulder, lifted black
    col = col / (1 + 0.35 * col)
    col = col * 1.25
    col = BLACK[None, None] + col * (1 - BLACK[None, None])
    # split tone: cool shadows, warm highlights
    lum = col.mean(-1, keepdims=True)
    col = col + np.array([-0.004, 0.0, 0.006]) * (1 - lum) + np.array([0.012, 0.004, -0.010]) * lum
    # grain (luma-weighted, slightly soft)
    g = np.random.default_rng(GRAIN_SEED + fi).standard_normal((H // 2, W // 2)).astype(np.float32)
    g = up(g, H, W)
    col = col + (g * (0.020 + 0.018 * (1 - lum[..., 0])))[..., None]
    col = clamp(col)
    return (col ** (1 / 1.0) * 255 + 0.5).astype(np.uint8)


def frame(fi):
    global ROW_SRC
    if ROW_SRC is None:
        ROW_SRC = build_row_sources()
    t = fi / FPS
    if t < 4.08:
        col = scene_macro(t)
    elif t < 14.5:
        col = scene_page(t)
    else:
        col = scene_titles(t)
    return finish(col.astype(np.float32), t, fi)


def main():
    os.makedirs(OUT, exist_ok=True)
    if "--still" in sys.argv:
        for a in sys.argv[sys.argv.index("--still") + 1:]:
            t = float(a)
            Image.fromarray(frame(int(round(t * FPS)))).save(os.path.join(OUT, "still_%05.2f.png" % t))
        return
    out = os.path.join(OUT, "frames.mp4")
    ff = subprocess.Popen(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
                           "-s", "%dx%d" % (W, H), "-r", str(FPS), "-i", "-",
                           "-c:v", "libx264", "-preset", "slow", "-crf", "14", "-pix_fmt", "yuv420p",
                           "-tune", "grain", out], stdin=subprocess.PIPE)
    with Pool(int(os.environ.get("JOBS", os.cpu_count()))) as pool:
        for i, fr in enumerate(pool.imap(frame, range(N), chunksize=2)):
            ff.stdin.write(fr.tobytes())
            if i % 30 == 0:
                print("frame %d/%d" % (i, N), file=sys.stderr)
    ff.stdin.close()
    ff.wait()


if __name__ == "__main__":
    main()
