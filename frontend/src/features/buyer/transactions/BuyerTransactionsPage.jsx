import React, { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { CHANNEL_CODES, formatBdt } from "../../../shared/config.js";
import { fetchTransactions } from "./api.js";

export const TRANSACTION_STATUS_BADGES = {
  APPROVED: "bg-emerald-100 text-emerald-800 border-emerald-200",
  DECLINED: "bg-amber-100 text-amber-800 border-amber-200",
  FAILED: "bg-red-100 text-red-800 border-red-200",
  PENDING: "bg-slate-100 text-slate-700 border-slate-300",
  SETTLED: "bg-blue-100 text-blue-800 border-blue-200",
  REFUNDED: "bg-purple-100 text-purple-800 border-purple-200",
  REVERSED: "bg-slate-200 text-slate-800 border-slate-400",
};

const ALL_STATUSES = [
  "PENDING",
  "APPROVED",
  "DECLINED",
  "FAILED",
  "SETTLED",
  "REFUNDED",
  "REVERSED",
];

export function TransactionStatusBadge({ status }) {
  const normalized = (status || "PENDING").toUpperCase();
  const classes =
    TRANSACTION_STATUS_BADGES[normalized] ||
    TRANSACTION_STATUS_BADGES.PENDING;
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${classes}`}
    >
      {normalized}
    </span>
  );
}

export default function BuyerTransactionsPage() {
  const [pageData, setPageData] = useState({
    content: [],
    number: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [statusFilter, setStatusFilter] = useState("");
  const [channelFilter, setChannelFilter] = useState("");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const loadTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const fromIso = fromDate ? new Date(`${fromDate}T00:00:00Z`).toISOString() : undefined;
      const toIso = toDate ? new Date(`${toDate}T23:59:59Z`).toISOString() : undefined;
      const result = await fetchTransactions({
        page,
        size,
        status: statusFilter || undefined,
        channel: channelFilter || undefined,
        from: fromIso,
        to: toIso,
      });
      setPageData(result);
    } catch (err) {
      setError(err?.message || "Failed to load transaction history.");
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter, channelFilter, fromDate, toDate]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const handleResetFilters = () => {
    setStatusFilter("");
    setChannelFilter("");
    setFromDate("");
    setToDate("");
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Transaction History
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Filter and inspect transactions across ATM, Mobile, and Web Checkout channels.
          </p>
        </div>
        <button
          type="button"
          onClick={loadTransactions}
          disabled={loading}
          className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {/* C3 Filter Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 items-end">
          <div>
            <label
              htmlFor="filter-status"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Status
            </label>
            <select
              id="filter-status"
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:outline-none"
            >
              <option value="">All Statuses</option>
              {ALL_STATUSES.map((st) => (
                <option key={st} value={st}>
                  {st}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label
              htmlFor="filter-channel"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Channel
            </label>
            <select
              id="filter-channel"
              value={channelFilter}
              onChange={(e) => {
                setChannelFilter(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:outline-none"
            >
              <option value="">All Channels</option>
              {CHANNEL_CODES.map((ch) => (
                <option key={ch} value={ch}>
                  {ch}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label
              htmlFor="filter-from-date"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              From Date
            </label>
            <input
              id="filter-from-date"
              type="date"
              value={fromDate}
              onChange={(e) => {
                setFromDate(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:outline-none"
            />
          </div>

          <div>
            <label
              htmlFor="filter-to-date"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              To Date
            </label>
            <input
              id="filter-to-date"
              type="date"
              value={toDate}
              onChange={(e) => {
                setToDate(e.target.value);
                setPage(0);
              }}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-indigo-600 focus:outline-none"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleResetFilters}
              className="w-full px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50"
            >
              Clear Filters
            </button>
          </div>
        </div>
      </div>

      {/* Paged Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-slate-900">
            Transactions ({pageData.totalElements})
          </h2>
          <div className="flex items-center gap-2 text-xs text-slate-600">
            <label htmlFor="page-size-select">Rows per page:</label>
            <select
              id="page-size-select"
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value));
                setPage(0);
              }}
              className="rounded border border-slate-300 px-2 py-1 text-xs text-slate-800"
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            Loading transactions...
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
                onClick={loadTransactions}
                className="px-3 py-1.5 rounded-md text-xs font-semibold bg-red-100 text-red-800 hover:bg-red-200"
              >
                Retry
              </button>
            </div>
          </div>
        ) : pageData.content.length === 0 ? (
          <div className="p-8 text-center space-y-2">
            <p className="text-sm font-medium text-slate-700">
              No transactions match your current filters.
            </p>
            <p className="text-xs text-slate-500">
              Submit an ATM withdrawal, mobile transfer, or web checkout payment to see transactions here.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                <tr>
                  <th className="px-6 py-3">Reference</th>
                  <th className="px-6 py-3">Channel</th>
                  <th className="px-6 py-3">Type</th>
                  <th className="px-6 py-3">Source / Dest</th>
                  <th className="px-6 py-3 text-right">Amount (BDT)</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Timestamp</th>
                  <th className="px-6 py-3 text-right">Detail</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {pageData.content.map((txn) => (
                  <tr key={txn.reference} className="hover:bg-slate-50">
                    <td className="px-6 py-4 font-mono text-xs font-semibold text-indigo-700">
                      <Link
                        to={`/transactions/${txn.reference}`}
                        className="hover:underline"
                      >
                        {txn.reference}
                      </Link>
                    </td>
                    <td className="px-6 py-4 text-xs font-semibold text-slate-700">
                      {txn.channelCode || "—"}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-700">
                      {txn.type || "—"}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-slate-600">
                      {txn.sourceAccountNo || "—"}
                      {txn.destinationAccountNo
                        ? ` -> ${txn.destinationAccountNo}`
                        : ""}
                    </td>
                    <td className="px-6 py-4 text-right font-mono font-semibold text-slate-900">
                      {formatBdt(txn.amount)}
                    </td>
                    <td className="px-6 py-4">
                      <TransactionStatusBadge status={txn.status} />
                      {txn.status === "DECLINED" && txn.declineReason && (
                        <p className="mt-1 text-xs text-amber-700">
                          {txn.declineReason}
                        </p>
                      )}
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-500">
                      {txn.createdAt
                        ? new Date(txn.createdAt).toLocaleString()
                        : "—"}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link
                        to={`/transactions/${txn.reference}`}
                        className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-slate-300 text-slate-700 hover:bg-slate-100"
                      >
                        Track
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-600">
          <span>
            Page <span className="font-semibold">{pageData.number + 1}</span> of{" "}
            <span className="font-semibold">
              {Math.max(1, pageData.totalPages)}
            </span>{" "}
            ({pageData.totalElements} total records)
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={page <= 0 || loading}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1.5 rounded-md border border-slate-300 bg-white text-slate-700 hover:bg-slate-100 disabled:opacity-40"
            >
              Previous
            </button>
            <button
              type="button"
              disabled={
                loading ||
                pageData.totalPages === 0 ||
                page + 1 >= pageData.totalPages
              }
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1.5 rounded-md border border-slate-300 bg-white text-slate-700 hover:bg-slate-100 disabled:opacity-40"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
