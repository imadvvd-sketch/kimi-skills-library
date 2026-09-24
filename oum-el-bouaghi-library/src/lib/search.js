import { catalog } from '../data/catalog.js'

/** تطبيع النص للبحث: حذف التشكيل وتوحيد أشكال الحروف العربية والحروف اللاتينية المنبورة */
export function normalize(text = '') {
  return String(text)
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '') // النبرات اللاتينية é → e
    .replace(/[ً-ٰٟـ]/g, '') // التشكيل والتطويل
    .replace(/[أإآٱ]/g, 'ا')
    .replace(/ة/g, 'ه')
    .replace(/ى/g, 'ي')
    .replace(/ؤ/g, 'و')
    .replace(/ئ/g, 'ي')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const index = catalog.map((item) => ({
  item,
  haystack: normalize(`${item.title} ${item.author} ${item.subject} ${item.cote} ${item.year ?? ''}`),
}))

/**
 * البحث في الرصيد. حالياً في البيانات المحلية؛
 * لربطه بنظام حقيقي (Koha / PMB …) اجعل هذه الدالة تستدعي واجهة API وتعيد نفس الشكل.
 */
export function searchCatalog({ query = '', type = 'all', lang = 'all', availableOnly = false }) {
  const terms = normalize(query).split(' ').filter(Boolean)
  return index
    .filter(({ item, haystack }) => {
      if (type !== 'all' && item.type !== type) return false
      if (lang !== 'all' && item.lang !== lang) return false
      if (availableOnly && !item.available) return false
      return terms.every((term) => haystack.includes(term))
    })
    .map(({ item }) => item)
}
