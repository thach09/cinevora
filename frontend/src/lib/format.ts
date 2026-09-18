export const formatNumber = (value: number) => new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(value)
export const formatDate = (value: string) => new Intl.DateTimeFormat('en-US', { dateStyle: 'medium' }).format(new Date(value))
export const initials = (value: string) => value.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase()
