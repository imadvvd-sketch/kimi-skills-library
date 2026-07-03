---
name: analytics-reporter
description: "تحليل البيانات وإنشاء تقارير التسويق - Google Analytics 4، Google Search Console، Social Media Analytics، وDashboards. استخدم عندما يطلب المستخدم تحليل أداء، إنشاء Dashboard، تقرير Marketing، KPI Tracking، أو Data Visualization. يشمل: UTM Tracking، Attribution، Goal Setting، وReporting Templates."
---

# محلل بيانات التسويق

## 1. Google Analytics 4 📊

### الإعداد الأساسي
- [ ] Property + Data Stream
- [ ] Events مخصصة
- [ ] Conversions/Goals
- [ ] Audiences
- [ ] Google Ads Link
- [ ] Search Console Link

### Reports رئيسية
| التقرير | الاستخدام | المقاييس |
|---------|----------|----------|
| Acquisition | من أين يأتي الزوار | Sessions, Users, Channels |
| Engagement | كيف يتفاعلون | Avg. Session, Events |
| Monetization | التحويلات | Revenue, Purchases |
| Retention | هل يعودون؟ | New vs Returning |
| Demographics | من هم؟ | Age, Gender, Location |

### Events أساسية
```
page_view          - مشاهدة صفحة
scroll             - تمرير (90%)
click              - نقر على رابط خارجي
file_download      - تحميل ملف
form_start         - بدء تعبئة نموذج
form_submit        - إرسال نموذج
purchase           - عملية شراء
add_to_cart        - إضافة للسلة
```

---

## 2. Google Search Console 🔍

### Reports رئيسية
| التقرير | الاستخدام |
|---------|----------|
| Performance | الظهور، النقرات، CTR، المركز |
| Indexing | الصفحات المفهرسة والمستبعدة |
| Experience | Core Web Vitals |
| Enhancements | Schema، Mobile Usability |

### تحليل Performance
```
Query Analysis:
- كلمات ذات CTR منخفض: تحسين Title/Description
- كلمات في المركز 4-10: تحسين المحتوى للوصول للصفحة 1
- كلمات في المركز 11-20: تحسين On-Page
- New Keywords: فرص جديدة
```

---

## 3. UTM Tracking 🔗

### بناء UTM Parameters
```
?utm_source=facebook
&utm_medium=paid_social
&utm_campaign=summer_sale_2026
&utm_content=video_ad_1
&utm_term=seo_tools
```

| Parameter | الوصف | أمثلة |
|-----------|-------|-------|
| source | المنصة | google, facebook, newsletter |
| medium | النوع | cpc, organic, email, social |
| campaign | الحملة | product_launch, black_friday |
| content | المحتوى | banner_a, video_1 |
| term | الكلمة | seo_tools (للـ Paid Search) |

### UTM Builder Template
```
https://example.com/landing-page
?utm_source={PLATFORM}
&utm_medium={TYPE}
&utm_campaign={CAMPAIGN_NAME}_{DATE}
&utm_content={CREATIVE_VERSION}
```

---

## 4. KPIs Dashboard 📈

### Marketing Dashboard الأساسي
```
┌─────────────────────────────────────────────────────┐
│  TRAFFIC          │  CONVERSIONS      │  REVENUE    │
│  15,420 visits    │  312 leads        │  $45,000    │
│  ↑ 23% vs LM      │  ↑ 15% vs LM      │  ↑ 30% vs LM│
├─────────────────────────────────────────────────────┤
│  CHANNELS         │  TOP PAGES        │  ADS        │
│  Organic: 40%     │  /blog/seo-guide  │  ROAS: 4.2  │
│  Paid: 30%        │  /pricing         │  CPA: $25   │
│  Social: 20%      │  /case-studies    │  CTR: 3.1%  │
│  Email: 10%       │  /tools           │  Spend: $5K │
└─────────────────────────────────────────────────────┘
```

### Monthly Report Template
```
# تقرير التسويق - [الشهر]

## ملخص تنفيذي
- الزيارات: X (↑/↓ Y%)
- Leads: X (↑/↓ Y%)
- المبيعات: X (↑/↓ Y%)
- ROAS: X (↑/↓ Y%)

## حركة الموقع
| Channel | Sessions | Users | Bounce Rate |
|---------|----------|-------|-------------|
| Organic | X        | X     | X%          |
| Paid    | X        | X     | X%          |
| Social  | X        | X     | X%          |
| Email   | X        | X     | X%          |

## الأداء
| Campaign | Spend | Revenue | ROAS | CPA |
|----------|-------|---------|------|-----|
| Campaign 1 | $X  | $X      | X    | $X  |

## التوصيات
1. ...
2. ...
3. ...
```

---

## 5. Attribution Models 🎯

### النماذج المتاحة
| النموذج | الوصف | متى تستخدم |
|---------|-------|-----------|
| First Click | آخر نقطة قبل الشراء | Brand Awareness |
| Last Click | أول نقطة اتصال | Direct Response |
| Linear | وزن متساوٍ لكل نقطة | رحلة طويلة |
| Time Decay | الأحدث أوزن أكثر | Short Sales Cycle |
| Data-Driven | GA4 ML-based | الأكثر دقة |

---

## 6. أدوات التحليل 🔧

| الأداة | الاستخدام |
|--------|----------|
| Google Analytics 4 | تحليل الموقع |
| Google Search Console | أداء البحث |
| Google Looker Studio | Dashboards |
| Meta Ads Manager | أداء Meta |
| Google Ads | أداء Search |
| Hotjar | Heatmaps |
| SEMrush/Ahrefs | SEO Analysis |

---

## نصائح ذهبية 🏆

1. ** consistency**: حدد KPIs واستمر عليها
2. **Automate**: استخدم Looker Studio للتقارير التلقائية
3. **Context**: الأرقام بدون سياق لا تعني شيئاً
4. **Actionable**: كل تقرير يجب أن يتضمن توصيات
5. **Regular**: تقارير أسبوعية سريعة + شاملة شهرياً
