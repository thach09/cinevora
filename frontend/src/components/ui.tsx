import {
  forwardRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
} from "react";
import { useI18n } from "../lib/i18n";

export function Button({
  children,
  className = "",
  variant = "primary",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
}) {
  return (
    <button className={`btn btn-${variant} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

export const Input = forwardRef<
  HTMLInputElement,
  InputHTMLAttributes<HTMLInputElement>
>(function Input(props, ref) {
  return <input ref={ref} className="input" {...props} />;
});

export const Select = forwardRef<
  HTMLSelectElement,
  SelectHTMLAttributes<HTMLSelectElement>
>(function Select(props, ref) {
  return <select ref={ref} className="input" {...props} />;
});

export function Spinner({ label }: { label?: string }) {
  const { t } = useI18n();
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-sm text-slate-400">
      <span className="spinner" />
      {label || t("common.loading")}
    </div>
  );
}

export function QueryError({ message }: {
  message?: string;
}) {
  const { t } = useI18n();
  return (
    <div className="surface flex min-h-40 items-center justify-center px-6 text-center text-sm text-rose-200">
      {message || t("common.unavailable")}
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="surface flex min-h-52 flex-col items-center justify-center px-6 text-center">
      <span className="mb-3 text-3xl text-pink-300">✦</span>
      <h3 className="text-lg font-semibold text-white">{title}</h3>
      <p className="mt-1 max-w-md text-sm text-slate-400">{description}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function StatCard({
  label,
  value,
  accent = "pink",
}: {
  label: string;
  value: string | number;
  accent?: "pink" | "cyan" | "violet";
}) {
  return (
    <div className={`stat-card stat-${accent}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

export function PageHeading({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2 className="section-title mt-1">{title}</h2>
        <p className="mt-2 max-w-2xl text-slate-400">{description}</p>
      </div>
      {actions && <div className="page-heading-actions">{actions}</div>}
    </div>
  );
}

export function StatusPill({ active, activeLabel = "Active", inactiveLabel = "Archived" }: {
  active: boolean;
  activeLabel?: string;
  inactiveLabel?: string;
}) {
  return (
    <span className={`status-pill ${active ? "status-pill-active" : "status-pill-inactive"}`}>
      {active ? activeLabel : inactiveLabel}
    </span>
  );
}
