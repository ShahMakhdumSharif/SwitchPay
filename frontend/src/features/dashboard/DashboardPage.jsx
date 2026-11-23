import React from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../shared/auth/AuthContext.jsx";
import { ROLES } from "../../shared/auth/roles.js";
import { CHANNEL_CODES, CURRENCY } from "../../shared/config.js";

export default function DashboardPage() {
  const { user, roles, hasRole } = useAuth();

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">
              Welcome, {user?.fullName || user?.email}
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              SwitchPay Multi-Channel Banking & Payment Switch • Default Currency:{" "}
              <span className="font-semibold text-slate-800">{CURRENCY}</span> • Channels:{" "}
              <span className="font-semibold text-slate-800">{CHANNEL_CODES.join(", ")}</span>
            </p>
          </div>
          <div className="flex items-center gap-2">
            {roles.map((r) => (
              <span
                key={r}
                className="px-3 py-1 rounded-full text-xs font-semibold bg-indigo-100 text-indigo-800"
              >
                {r}
              </span>
            ))}
          </div>
        </div>
      </div>

      {hasRole(ROLES.BUYER) && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-slate-900">Customer (BUYER) Actions</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <Link
              to="/accounts"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">My Accounts</h3>
              <p className="mt-1 text-xs text-slate-600">
                View your accounts and perform real-time BDT balance inquiry.
              </p>
            </Link>
            <Link
              to="/atm"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">ATM Terminal</h3>
              <p className="mt-1 text-xs text-slate-600">
                Withdraw cash, check balance, or validate account via ATM channel.
              </p>
            </Link>
            <Link
              to="/transfer"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Mobile Transfer</h3>
              <p className="mt-1 text-xs text-slate-600">
                Transfer funds between accounts via Mobile Banking channel.
              </p>
            </Link>
            <Link
              to="/transactions"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Transaction History</h3>
              <p className="mt-1 text-xs text-slate-600">
                Track transaction statuses across the switch lifecycle.
              </p>
            </Link>
          </div>
        </section>
      )}

      {hasRole(ROLES.SELLER) && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-slate-900">Merchant (SELLER) Actions</h2>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Link
              to="/merchant"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Merchant Profile</h3>
              <p className="mt-1 text-xs text-slate-600">
                Register or manage your business profile and settlement account.
              </p>
            </Link>
            <Link
              to="/checkout-sessions"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Checkout Sessions</h3>
              <p className="mt-1 text-xs text-slate-600">
                Create web checkout sessions and share payment links.
              </p>
            </Link>
            <Link
              to="/settlements"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Settlements & Refunds</h3>
              <p className="mt-1 text-xs text-slate-600">
                Review merchant settlements and issue customer refunds.
              </p>
            </Link>
          </div>
        </section>
      )}

      {hasRole(ROLES.ADMIN) && (
        <section className="space-y-3">
          <h2 className="text-lg font-semibold text-slate-900">
            Platform & Operations (ADMIN) Actions
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <Link
              to="/admin/ops"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Operational Monitoring</h3>
              <p className="mt-1 text-xs text-slate-600">
                Inspect summary counts, failed transactions, and stuck pending requests.
              </p>
            </Link>
            <Link
              to="/admin/ledger"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Double-Entry Ledger</h3>
              <p className="mt-1 text-xs text-slate-600">
                Verify balanced debit/credit postings and account statements.
              </p>
            </Link>
            <Link
              to="/admin/adjustments"
              className="bg-white rounded-xl border border-slate-200 p-5 hover:border-indigo-500 transition-colors shadow-sm"
            >
              <h3 className="font-semibold text-slate-900">Adjustments & Reversals</h3>
              <p className="mt-1 text-xs text-slate-600">
                Execute transaction reversals and review refund adjustments.
              </p>
            </Link>
          </div>
        </section>
      )}
    </div>
  );
}
