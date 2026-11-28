import React, { useCallback, useEffect, useMemo, useState } from "react";
import { fetchAuditLogs } from "./api.js";

export default function AdminAuditPage() {
  const [rawResult, setRawResult] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [actionFilter, setActionFilter] = useState("");
  const [targetFilter, setTargetFilter] = useState("");

  const loadLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAuditLogs({ page, size });
      setRawResult(data);
    } catch (err) {
      setError(err?.message || "Failed to load audit logs.");
    } finally {
      setLoading(false);
    }
  }, [page, size]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  const isPaged =
    rawResult && !Array.isArray(rawResult) && Array.isArray(rawResult.content);
  const entries = useMemo(() => {
    const list = isPaged
      ? rawResult.content
      : Array.isArray(rawResult)
      ? rawResult
      : [];
    const actionQ = actionFilter.trim().toLowerCase();
    const targetQ = targetFilter.trim().toLowerCase();
    return list.filter((entry) => {
      if (actionQ && !(entry.action || "").toLowerCase().includes(actionQ)) {
        return false;
      }
      if (
        targetQ &&
        !(entry.target || "").toLowerCase().includes(targetQ) &&
        !(entry.details || "").toLowerCase().includes(targetQ)
      ) {
        return false;
      }
      return true;
    });
  }, [rawResult, isPaged, actionFilter, targetFilter]);

  const totalPages = isPaged ? rawResult.totalPages ?? 1 : 1;
  const totalElements = isPaged ? rawResult.totalElements ?? entries.length : entries.length;

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Audit Logs</h1>
          <p className="mt-1 text-sm text-slate-600">
            Immutable security and transaction decision trail recorded by AuditService.
          </p>
        </div>
        <button
          type="button"
          onClick={loadLogs}
          disabled={loading}
          className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          {error}
        </div>
      )}

      {/* Filter Bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label
              htmlFor="audit-action-filter"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Action
            </label>
            <input
              id="audit-action-filter"
              type="text"
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              placeholder="e.g. LOGIN, LOGOUT, TRANSACTION_APPROVED"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label
              htmlFor="audit-target-filter"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Target or Details
            </label>
            <input
              id="audit-target-filter"
              type="text"
              value={targetFilter}
              onChange={(e) => setTargetFilter(e.target.value)}
              placeholder="Reference, user email, or details"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
        </div>
      </div>

      {/* Audit Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            Loading audit logs...
          </div>
        ) : entries.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            No audit log entries found.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                <tr>
                  <th className="px-4 py-3">Timestamp</th>
                  <th className="px-4 py-3">Actor ID</th>
                  <th className="px-4 py-3">Action</th>
                  <th className="px-4 py-3">Target</th>
                  <th className="px-4 py-3">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {entries.map((entry) => (
                  <tr key={entry.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                      {entry.createdAt
                        ? new Date(entry.createdAt).toLocaleString()
                        : "—"}
                    </td>
                    <td className="px-4 py-3 text-xs font-mono text-slate-700 whitespace-nowrap">
                      {entry.actorUserId ?? "system"}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-800 border border-slate-300">
                        {entry.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs font-mono text-slate-700">
                      {entry.target || "—"}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-600 max-w-md break-words">
                      {entry.details || "—"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {isPaged && totalPages > 1 && (
          <div className="px-4 py-3 border-t border-slate-200 bg-slate-50 flex items-center justify-between text-xs text-slate-600">
            <span>
              Page {page + 1} of {totalPages} ({totalElements} total)
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={page === 0 || loading}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1 rounded border border-slate-300 bg-white hover:bg-slate-100 disabled:opacity-50"
              >
                Previous
              </button>
              <button
                type="button"
                disabled={page + 1 >= totalPages || loading}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 rounded border border-slate-300 bg-white hover:bg-slate-100 disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
