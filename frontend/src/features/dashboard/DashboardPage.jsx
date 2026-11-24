import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../shared/auth/AuthContext.jsx";
import { ROLES } from "../../shared/auth/roles.js";
import { CHANNEL_CODES, CURRENCY, formatBdt } from "../../shared/config.js";
import { TransactionStatusBadge } from "../buyer/transactions/BuyerTransactionsPage.jsx";
import { fetchBuyerDashboardData } from "./api.js";

export default function DashboardPage() {
  const { user, roles, hasRole } = useAuth();
  const isBuyer = hasRole(ROLES.BUYER);

  const [buyerWidgets, setBuyerWidgets] = useState({
    loading: false,
    accounts: [],
    accountsError: null,
    recentTransactions: [],
    transactionsError: null,
  });

  const loadBuyerWidgets = useCallback(async () => {
    if (!isBuyer) return;
    setBuyerWidgets((prev) => ({ ...prev, loading: true }));
    const data = await fetchBuyerDashboardData();
    setBuyerWidgets({
      loading: false,
      ...data,
    });
  }, [isBuyer]);

  useEffect(() => {
    loadBuyerWidgets();
  }, [loadBuyerWidgets]);

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

      {isBuyer && (
        <>
          <section className="space-y-3">
            <h2 className="text-lg font-semibold text-slate-900">
              Customer (BUYER) Quick Actions
            </h2>
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

          {/* BUYER Dashboard Widgets: Account Balances & Last 5 Transactions */}
          <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Widget 1: Account Balances */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
                <h3 className="font-semibold text-slate-900">My Account Balances</h3>
                <Link
                  to="/accounts"
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                >
                  Open Accounts &rarr;
                </Link>
              </div>
              <div className="p-5 flex-1">
                {buyerWidgets.loading ? (
                  <p className="text-sm text-slate-500">Loading account balances...</p>
                ) : buyerWidgets.accountsError ? (
                  <div className="text-xs text-slate-600 space-y-2">
                    <p>{buyerWidgets.accountsError}</p>
                    <Link
                      to="/accounts"
                      className="inline-block font-semibold text-indigo-600 hover:underline"
                    >
                      Go to Balance Inquiry
                    </Link>
                  </div>
                ) : buyerWidgets.accounts.length === 0 ? (
                  <p className="text-sm text-slate-500">
                    No linked accounts yet. Visit{" "}
                    <Link to="/accounts" className="text-indigo-600 font-semibold hover:underline">
                      My Accounts
                    </Link>{" "}
                    to inquire a balance.
                  </p>
                ) : (
                  <ul className="divide-y divide-slate-200 text-sm">
                    {buyerWidgets.accounts.map((acc) => {
                      const no = acc.accountNo || acc.account_no;
                      return (
                        <li
                          key={acc.id || no}
                          className="py-3 flex items-center justify-between gap-2"
                        >
                          <div>
                            <p className="font-mono font-semibold text-slate-900">
                              {no}
                            </p>
                            <p className="text-xs text-slate-500">
                              {acc.type || "SAVINGS"} • {acc.status || "ACTIVE"}
                            </p>
                          </div>
                          <span className="font-mono font-bold text-slate-900">
                            {formatBdt(acc.balance)}
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            </div>

            {/* Widget 2: Last 5 Transactions */}
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
              <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
                <h3 className="font-semibold text-slate-900">Last 5 Transactions</h3>
                <Link
                  to="/transactions"
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800"
                >
                  View All &rarr;
                </Link>
              </div>
              <div className="p-5 flex-1">
                {buyerWidgets.loading ? (
                  <p className="text-sm text-slate-500">Loading recent transactions...</p>
                ) : buyerWidgets.transactionsError ? (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-xs text-red-700 flex items-center justify-between">
                    <span>{buyerWidgets.transactionsError}</span>
                    <button
                      type="button"
                      onClick={loadBuyerWidgets}
                      className="font-semibold underline"
                    >
                      Retry
                    </button>
                  </div>
                ) : buyerWidgets.recentTransactions.length === 0 ? (
                  <p className="text-sm text-slate-500">
                    No recent transactions recorded yet.
                  </p>
                ) : (
                  <ul className="divide-y divide-slate-200 text-sm">
                    {buyerWidgets.recentTransactions.map((txn) => (
                      <li
                        key={txn.reference}
                        className="py-2.5 flex items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <Link
                            to={`/transactions/${txn.reference}`}
                            className="font-mono text-xs font-semibold text-indigo-700 hover:underline block truncate"
                          >
                            {txn.reference}
                          </Link>
                          <p className="text-xs text-slate-500">
                            {txn.channelCode || "SWITCH"} • {txn.type}
                          </p>
                        </div>
                        <div className="flex items-center gap-3 shrink-0">
                          <span className="font-mono font-semibold text-slate-900">
                            {formatBdt(txn.amount)}
                          </span>
                          <TransactionStatusBadge status={txn.status} />
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </section>
        </>
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
