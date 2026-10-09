#!/usr/bin/env bash
# التحقق من تثبيت OpenMontage
# الاستخدام: bash verify_openmontage.sh [مجلد_OpenMontage]   (الافتراضي: ./OpenMontage)
set -uo pipefail

DIR="${1:-OpenMontage}"
PASS=0; FAIL=0; WARN=0
ok()   { echo "  ✓ $1"; PASS=$((PASS+1)); }
bad()  { echo "  ✗ $1"; FAIL=$((FAIL+1)); }
warn() { echo "  ⚠ $1"; WARN=$((WARN+1)); }

if [ ! -f "$DIR/AGENT_GUIDE.md" ]; then
  echo "✗ لم يتم العثور على OpenMontage في: $DIR" >&2
  exit 1
fi
cd "$DIR"
PY=.venv/bin/python
export PATH="$PWD/.venv/bin:$PATH"

echo "== المتطلبات"
command -v ffmpeg  >/dev/null && ok "FFmpeg: $(ffmpeg -version | head -1 | cut -d' ' -f3)" || bad "FFmpeg غير مثبت"
command -v ffprobe >/dev/null && ok "FFprobe" || bad "FFprobe غير مثبت"
if command -v node >/dev/null; then
  NM="$(node -p 'process.versions.node.split(".")[0]')"
  if   [ "$NM" -ge 22 ]; then ok "Node.js $(node -v)"
  elif [ "$NM" -ge 18 ]; then warn "Node.js $(node -v) — يكفي لـ Remotion، HyperFrames يتطلب 22+"
  else bad "Node.js $(node -v) — مطلوب 18+"; fi
else bad "Node.js غير مثبت"; fi

echo "== بيئة Python"
if [ -x "$PY" ]; then
  ok "البيئة الافتراضية .venv ($($PY --version))"
  $PY -c "import yaml, jsonschema" 2>/dev/null && ok "مكتبات Python الأساسية" || bad "مكتبات Python ناقصة — نفّذ: make setup"
else
  bad "لا توجد .venv — نفّذ: make setup"
fi
[ -f .env ] && ok "ملف .env موجود" || warn "ملف .env غير موجود — نفّذ: cp .env.example .env"

echo "== Remotion"
[ -d remotion-composer/node_modules/remotion ] && ok "حزم Remotion مثبتة" || bad "Remotion غير مثبت — نفّذ: cd remotion-composer && npm install"

echo "== التعليق الصوتي المجاني (Piper)"
command -v piper >/dev/null && ok "أمر piper متاح" || warn "piper غير مثبت — سيُستخدم TTS سحابي"
if ls ./*.onnx >/dev/null 2>&1; then ok "نموذج صوت Piper: $(ls ./*.onnx | xargs -n1 basename | tr '\n' ' ')"
else warn "لا يوجد نموذج صوت Piper — نفّذ: $PY -m piper.download_voices en_US-lessac-medium"; fi

echo "== سجل الأدوات"
if [ -x "$PY" ]; then
  $PY - <<'PYCODE' || bad "فشل تحميل سجل الأدوات"
from tools.tool_registry import registry
registry.discover()
env = registry.support_envelope()
avail = [n for n, t in env.items() if t.get("status") == "available"]
print(f"  ✓ الأدوات المتاحة: {len(avail)} من {len(env)}")
for key in ("piper_tts", "video_compose", "hyperframes_compose", "subtitle_gen", "audio_mixer"):
    if key in env:
        mark = "✓" if env[key].get("status") == "available" else "·"
        print(f"    {mark} {key}: {env[key].get('status')}")
PYCODE
fi

echo ""
echo "النتيجة: $PASS ناجح، $WARN تحذير، $FAIL فشل"
[ "$FAIL" -eq 0 ] && echo "✓ OpenMontage جاهز للاستخدام" || { echo "✗ يوجد مشاكل — راجع ما سبق"; exit 1; }
