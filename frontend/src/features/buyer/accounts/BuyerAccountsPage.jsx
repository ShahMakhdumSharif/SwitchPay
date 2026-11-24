import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { formatBdt } from "../../../shared/config.js";
import { fetchAccountBalance, fetchMyAccounts } from "./api.js";

const ACCOUNT_STATUS_STYLES = {
  ACTIVE: "bg-emerald-100 text-emerald-800 border-emerald-200",
  FROZEN: "bg-amber-100 text-amber-800 border-amber-200",
  CLOSED: "bg-slate-200 text-slate-700 border-slate-300",
};

export default function BuyerAccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [inquiryInput, setInquiryInput] = useState("");
  const [inquiryLoading, setInquiryLoading] = useState(false);
  const [inquiryError, setInquiryError] = useState(null);
  const [inquiryResult, setInquiryResult] = useState(null);

  const loadAccounts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await fetchMyAccounts();
      setAccounts(list);
      if (list.length > 0 && !inquiryInput) {
        setInquiryInput(list[0].accountNo || list[0].account_no || "");
      }
    } catch (err) {
      setError(err?.message || "Unable to load your accounts right now.");
    } finally {
      setLoading(false);
    }
  }, [inquiryInput]);

  useEffect(() => {
    loadAccounts();
  }, [loadAccounts]);

  const handleBalanceInquiry = async (accountNoToQuery) => {
    const targetNo = (accountNoToQuery ?? inquiryInput).trim();
    if (!targetNo) {
      setInquiryError("Please enter or select an account number.");
      return;
    }
    setInquiryInput(targetNo);
    setInquiryLoading(true);
    setInquiryError(null);
    setInquiryResult(null);
    try {
      const data = await fetchAccountBalance(targetNo);
      const resolvedBalance =
        typeof data === "number"
          ? data
          : data?.balance ?? data?.availableBalance ?? 0;
      setInquiryResult({
        accountNo: data?.accountNo || targetNo,
        balance: resolvedBalance,
        currency: data?.currency || "BDT",
        status: data?.status || "ACTIVE",
        checkedAt: new Date().toLocaleTimeString(),
      });
    } catch (err) {
      setInquiryError(
        err?.message || `Could not inquire balance for account ${targetNo}.`
      );
    } finally {
      setInquiryLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            My Accounts & Balance Inquiry
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            View your core banking accounts and perform real-time BDT balance inquiries from the server.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={loadAccounts}
            disabled={loading}
            className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            {loading ? "Refreshing..." : "Refresh Accounts"}
          </button>
          <Link
            to="/transactions"
            className="px-4 py-2 rounded-lg text-sm font-semibold bg-indigo-600 text-white hover:bg-indigo-700"
          >
            View Transactions
          </Link>
        </div>
      </div>

      {/* Real-time Balance Inquiry Card */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm space-y-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">
            Real-Time Balance Inquiry
          </h2>
          <p className="text-xs text-slate-500">
            Queries <code className="font-mono">GET /api/accounts/&#123;no&#125;/balance</code> directly; balances are never cached in the browser.
          </p>
        </div>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleBalanceInquiry(inquiryInput);
          }}
          className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-end"
        >
          <div className="flex-1">
            <label
              htmlFor="balance-inquiry-account-no"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Account Number
            </label>
            <input
              id="balance-inquiry-account-no"
              type="text"
              value={inquiryInput}
              onChange={(e) => setInquiryInput(e.target.value)}
              placeholder="e.g. ACC-1001"
              className="w-full rounded-lg border border-slate-300 px-3.5 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
            />
          </div>
          <button
            type="submit"
            disabled={inquiryLoading}
            className="px-5 py-2.5 rounded-lg text-sm font-semibold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {inquiryLoading ? "Checking Balance..." : "Check Balance"}
          </button>
        </form>

        {inquiryError && (
          <div
            role="alert"
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
          >
            {inquiryError}
          </div>
        )}

        {inquiryResult && (
          <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3.5 flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">
                Live Balance Inquiry Result
              </p>
              <p className="text-sm font-medium text-emerald-950 mt-0.5">
                Account <span className="font-mono font-bold">{inquiryResult.accountNo}</span>
                {" • "}Status:{" "}
                <span className="font-semibold">{inquiryResult.status}</span>
              </p>
              <p className="text-xs text-emerald-700 mt-0.5">
                Verified at {inquiryResult.checkedAt}
              </p>
            </div>
            <div className="text-right">
              <span className="text-xs text-emerald-700 block">Available Balance</span>
              <span className="text-xl font-bold text-emerald-900 font-mono">
                {formatBdt(inquiryResult.balance)}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Accounts List Section */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Your Bank Accounts</h2>
          <span className="text-xs font-medium text-slate-500">
            {accounts.length} {accounts.length === 1 ? "account" : "accounts"}
          </span>
        </div>

        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            Loading your accounts...
          </div>
        ) : error ? (
          <div className="p-6">
            <div
              role="alert"
              className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex flex-wrap items-center justify-between gap-3"
            >
              <span>{error}</span>
              <button
                type="button"
                onClick={loadAccounts}
                className="px-3 py-1.5 rounded-md text-xs font-semibold bg-red-100 text-red-800 hover:bg-red-200"
              >
                Retry
              </button>
            </div>
          </div>
        ) : accounts.length === 0 ? (
          <div className="p-8 text-center space-y-2">
            <p className="text-sm font-medium text-slate-700">
              No bank accounts found for your profile yet.
            </p>
            <p className="text-xs text-slate-500">
              You can still inquire an account balance above by entering its account number, or ask an administrator to provision a SAVINGS or CURRENT account.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                <tr>
                  <th className="px-6 py-3">Account Number</th>
                  <th className="px-6 py-3">Type</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3 text-right">Balance (BDT)</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {accounts.map((acc) => {
                  const accountNo = acc.accountNo || acc.account_no;
                  const status = acc.status || "ACTIVE";
                  const badgeStyle =
                    ACCOUNT_STATUS_STYLES[status] ||
                    "bg-slate-100 text-slate-700 border-slate-200";
                  return (
                    <tr key={acc.id || accountNo} className="hover:bg-slate-50">
                      <td className="px-6 py-4 font-mono font-semibold text-slate-900">
                        {accountNo}
                      </td>
                      <td className="px-6 py-4 text-slate-700">
                        {acc.type || "SAVINGS"}
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${badgeStyle}`}
                        >
                          {status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right font-mono font-semibold text-slate-900">
                        {formatBdt(acc.balance)}
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        <button
                          type="button"
                          onClick={() => handleBalanceInquiry(accountNo)}
                          disabled={inquiryLoading}
                          className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-indigo-200 text-indigo-700 hover:bg-indigo-50 disabled:opacity-50"
                        >
                          Inquire Live Balance
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
