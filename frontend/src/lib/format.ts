import { intlLocale, type Locale } from './i18n'

export const formatNumber = (value: number, locale: Locale = 'en') => new Intl.NumberFormat(intlLocale(locale), { notation: 'compact', maximumFractionDigits: 1 }).format(value)
export const formatDate = (value: string, locale: Locale = 'en') => new Intl.DateTimeFormat(intlLocale(locale), { dateStyle: 'medium' }).format(new Date(value))
export const initials = (value: string) => value.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase()
