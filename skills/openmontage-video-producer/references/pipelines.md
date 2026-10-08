# مرجع OpenMontage

## البنية الداخلية

```
OpenMontage/
├── pipeline_defs/      # تعريفات خطوط الإنتاج (YAML): المراحل، الأدوات، معايير المراجعة
├── skills/
│   ├── pipelines/      # مهارات "المخرج" لكل مرحلة في كل خط إنتاج
│   ├── core/ creative/ meta/
│   └── INDEX.md
├── tools/              # أدوات Python + tool_registry (اختيار المزوّد بتقييم 7 أبعاد)
├── remotion-composer/  # تصيير React (Remotion)
├── styles/ schemas/ lib/
├── AGENT_GUIDE.md      # عقد العمل للوكيل — يُقرأ أولاً
└── .env                # مفاتيح API الاختيارية
```

## كيف يعمل

```
المستخدم: "اصنع فيديو شرح عن تكوّن الثقوب السوداء"
  ↓ الوكيل يقرأ manifest خط الإنتاج (YAML)
  ↓ الوكيل يقرأ مهارة المرحلة (Markdown)
  ↓ يستدعي أدوات Python (اختيار مزوّد مُقيَّم)
  ↓ مراجعة ذاتية (schema، playbook، جودة)
  ↓ حفظ الحالة JSON (قابل للاستئناف + سجل قرارات + تكلفة)
  ↓ عرض على المستخدم للموافقة
  ↓ بوابة تحقق قبل التركيب
  ↓ التصيير (Remotion أو HyperFrames أو FFmpeg)
```

## ما تحصل عليه بدون أي مفتاح API

| القدرة | الأداة المجانية |
|--------|-----------------|
| التعليق الصوتي | Piper TTS (محلي) |
| لقطات مفتوحة | Archive.org + NASA + Wikimedia Commons |
| تركيب React | Remotion |
| تركيب HTML/GSAP | HyperFrames |
| ما بعد الإنتاج | FFmpeg |
| الترجمة | مدمجة بتوقيت على مستوى الكلمة |

## جميع مفاتيح API

```bash
# بوابة الصور والفيديو
FAL_KEY=
ATLASCLOUD_API_KEY=
# Kling الرسمي
KLING_API_KEY=
KLING_API_BASE_URL=
# لقطات مجانية
PEXELS_API_KEY=
PIXABAY_API_KEY=
UNSPLASH_ACCESS_KEY=
# موسيقى
SUNO_API_KEY=
# صوت وصور
ELEVENLABS_API_KEY=
OPENAI_API_KEY=
XAI_API_KEY=
GOOGLE_API_KEY=
# مزوّدو فيديو إضافيون
ARK_API_KEY=
HEYGEN_API_KEY=
RUNWAY_API_KEY=
```

## توليد فيديو محلي مجاني (يتطلب GPU)

```bash
make install-gpu
# ثم في .env:
VIDEO_GEN_LOCAL_ENABLED=true
VIDEO_GEN_LOCAL_MODEL=wan2.2-ti2v-5b   # أو wan2.1-1.3b, wan2.1-14b, hunyuan-1.5, ltx2-local, cogvideo-5b
```

## حل المشاكل

- **Windows**: إذا فشل `npm install` بخطأ `ERR_INVALID_ARG_TYPE` استخدم `npx --yes npm install`
- **فشل piper-tts**: النظام يتحول تلقائياً إلى مزوّد TTS سحابي إن وُجد مفتاح
- **أول تصيير بطيء**: HyperFrames يُجلب عند أول استخدام (~20MB)
