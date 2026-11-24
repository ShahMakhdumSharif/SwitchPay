import React, { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { ROLES } from "../auth/roles.js";

const NAV_ITEMS = [
  { to: "/", label: "Dashboard", roles: [ROLES.BUYER, ROLES.SELLER, ROLES.ADMIN] },
  // Buyer screens
  { to: "/accounts", label: "My Accounts", roles: [ROLES.BUYER] },
  { to: "/atm", label: "ATM Terminal", roles: [ROLES.BUYER] },
  { to: "/transfer", label: "Mobile Transfer", roles: [ROLES.BUYER] },
  { to: "/transactions", label: "Transactions", roles: [ROLES.BUYER, ROLES.ADMIN] },
  // Seller screens
  { to: "/merchant", label: "Merchant Profile", roles: [ROLES.SELLER] },
  { to: "/checkout-sessions", label: "Checkout Sessions", roles: [ROLES.SELLER] },
  { to: "/settlements", label: "Settlements", roles: [ROLES.SELLER, ROLES.ADMIN] },
  { to: "/refunds", label: "Refunds", roles: [ROLES.SELLER, ROLES.ADMIN] },
  // Admin screens
  { to: "/admin/ops", label: "Ops Monitoring", roles: [ROLES.ADMIN] },
  { to: "/admin/ledger", label: "Double-Entry Ledger", roles: [ROLES.ADMIN] },
  { to: "/admin/adjustments", label: "Adjustments & Reversals", roles: [ROLES.ADMIN] },
  { to: "/admin/accounts", label: "Manage Accounts", roles: [ROLES.ADMIN] },
  { to: "/admin/channels", label: "Manage Channels", roles: [ROLES.ADMIN] },
  { to: "/admin/users", label: "Manage Users", roles: [ROLES.ADMIN] },
  { to: "/admin/audit", label: "Audit Logs", roles: [ROLES.ADMIN] },
];

export default function AppShell() {
  const { user, roles, hasAnyRole, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const visibleNav = NAV_ITEMS.filter((item) => hasAnyRole(item.roles));

  const handleLogout = async () => {
    setLoggingOut(true);
    await logout();
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col md:flex-row">
      {/* Mobile Header */}
      <header className="md:hidden bg-slate-900 text-white px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="font-bold text-lg tracking-tight">SwitchPay</span>
          {roles.map((role) => (
            <span
              key={role}
              className="text-[11px] font-semibold px-2 py-0.5 rounded bg-indigo-600 text-white"
            >
              {role}
            </span>
          ))}
        </div>
        <button
          type="button"
          onClick={() => setMobileMenuOpen((prev) => !prev)}
          className="px-3 py-1.5 rounded text-xs font-medium bg-slate-800 hover:bg-slate-700"
        >
          {mobileMenuOpen ? "Close" : "Menu"}
        </button>
      </header>

      {/* Sidebar */}
      <aside
        className={`${
          mobileMenuOpen ? "block" : "hidden"
        } md:flex md:flex-col md:w-64 bg-slate-900 text-slate-100 shrink-0`}
      >
        <div className="hidden md:flex items-center justify-between px-6 py-5 border-b border-slate-800">
          <div>
            <span className="text-xl font-bold tracking-tight text-white">SwitchPay</span>
            <p className="text-xs text-slate-400 mt-0.5">Banking & Payment Switch</p>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          {visibleNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              onClick={() => setMobileMenuOpen(false)}
              className={({ isActive }) =>
                `block px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? "bg-indigo-600 text-white"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white"
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-slate-800 bg-slate-950/50">
          <div className="flex flex-wrap gap-1 mb-2">
            {roles.map((role) => (
              <span
                key={role}
                className="text-[11px] font-semibold px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30"
              >
                {role}
              </span>
            ))}
          </div>
          <p className="text-sm font-medium text-white truncate">
            {user?.fullName || "Authenticated User"}
          </p>
          <p className="text-xs text-slate-400 truncate">{user?.email}</p>
          <button
            type="button"
            disabled={loggingOut}
            onClick={handleLogout}
            className="mt-3 w-full px-3 py-2 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-red-600 text-slate-200 hover:text-white transition-colors disabled:opacity-50"
          >
            {loggingOut ? "Signing out..." : "Sign Out"}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8">
        <div className="max-w-6xl mx-auto">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
