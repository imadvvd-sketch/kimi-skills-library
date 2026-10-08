---
name: openmontage-video-producer
description: "إنتاج فيديوهات كاملة باستخدام OpenMontage — نظام إنتاج فيديو وكيلي مفتوح المصدر. استخدم عندما يطلب المستخدم إنشاء فيديو، فيديو شرح (explainer)، مونتاج وثائقي، مقاطع قصيرة من فيديو طويل، دبلجة وترجمة فيديو، فيديو منتج، أو تحويل بودكاست إلى فيديو. يدعم: البحث، كتابة السيناريو، التعليق الصوتي، الموسيقى، الترجمة، والتصيير النهائي."
---

# منتج الفيديو — OpenMontage 🎬

## نظرة عامة

[OpenMontage](https://github.com/calesthio/OpenMontage) هو أول نظام إنتاج فيديو **وكيلي** مفتوح المصدر.
لا يوجد فيه منسّق برمجي — **الوكيل الذكي نفسه هو المخرج**: يقرأ تعريف خط الإنتاج (YAML)،
ثم مهارة المرحلة (Markdown)، ثم يستدعي أدوات Python، ويراجع عمله ويطلب موافقة المستخدم في كل قرار إبداعي.

- **+10 خطوط إنتاج** (pipelines) — شرح، رسوم متحركة، وثائقي، سينمائي، بودكاست، دبلجة...
- **+100 أداة** — توليد فيديو وصور، TTS، موسيقى، ميكساج، ترجمة، تحليل
- **+60 مزوّد** — APIs سحابية، نماذج محلية، مكتبات مجانية
- **يعمل بدون أي مفتاح API** (Piper TTS + Archive.org/NASA/Wikimedia + Remotion + FFmpeg)

> الرخصة: **AGPLv3** — المشروع لا يُنسخ داخل هذه المكتبة، بل يُثبَّت من مستودعه الأصلي.

## متى تستخدم هذه المهارة

- فيديو شرح تعليمي متحرك عن أي موضوع
- مونتاج وثائقي من لقطات حقيقية مجانية (بدون توليد مدفوع)
- تقطيع فيديو/بودكاست طويل إلى مقاطع قصيرة للسوشيال ميديا
- ترجمة ودبلجة فيديو موجود إلى لغات أخرى (منها العربية)
- فيديو منتج / إعلان سينمائي / عرض شاشة لبرنامج

## الخطوة 1: التثبيت

### المتطلبات
- Python 3.10+
- FFmpeg
- Node.js 18+
- وكيل برمجي: Claude Code أو Cursor أو Codex أو Copilot أو Kimi CLI

### التثبيت التلقائي (موصى به)

```bash
bash scripts/install_openmontage.sh            # يثبّت في ./OpenMontage
bash scripts/install_openmontage.sh ~/OpenMontage
```

### التثبيت اليدوي

```bash
git clone https://github.com/calesthio/OpenMontage.git
cd OpenMontage
make setup
```

بدون `make`:
```bash
python3 -m venv .venv && source .venv/bin/activate
python -m pip install -r requirements.txt
cd remotion-composer && npm install && cd ..
python -m pip install piper-tts
cp .env.example .env
```

### مفاتيح API (كلها اختيارية)

أضفها في `.env` — كل مفتاح يفتح أدوات إضافية:

| المفتاح | ما يفتحه |
|---------|----------|
| `FAL_KEY` | صور FLUX + فيديو Veo / Kling / MiniMax |
| `PEXELS_API_KEY` / `PIXABAY_API_KEY` / `UNSPLASH_ACCESS_KEY` | لقطات وصور مجانية (المفاتيح مجانية) |
| `ELEVENLABS_API_KEY` | تعليق صوتي احترافي، موسيقى، مؤثرات |
| `OPENAI_API_KEY` | OpenAI TTS + GPT Image |
| `GOOGLE_API_KEY` | Imagen + Google TTS (700+ صوت، منها العربية) |
| `SUNO_API_KEY` | أغانٍ وموسيقى كاملة |

للمزيد راجع [`references/pipelines.md`](references/pipelines.md).

## الخطوة 2: التحقق من القدرات المتاحة

من داخل مجلد OpenMontage (بعد تفعيل `.venv`):

```bash
python -c "from tools.tool_registry import registry; import json; registry.discover(); print(json.dumps(registry.support_envelope(), indent=2))"
python -c "from tools.tool_registry import registry; import json; registry.discover(); print(json.dumps(registry.provider_menu(), indent=2))"
```

## الخطوة 3: الإنتاج

افتح مجلد OpenMontage في الوكيل البرمجي واطلب الفيديو بلغة طبيعية:

```
Make a 60-second animated explainer about how neural networks learn
```

```
Make a 75-second documentary montage about city life in the rain.
Use real footage only, no narration, elegiac tone, with music.
```

```
اصنع فيديو شرح مدته 90 ثانية بالعربية عن تاريخ الخط العربي، بتعليق صوتي وترجمة مدمجة.
```

### قواعد العمل للوكيل (مهم)

1. **اقرأ العقد أولاً**: `AGENT_GUIDE.md` ثم `PROJECT_CONTEXT.md`
2. **لا ترتجل سير العمل**: كل طلب = اختيار خط إنتاج من `pipeline_defs/`
3. اقرأ ملف الـ manifest ثم مهارة المرحلة في `skills/pipelines/<pipeline>/` ثم استخدم الأدوات
4. كل خط إنتاج يمر بالمراحل:
   ```
   research → proposal → script → scene_plan → assets → edit → compose
   ```
5. اطلب موافقة المستخدم عند كل قرار إبداعي (المقترح، السيناريو، خطة المشاهد)
6. قبل التسليم: فحص ffprobe، عينات إطارات، مستويات الصوت، والترجمة

## خطوط الإنتاج المتاحة

| خط الإنتاج | الملف | الأنسب لـ |
|-----------|-------|-----------|
| شرح متحرك | `animated-explainer.yaml` | محتوى تعليمي، دروس |
| رسوم متحركة | `animation.yaml` | موشن جرافيك، نصوص حركية |
| متحدث أفاتار | `avatar-spokesperson.yaml` | تدريب، إعلانات داخلية |
| تحريك شخصيات | `character-animation.yaml` | قصص بشخصيات SVG |
| سينمائي | `cinematic.yaml` | تريلر، تيزر، أفلام علامة تجارية |
| مصنع المقاطع | `clip-factory.yaml` | تقطيع محتوى طويل للسوشيال |
| مونتاج وثائقي | `documentary-montage.yaml` | لقطات حقيقية مجانية، مقالات فيديو |
| هجين | `hybrid.yaml` | لقطات أصلية + رسومات مولّدة |
| ترجمة ودبلجة | `localization-dub.yaml` | توزيع متعدد اللغات |
| تحويل بودكاست | `podcast-repurpose.yaml` | Audiograms، مقتطفات |
| عرض شاشة | `screen-demo.yaml` | عروض برامج ومنتجات |
| متحدث أمام الكاميرا | `talking-head.yaml` | فلوق، مقابلات، عروض |

## نصائح للمحتوى العربي

- استخدم `GOOGLE_API_KEY` أو `ELEVENLABS_API_KEY` لتعليق صوتي عربي طبيعي (Piper محدود في العربية)
- اطلب صراحةً اتجاه النص RTL وخطاً عربياً واضحاً في الترجمة المدمجة
- لخط `localization-dub` حدّد اللهجة المطلوبة (فصحى، خليجية، مصرية...)

## روابط

- المستودع: https://github.com/calesthio/OpenMontage
- الموقع: https://openmontage.video
- دليل الوكيل: `AGENT_GUIDE.md` داخل المستودع
- المزوّدون: `docs/PROVIDERS.md` داخل المستودع
