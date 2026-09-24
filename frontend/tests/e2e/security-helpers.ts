import type { APIRequestContext } from '@playwright/test'
export async function csrfPost(request: APIRequestContext, url: string, options: Parameters<APIRequestContext['post']>[1] = {}) {
  const csrfUrl = url.replace(/\/auth\/[^/]+$/, '/auth/csrf')
  const response = await request.get(csrfUrl)
  const csrf = (await response.json()).data
  return request.post(url, { ...options, headers: { ...options.headers, [csrf.headerName]: csrf.token } })
}
