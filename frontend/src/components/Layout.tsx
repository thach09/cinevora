import { useEffect, useState, type ReactNode } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAuthStore } from "../store/authStore";
import { authApi, notificationApi, profileApi } from '../lib/api'
import { initials } from "../lib/format";
import { Button } from "./ui";

function isSafeNotificationPath(value: string) {
  try {
    const url = new URL(value, window.location.origin);
    return value.startsWith("/") && !value.startsWith("//") && url.origin === window.location.origin;
  } catch {
    return false;
  }
}

const customerLinks = [
  ["Browse", "/browse", "⌂"],
  ["Search", "/search", "⌕"],
  ["Watchlist", "/watchlist", "＋"],
  ["Favourites", "/favourites", "♥"],
  ["Continue watching", "/continue-watching", "▷"],
  ["History", "/history", "◷"],
  ["Account", "/account", "◎"],
];

const adminLinks = [
  ["Users", "/admin/users", "U"],
  ["Archive", "/admin/archive", "A"],
  ["Media", "/admin/media", "M"],
  ["Dashboard", "/admin", "▦"],
  ["Movies", "/admin/movies", "▣"],
  ["Categories", "/admin/categories", "◈"],
  ["Statistics", "/admin/statistics", "↗"],
];

export function AppLayout({ children }: { children?: ReactNode }) {
  const [open, setOpen] = useState(false);

  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const activeProfileId = useAuthStore((state) => state.activeProfileId)
  const setActiveProfile = useAuthStore((state) => state.setActiveProfile)
  const queryClient = useQueryClient()
  const profiles = useQuery({ queryKey: ['profiles', user?.id], queryFn: profileApi.list, enabled: Boolean(user) })

  const navigate = useNavigate();
  const location = useLocation();

  const links =
    user?.role === "ADMIN" ? [...customerLinks, ...adminLinks] : customerLinks;

  const exactMatch = links.find(([, path]) => location.pathname === path);

  const nestedMatch = [...links]
    .sort((a, b) => b[1].length - a[1].length)
    .find(([, path]) => location.pathname.startsWith(`${path}/`));

  const pageName = exactMatch?.[0] || nestedMatch?.[0] || "Cinevora";

  useEffect(() => {
    if (!document.querySelector('[data-seo-page="true"]')) document.title = `${pageName} · Cinevora`;
  }, [pageName]);

  useEffect(() => {
    const defaultProfile = profiles.data?.find((profile) => profile.defaultProfile) || profiles.data?.[0]
    const activeProfileIsOwned = activeProfileId != null && profiles.data?.some((profile) => profile.id === activeProfileId)
    if (defaultProfile && !activeProfileIsOwned) setActiveProfile(defaultProfile.id)
  }, [activeProfileId, profiles.data, setActiveProfile])

  const signOut = () => {
    queryClient.clear()
    void authApi.logout().finally(() => { logout(); navigate("/login"); })
  };

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Skip to content</a>
      <aside className={`sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="flex items-center justify-between px-6 py-6">
          <NavLink
            to="/browse"
            className="brand"
            onClick={() => setOpen(false)}
          >
            <span className="brand-mark">C</span>
            <span>Cinevora</span>
          </NavLink>

          <button
            type="button"
            className="sidebar-close md:hidden"
            onClick={() => setOpen(false)}
            aria-label="Close menu"
          >
            ×
          </button>
        </div>

        <div className="px-4 pb-4">
          <p className="eyebrow px-3">Discover</p>

          <nav className="mt-3 space-y-1">
            {customerLinks.map(([label, path, icon]) => (
              <SideLink
                key={path}
                label={label}
                path={path}
                icon={icon}
                close={() => setOpen(false)}
              />
            ))}
          </nav>
        </div>

        {user?.role === "ADMIN" && (
          <div className="border-t border-slate-800/80 px-4 py-5">
            <p className="eyebrow px-3">Workspace</p>

            <nav className="mt-3 space-y-1">
              {adminLinks.map(([label, path, icon]) => (
                <SideLink
                  key={path}
                  label={label}
                  path={path}
                  icon={icon}
                  close={() => setOpen(false)}
                />
              ))}
            </nav>
          </div>
        )}

        <div className="mt-auto hidden p-5 md:block">
          <div className="sidebar-note">
            <span className="text-xl">✦</span>
            <p>Find your next favourite story.</p>
          </div>
        </div>
      </aside>

      {open && (
        <button
          type="button"
          className="sidebar-scrim md:hidden"
          aria-label="Close menu"
          onClick={() => setOpen(false)}
        />
      )}

      <div className="main-column">
        <header className="topbar">
          <button
            type="button"
            className="menu-button md:hidden"
            onClick={() => setOpen(true)}
            aria-label="Open menu"
          >
            ☰
          </button>

          <div>
            <p className="eyebrow">
              {user?.role === "ADMIN"
                ? "Admin workspace"
                : "Your cinema, anywhere"}
            </p>

            <h1 className="topbar-title">{pageName}</h1>
          </div>

          <div className="ml-auto flex items-center gap-3">
            {profiles.data && profiles.data.length > 0 && <label className="hidden items-center gap-2 text-xs text-slate-500 sm:flex"><span className="sr-only">Active profile</span><select className="profile-select" value={activeProfileId || profiles.data[0].id} onChange={(event) => setActiveProfile(Number(event.target.value))}>{profiles.data.map((profile) => <option key={profile.id} value={profile.id}>{profile.name}</option>)}</select></label>}
            <NotificationCenter />
            <div className="avatar">
              {initials(user?.fullName || user?.username || "U")}
            </div>

            <div className="hidden text-right sm:block">
              <p className="text-sm font-semibold text-white">
                {user?.fullName || user?.username}
              </p>

              <p className="text-xs text-slate-500">
                {user?.role === "ADMIN" ? "Administrator" : "Member"}
              </p>
            </div>

            <Button
              variant="ghost"
              className="hidden sm:inline-flex"
              onClick={signOut}
            >
              Sign out
            </Button>
            <Button
              variant="ghost"
              className="px-2 sm:hidden"
              onClick={signOut}
              aria-label="Sign out"
            >
              Exit
            </Button>
          </div>
        </header>

        <main id="main-content" className="page-content">{children || <Outlet />}</main>

        <footer className="footer">
          <span>© {new Date().getFullYear()} Cinevora</span>
          <span>Stories worth staying up for.</span>
        </footer>
      </div>
    </div>
  );
}

function NotificationCenter() {
  const [open, setOpen] = useState(false)
  const client = useQueryClient()
  const inbox = useQuery({ queryKey: ['notifications'], queryFn: notificationApi.inbox, staleTime: 30_000, initialData: { items: [], unreadCount: 0 } })
  const markRead = useMutation({ mutationFn: notificationApi.markRead, onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }) })
  const markAll = useMutation({ mutationFn: notificationApi.markAllRead, onSuccess: () => client.invalidateQueries({ queryKey: ['notifications'] }) })
  return <div className="notification-center"><button type="button" className="notification-button" aria-label={`Notifications${inbox.data?.unreadCount ? `, ${inbox.data.unreadCount} unread` : ''}`} aria-expanded={open} onClick={() => setOpen((current) => !current)}><span aria-hidden="true">♧</span>{Boolean(inbox.data?.unreadCount) && <span className="notification-badge">{inbox.data.unreadCount > 9 ? '9+' : inbox.data.unreadCount}</span>}</button>{open && <div className="notification-popover" role="dialog" aria-label="Notifications"><div className="flex items-center justify-between gap-4 border-b border-slate-800 px-4 py-3"><strong className="text-sm text-white">Notifications</strong><button type="button" className="text-xs text-pink-200 hover:text-white" onClick={() => markAll.mutate()}>Mark all read</button></div><div className="max-h-80 overflow-y-auto">{inbox.data?.items.length ? inbox.data.items.map((item) => <button type="button" key={item.id} className={`notification-item ${item.read ? '' : 'notification-item-unread'}`} onClick={() => { if (!item.read) markRead.mutate(item.id); if (item.actionUrl && isSafeNotificationPath(item.actionUrl)) window.location.assign(item.actionUrl) }}><span className="block font-semibold text-white">{item.title}</span><span className="mt-1 block text-xs leading-5 text-slate-400">{item.body}</span></button>) : <p className="px-4 py-6 text-center text-xs text-slate-500">You are all caught up.</p>}</div></div>}</div>
}

function SideLink({
  label,
  path,
  icon,
  close,
}: {
  label: string;
  path: string;
  icon: string;
  close: () => void;
}) {
  return (
    <NavLink
      to={path}
      end={path === "/admin"}
      onClick={close}
      className={({ isActive }) =>
        `side-link ${isActive ? "side-link-active" : ""}`
      }
    >
      <span className="side-icon">{icon}</span>

      <span>{label}</span>
    </NavLink>
  );
}
