import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { authApi, getApiError } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { Button, Field, Input } from '../components/ui'
import { Brand } from '../components/Layout'
import { useToast } from '../components/ToastProvider'
import { type Translator, useI18n } from '../lib/i18n'
import type { LoginRequest, RegisterRequest } from '../types/api'

const loginSchema = (t: Translator) => z.object({ username: z.string().min(1, t('auth.usernameRequired')), password: z.string().min(6, t('auth.passwordMin6')) })
const registerSchema = (t: Translator) => z.object({ fullName: z.string().min(2, t('auth.nameMin')), username: z.string().min(1, t('auth.usernameRequired')).max(50), email: z.string().email(t('auth.emailValid')), password: z.string().min(8, t('auth.passwordMin8')) })

export function LoginPage() {
  const { t } = useI18n(); const setAuth = useAuthStore((state) => state.setAuth); const queryClient = useQueryClient(); const navigate = useNavigate(); const location = useLocation(); const { push } = useToast(); const [busy, setBusy] = useState(false)
  const form = useForm<LoginRequest>({ resolver: zodResolver(loginSchema(t)), defaultValues: { username: '', password: '' } })
  useEffect(() => { if (location.state?.message) push(location.state.message, 'info') }, [location.state, push])
  const submit = form.handleSubmit(async (values) => { setBusy(true); try { const auth = await authApi.login(values); queryClient.clear(); setAuth(auth); navigate(auth.user.role === 'ADMIN' ? '/admin' : '/browse', { replace: true }) } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } })
  return <AuthFrame title={t('auth.welcome')} subtitle={t('auth.signInSubtitle')}><form className="space-y-5" onSubmit={submit}><Field label={t('auth.username')} error={form.formState.errors.username?.message}><Input autoComplete="username" placeholder={t('auth.usernamePlaceholder')} {...form.register('username')} /></Field><Field label={t('auth.password')} error={form.formState.errors.password?.message}><Input type="password" autoComplete="current-password" placeholder="••••••••" {...form.register('password')} /></Field><div className="text-right"><Link className="text-sm text-pink-300 hover:text-pink-200" to="/forgot-password">{t('auth.forgot')}</Link></div><Button className="w-full" disabled={busy}>{busy ? t('auth.signingIn') : t('auth.signIn')}</Button></form><p className="mt-7 text-center text-sm text-slate-400">{t('auth.newHere')} <Link className="font-semibold text-pink-300 hover:text-pink-200" to="/register">{t('auth.createAccount')}</Link></p></AuthFrame>
}

export function RegisterPage() {
  const { t } = useI18n(); const setAuth = useAuthStore((state) => state.setAuth); const navigate = useNavigate(); const { push } = useToast(); const [busy, setBusy] = useState(false); const queryClient = useQueryClient(); const form = useForm<RegisterRequest>({ resolver: zodResolver(registerSchema(t)), defaultValues: { fullName: '', username: '', email: '', password: '' } })
  const submit = form.handleSubmit(async (values) => { setBusy(true); try { const auth = await authApi.register(values); queryClient.clear(); setAuth(auth); navigate('/browse', { replace: true }); push(t('auth.welcomeToast'), 'success') } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } })
  return <AuthFrame title={t('auth.join')} subtitle={t('auth.registerSubtitle')}><form className="space-y-4" onSubmit={submit}><Field label={t('auth.fullName')} error={form.formState.errors.fullName?.message}><Input placeholder="Alex Morgan" {...form.register('fullName')} /></Field><Field label={t('auth.username')} error={form.formState.errors.username?.message}><Input autoComplete="username" placeholder="alexmorgan" {...form.register('username')} /></Field><Field label={t('auth.email')} error={form.formState.errors.email?.message}><Input type="email" autoComplete="email" placeholder="alex@example.com" {...form.register('email')} /></Field><Field label={t('auth.password')} error={form.formState.errors.password?.message}><Input type="password" autoComplete="new-password" placeholder={t('auth.passwordPlaceholder')} {...form.register('password')} /></Field><Button className="mt-2 w-full" disabled={busy}>{busy ? t('auth.creating') : t('auth.createAccount')}</Button></form><p className="mt-7 text-center text-sm text-slate-400">{t('auth.haveAccount')} <Link className="font-semibold text-pink-300 hover:text-pink-200" to="/login">{t('auth.signIn')}</Link></p></AuthFrame>
}

export function ForgotPasswordPage() {
  const { t } = useI18n(); const { push } = useToast(); const [email, setEmail] = useState(''); const [busy, setBusy] = useState(false)
  const submit = async (event: FormEvent) => { event.preventDefault(); setBusy(true); try { await authApi.forgotPassword(email); push(t('auth.recoverySubtitle'), 'success') } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } }
  return <AuthFrame title={t('auth.recovery')} subtitle={t('auth.recoverySubtitle')}><form className="space-y-5" onSubmit={submit}><Field label={t('auth.email')}><Input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></Field><Button className="w-full" disabled={busy}>{busy ? t('auth.preparing') : t('auth.sendReset')}</Button></form><p className="mt-7 text-center text-sm text-slate-400"><Link className="text-pink-300 hover:text-pink-200" to="/login">{t('auth.backToSignIn')}</Link></p></AuthFrame>
}

export function ResetPasswordPage() {
  const { t } = useI18n(); const [params] = useSearchParams(); const { push } = useToast(); const [password, setPassword] = useState(''); const [busy, setBusy] = useState(false); const navigate = useNavigate()
  const submit = async (event: FormEvent) => { event.preventDefault(); const token = params.get('token'); if (!token) { push(t('auth.resetMissing'), 'error'); return } setBusy(true); try { await authApi.resetPassword(token, password); push(t('auth.passwordUpdated'), 'success'); navigate('/login') } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } }
  return <AuthFrame title={t('auth.newPassword')} subtitle={t('auth.newPasswordSubtitle')}><form className="space-y-5" onSubmit={submit}><Field label={t('auth.newPassword')}><Input type="password" minLength={8} required value={password} onChange={(event) => setPassword(event.target.value)} placeholder={t('auth.passwordPlaceholder')} /></Field><Button className="w-full" disabled={busy}>{busy ? t('auth.updating') : t('auth.updatePassword')}</Button></form></AuthFrame>
}

export function VerifyEmailPage() {
  const { t } = useI18n(); const [params] = useSearchParams(); const { push } = useToast(); const [done, setDone] = useState(false)
  useEffect(() => { const token = params.get('token'); if (!token) return; void authApi.verifyEmail(token).then(() => { setDone(true); push(t('auth.verified'), 'success') }).catch((error) => push(getApiError(error), 'error')) }, [params, push, t])
  return <AuthFrame title={t('auth.emailVerification')} subtitle={done ? t('auth.emailVerified') : t('auth.checkVerification')}><Link className="btn btn-primary w-full" to="/login">{t('auth.continueSignIn')}</Link></AuthFrame>
}

function AuthFrame({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  const { t } = useI18n(); return <div className="auth-page"><div className="auth-art"><div className="auth-orbit orbit-one" /><div className="auth-orbit orbit-two" /><span className="auth-kicker">CINEVORA</span><h1>{t('auth.artTitle')}</h1><p>{t('auth.artCopy')}</p></div><main className="auth-panel"><Link to="/login" className="mb-12 inline-flex"><Brand /></Link><div className="max-w-md"><p className="eyebrow text-pink-300">{title}</p><h2 className="mt-2 text-4xl font-semibold tracking-tight text-white">{subtitle}</h2><div className="mt-9">{children}</div></div></main></div>
}
