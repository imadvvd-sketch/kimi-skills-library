---
name: excel-automation
description: "أتمتة مهام Excel والجداول الحسابية. استخدم عندما يطلب المستخدم معالجة ملف Excel، إنشاء جداول، حسابات مالية، تحليل بيانات في Excel، أو أتمتة مهام متكررة. يدعم: XLSX, CSV, التنسيق، الصيغ، والرسوم البيانية."
---

# أتمتة Excel

## القدرات

- 📊 إنشاء وتحرير جداول
- 🧮 صيغ وحسابات
- 📈 رسوم بيانية
- 🔄 معالجة CSV/Excel
- 📋 تقارير وملخصات
- 🎨 تنسيق خلايا

## العمليات الشائعة

### 1. قراءة ملف
```python
import pandas as pd

df = pd.read_excel('file.xlsx', sheet_name='Sheet1')
print(df.head())
```

### 2. كتابة ملف
```python
df.to_excel('output.xlsx', index=False, engine='openpyxl')
```

### 3. تنسيق الخلايا
```python
from openpyxl import load_workbook
from openpyxl.styles import Font, PatternFill, Alignment

wb = load_workbook('file.xlsx')
ws = wb.active

# تنسيق عنوان
ws['A1'].font = Font(bold=True, size=14, color='FFFFFF')
ws['A1'].fill = PatternFill(start_color='366092', end_color='366092', fill_type='solid')
ws['A1'].alignment = Alignment(horizontal='center')

wb.save('formatted.xlsx')
```

### 4. إنشاء رسم بياني
```python
import matplotlib.pyplot as plt

plt.figure(figsize=(10, 6))
df['Column'].plot(kind='bar')
plt.title('العنوان')
plt.savefig('chart.png')
```

### 5. صيغ Excel
```python
# كتابة صيغة مباشرة
ws['D2'] = '=SUM(A2:C2)'

# صيغ شرطية
ws['E2'] = '=IF(A2>100,"مرتفع","منخفض")'
```

## النماذج المالية

### تقرير الدخل
```
الإيرادات
- إيرادات المبيعات
- إيرادات أخرى
الإجمالي

المصروفات
- تكلفة البضاعة
- المصروفات التشغيلية
- المصروفات الإدارية
الإجمالي

صافي الربح = الإيرادات - المصروفات
```

### الميزانية
```
المصادر:
- الراتب
- إيرادات إضافية
الإجمالي

الاستخدامات:
- السكن
- المواصلات
- الطعام
- الترفيه
- الادخار
الإجمالي

الفائض/العجز
```

## نصائح

- استخدم named ranges
- freeze panes للعناوين
- data validation للإدخال
- conditional formatting للتمييز
- حماية الخلايا الحساسة
