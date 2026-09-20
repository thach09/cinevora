import { useEffect, useState, type ReactNode } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";

import { useAuthStore } from "../store/authStore";
import { initials } from "../lib/format";
import { Button } from "./ui";

const customerLinks = [
  ["Browse", "/browse", "⌂"],
  ["Search", "/search", "⌕"],
  ["Watchlist", "/watchlist", "＋"],
  ["Favourites", "/favourites", "♥"],
  ["Continue watching", "/continue-watching", "▷"],
  ["History", "/history", "◷"],
];

const adminLinks = [
  ["Dashboard", "/admin", "▦"],
  ["Movies", "/admin/movies", "▣"],
  ["Categories", "/admin/categories", "◈"],
  ["Statistics", "/admin/statistics", "↗"],
];

export function AppLayout({ children }: { children?: ReactNode }) {
  const [open, setOpen] = useState(false);

  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);

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
    document.title = `${pageName} · Cinevora`;
  }, [pageName]);

  const signOut = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="app-shell">
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

        <main className="page-content">{children || <Outlet />}</main>

        <footer className="footer">
          <span>© {new Date().getFullYear()} Cinevora</span>
          <span>Stories worth staying up for.</span>
        </footer>
      </div>
    </div>
  );
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
