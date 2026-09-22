export const apiBaseUrl = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? 'http://localhost:8080/api/v1' : '/api/v1')

/** Local upload paths belong to the API origin; object storage/import URLs stay intact. */
export function resolveMediaUrl(value?: string | null): string | undefined {
  if (!value) return undefined
  if (!value.startsWith('/media/')) return value
  return new URL(value, new URL(apiBaseUrl, window.location.origin)).href
}
