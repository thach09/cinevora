import { useMemo, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { adminApi, getApiError } from '../lib/api'
import { Button, EmptyState, Field, Input, PageHeading, QueryError, Select, Spinner } from '../components/ui'
import { useToast } from '../components/ToastProvider'
import { useI18n } from '../lib/i18n'

function isSafeInternalPath(value: string) {
  if (!value.trim()) return true
  const path = value.trim()
  try {
    const url = new URL(path, window.location.origin)
    return path.startsWith('/') && !path.startsWith('//') && url.origin === window.location.origin && !url.hash
  } catch { return false }
}

export function AdminNotificationPage() {
  const { t } = useI18n()
  const { push } = useToast()
  const [recipientMode, setRecipientMode] = useState<'all' | 'one'>('all')
  const [recipientUsername, setRecipientUsername] = useState('')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [actionUrl, setActionUrl] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const customers = useQuery({ queryKey: ['admin-notification-customers'], queryFn: () => adminApi.users({ status: 'active', page: 0, size: 50 }) })
  const activeCustomers = useMemo(() => customers.data?.content.filter((user) => user.role === 'CUSTOMER') || [], [customers.data])
  const invalidAction = !isSafeInternalPath(actionUrl)
  const valid = title.trim().length > 0 && title.trim().length <= 160 && body.trim().length > 0 && body.trim().length <= 4000 && !invalidAction && (recipientMode === 'all' ? confirmed : Boolean(recipientUsername))
  const send = useMutation({
    mutationFn: () => adminApi.sendNotification({ recipientUsername: recipientMode === 'one' ? recipientUsername : undefined, broadcastToActiveCustomers: recipientMode === 'all', title: title.trim(), body: body.trim(), actionUrl: actionUrl.trim() || undefined }),
    onSuccess: ({ recipientCount }) => { push(t('adminNotifications.sent', { count: recipientCount }), 'success'); setTitle(''); setBody(''); setActionUrl(''); setConfirmed(false) },
    onError: (error) => push(getApiError(error), 'error'),
  })
  if (customers.isLoading) return <Spinner />
  if (customers.isError) return <QueryError message={getApiError(customers.error)} />

  return <div className="space-y-8">
    <PageHeading eyebrow={t('adminNotifications.eyebrow')} title={t('adminNotifications.title')} description={t('adminNotifications.copy')} />
    <section className="surface max-w-3xl p-6 md:p-7">
      {!activeCustomers.length ? <EmptyState title={t('adminNotifications.customerUnavailable')} description={t('adminNotifications.copy')} /> : <form className="space-y-6" onSubmit={(event) => { event.preventDefault(); if (valid) send.mutate() }}>
        <Field label={t('adminNotifications.recipient')}>
          <Select value={recipientMode} onChange={(event) => { setRecipientMode(event.target.value as 'all' | 'one'); setConfirmed(false) }}>
            <option value="all">{t('adminNotifications.all')}</option>
            <option value="one">{t('adminNotifications.one')}</option>
          </Select>
        </Field>
        {recipientMode === 'one' ? <Field label={t('adminNotifications.choose')}><Select value={recipientUsername} onChange={(event) => setRecipientUsername(event.target.value)}><option value="">{t('adminNotifications.choose')}</option>{activeCustomers.map((customer) => <option key={customer.username} value={customer.username}>{customer.fullName || customer.username} (@{customer.username})</option>)}</Select></Field> : <label className="flex items-start gap-3 rounded-xl border border-amber-300/20 bg-amber-300/5 p-4 text-sm leading-6 text-slate-300"><input className="mt-1" type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} />{t('adminNotifications.confirm')}</label>}
        <Field label={t('adminNotifications.subject')}><Input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={160} /></Field>
        <Field label={t('adminNotifications.body')}><textarea className="input min-h-32 resize-y" value={body} onChange={(event) => setBody(event.target.value)} maxLength={4000} /></Field>
        <Field label={t('adminNotifications.action')} error={invalidAction ? t('adminNotifications.invalidAction') : undefined}><Input value={actionUrl} onChange={(event) => setActionUrl(event.target.value)} maxLength={500} placeholder="/browse" /><span className="block text-xs leading-5 text-slate-500">{t('adminNotifications.actionHint')}</span></Field>
        <div className="flex items-center justify-between gap-4"><span className="text-xs text-slate-500">{body.trim().length} / 4000</span><Button disabled={!valid || send.isPending}>{send.isPending ? t('adminNotifications.sending') : t('adminNotifications.send')}</Button></div>
      </form>}
    </section>
  </div>
}
