#!/usr/bin/env bash
# تثبيت OpenMontage — نظام إنتاج فيديو وكيلي مفتوح المصدر
# المصدر: https://github.com/calesthio/OpenMontage (رخصة AGPLv3)
#
# الاستخدام:
#   bash install_openmontage.sh [مجلد_التثبيت]
# الافتراضي: ./OpenMontage
set -euo pipefail

REPO_URL="https://github.com/calesthio/OpenMontage.git"
TARGET="${1:-OpenMontage}"

need() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "✗ الأداة '$1' غير مثبتة. $2" >&2
    exit 1
  fi
}

echo "==> التحقق من المتطلبات..."
need git    "ثبّتها من https://git-scm.com"
need python3 "مطلوب Python 3.10+ من https://www.python.org/downloads/"
need node   "مطلوب Node.js 18+ من https://nodejs.org"
need npm    "يأتي مع Node.js"
need ffmpeg "macOS: brew install ffmpeg | Ubuntu: sudo apt install ffmpeg"

python3 - <<'PY'
import sys
if sys.version_info < (3, 10):
    sys.exit(f"✗ مطلوب Python 3.10+ (الموجود {sys.version.split()[0]})")
PY

NODE_MAJOR="$(node -p 'process.versions.node.split(".")[0]')"
if [ "$NODE_MAJOR" -lt 18 ]; then
  echo "✗ مطلوب Node.js 18+ (الموجود $(node -v))" >&2
  exit 1
fi

if [ -d "$TARGET/.git" ]; then
  echo "==> OpenMontage موجود مسبقاً في $TARGET — جلب آخر تحديث..."
  git -C "$TARGET" pull --ff-only
else
  echo "==> استنساخ OpenMontage إلى $TARGET..."
  git clone "$REPO_URL" "$TARGET"
fi

cd "$TARGET"

if command -v make >/dev/null 2>&1; then
  echo "==> تشغيل make setup..."
  make setup
else
  echo "==> make غير متوفر — تثبيت يدوي..."
  python3 -m venv .venv
  # shellcheck disable=SC1091
  source .venv/bin/activate
  python -m pip install -r requirements.txt
  (cd remotion-composer && npm install)
  python -m pip install piper-tts || echo "  [تخطّي] فشل تثبيت piper-tts — سيتم استخدام TTS سحابي"
  [ -f .env ] || cp .env.example .env
fi

echo ""
echo "✓ تم تثبيت OpenMontage في: $(pwd)"
echo ""
echo "الخطوات التالية:"
echo "  1. (اختياري) أضف مفاتيح API في الملف .env — كلها اختيارية"
echo "  2. افتح المجلد في وكيل برمجي (Claude Code / Cursor / Codex / Kimi CLI)"
echo "  3. اطلب مثلاً: \"Make a 60-second animated explainer about how neural networks learn\""
