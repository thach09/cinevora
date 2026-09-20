import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { authApi, getApiError } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { Button, Field, Input } from '../components/ui'
import { useToast } from '../components/ToastProvider'
import type { LoginRequest, RegisterRequest } from '../types/api'

const loginSchema = z.object({ username: z.string().min(1, 'Username is required'), password: z.string().min(6, 'Use at least 6 characters') })
const registerSchema = z.object({ fullName: z.string().min(2, 'Tell us your name'), username: z.string().min(1, 'Username is required').max(50), email: z.string().email('Enter a valid email'), password: z.string().min(8, 'Use at least 8 characters') })

export function LoginPage() {
  const setAuth = useAuthStore((state) => state.setAuth)
  const navigate = useNavigate(); const location = useLocation(); const { push } = useToast()
  const [busy, setBusy] = useState(false)
  const form = useForm<LoginRequest>({ resolver: zodResolver(loginSchema), defaultValues: { username: '', password: '' } })
  useEffect(() => { if (location.state?.message) push(location.state.message, 'info') }, [location.state, push])
  const submit = form.handleSubmit(async (values) => { setBusy(true); try { const auth = await authApi.login(values); setAuth(auth); navigate(auth.user.role === 'ADMIN' ? '/admin' : '/browse', { replace: true }) } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } })
  return <AuthFrame title="Welcome back" subtitle="Your next great story is waiting."><form className="space-y-5" onSubmit={submit}><Field label="Username" error={form.formState.errors.username?.message}><Input autoComplete="username" placeholder="your username" {...form.register('username')} /></Field><Field label="Password" error={form.formState.errors.password?.message}><Input type="password" autoComplete="current-password" placeholder="••••••••" {...form.register('password')} /></Field><div className="text-right"><Link className="text-sm text-pink-300 hover:text-pink-200" to="/forgot-password">Forgot password?</Link></div><Button className="w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</Button></form><p className="mt-7 text-center text-sm text-slate-400">New to Cinevora? <Link className="font-semibold text-pink-300 hover:text-pink-200" to="/register">Create an account</Link></p></AuthFrame>
}

export function RegisterPage() {
  const setAuth = useAuthStore((state) => state.setAuth); const navigate = useNavigate(); const { push } = useToast(); const [busy, setBusy] = useState(false)
  const form = useForm<RegisterRequest>({ resolver: zodResolver(registerSchema), defaultValues: { fullName: '', username: '', email: '', password: '' } })
  const submit = form.handleSubmit(async (values) => { setBusy(true); try { const auth = await authApi.register(values); setAuth(auth); navigate('/browse', { replace: true }); push('Welcome to Cinevora!', 'success') } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } })
  return <AuthFrame title="Join Cinevora" subtitle="A more personal way to discover movies."><form className="space-y-4" onSubmit={submit}><Field label="Full name" error={form.formState.errors.fullName?.message}><Input placeholder="Alex Morgan" {...form.register('fullName')} /></Field><Field label="Username" error={form.formState.errors.username?.message}><Input autoComplete="username" placeholder="alexmorgan" {...form.register('username')} /></Field><Field label="Email" error={form.formState.errors.email?.message}><Input type="email" autoComplete="email" placeholder="alex@example.com" {...form.register('email')} /></Field><Field label="Password" error={form.formState.errors.password?.message}><Input type="password" autoComplete="new-password" placeholder="At least 8 characters" {...form.register('password')} /></Field><Button className="mt-2 w-full" disabled={busy}>{busy ? 'Creating account…' : 'Create account'}</Button></form><p className="mt-7 text-center text-sm text-slate-400">Already have an account? <Link className="font-semibold text-pink-300 hover:text-pink-200" to="/login">Sign in</Link></p></AuthFrame>
}

export function ForgotPasswordPage() {
  const { push } = useToast(); const [email, setEmail] = useState(''); const [token, setToken] = useState<string | null>(null); const [busy, setBusy] = useState(false)
  const submit = async (event: FormEvent) => { event.preventDefault(); setBusy(true); try { const result = await authApi.forgotPassword(email); setToken(result.developmentToken || null); push('If the email exists, reset instructions are ready.', 'success') } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } }
  return <AuthFrame title="Account recovery" subtitle="A fresh start is only a few steps away."><form className="space-y-5" onSubmit={submit}><Field label="Email"><Input type="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></Field><Button className="w-full" disabled={busy}>{busy ? 'Preparing…' : 'Send reset link'}</Button></form>{token && <div className="mt-5 rounded-xl border border-cyan/20 bg-cyan/5 p-4 text-sm text-cyan-100">Local development token ready. <Link className="font-semibold underline" to={`/reset-password?token=${encodeURIComponent(token)}`}>Continue reset</Link></div>}<p className="mt-7 text-center text-sm text-slate-400"><Link className="text-pink-300 hover:text-pink-200" to="/login">Back to sign in</Link></p></AuthFrame>
}

export function ResetPasswordPage() {
  const [params] = useSearchParams(); const { push } = useToast(); const [password, setPassword] = useState(''); const [busy, setBusy] = useState(false); const navigate = useNavigate()
  const submit = async (event: FormEvent) => { event.preventDefault(); const token = params.get('token'); if (!token) { push('Reset token is missing.', 'error'); return } setBusy(true); try { await authApi.resetPassword(token, password); push('Password updated. Sign in again.', 'success'); navigate('/login') } catch (error) { push(getApiError(error), 'error') } finally { setBusy(false) } }
  return <AuthFrame title="New password" subtitle="Choose something only you know."><form className="space-y-5" onSubmit={submit}><Field label="New password"><Input type="password" minLength={8} required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" /></Field><Button className="w-full" disabled={busy}>{busy ? 'Updating…' : 'Update password'}</Button></form></AuthFrame>
}

export function VerifyEmailPage() {
  const [params] = useSearchParams(); const { push } = useToast(); const [done, setDone] = useState(false)
  useEffect(() => { const token = params.get('token'); if (!token) return; void authApi.verifyEmail(token).then(() => { setDone(true); push('Email verified.', 'success') }).catch((error) => push(getApiError(error), 'error')) }, [params, push])
  return <AuthFrame title="Email verification" subtitle={done ? 'Your email is verified.' : 'Check your verification link.'}><Link className="btn btn-primary w-full" to="/login">Continue to sign in</Link></AuthFrame>
}

function AuthFrame({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return <div className="auth-page"><div className="auth-art"><div className="auth-orbit orbit-one" /><div className="auth-orbit orbit-two" /><span className="auth-kicker">CINEVORA / 01</span><h1>Stories have a way<br />of finding us.</h1><p>Build a library of films that feels like yours.</p><div className="auth-quote">“Cinema is a mirror by which we often see ourselves.”</div></div><div className="auth-panel"><Link to="/login" className="brand mb-12"><span className="brand-mark">C</span><span>Cinevora</span></Link><div className="max-w-md"><p className="eyebrow text-pink-300">{title}</p><h2 className="mt-2 text-4xl font-semibold tracking-tight text-white">{subtitle}</h2><div className="mt-9">{children}</div></div></div></div>
}
