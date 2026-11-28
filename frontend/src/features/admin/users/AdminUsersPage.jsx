import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../../shared/auth/AuthContext.jsx";
import { deactivateUser, fetchUsers } from "./api.js";

const ROLE_BADGE_CLASSES = {
  ADMIN: "bg-indigo-100 text-indigo-800 border-indigo-200",
  SELLER: "bg-amber-100 text-amber-800 border-amber-200",
  BUYER: "bg-sky-100 text-sky-800 border-sky-200",
};

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  const [confirmTarget, setConfirmTarget] = useState(null);
  const [deactivatingId, setDeactivatingId] = useState(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchUsers();
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err?.message || "Failed to load users.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const filteredUsers = useMemo(() => {
    const q = query.trim().toLowerCase();
    return users.filter((u) => {
      if (q) {
        const matchesText =
          String(u.id).includes(q) ||
          (u.email || "").toLowerCase().includes(q) ||
          (u.fullName || "").toLowerCase().includes(q);
        if (!matchesText) return false;
      }
      if (roleFilter && !(u.roles || []).includes(roleFilter)) {
        return false;
      }
      if (statusFilter === "ACTIVE" && !u.active) return false;
      if (statusFilter === "INACTIVE" && u.active) return false;
      return true;
    });
  }, [users, query, roleFilter, statusFilter]);

  const handleConfirmDeactivate = async () => {
    if (!confirmTarget) return;
    setDeactivatingId(confirmTarget.id);
    setError(null);
    setSuccessMessage(null);
    try {
      await deactivateUser(confirmTarget.id);
      setUsers((prev) =>
        prev.map((u) =>
          u.id === confirmTarget.id ? { ...u, active: false } : u
        )
      );
      setSuccessMessage(`User ${confirmTarget.email} has been deactivated.`);
      setConfirmTarget(null);
    } catch (err) {
      setError(err?.message || "Failed to deactivate user.");
      setConfirmTarget(null);
    } finally {
      setDeactivatingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">User Management</h1>
          <p className="mt-1 text-sm text-slate-600">
            Inspect registered platform users, assigned roles, and deactivate accounts.
          </p>
        </div>
        <button
          type="button"
          onClick={loadUsers}
          disabled={loading}
          className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {successMessage && (
        <div
          role="status"
          className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800 flex items-center justify-between gap-3"
        >
          <span>{successMessage}</span>
          <button
            type="button"
            onClick={() => setSuccessMessage(null)}
            className="text-xs font-semibold text-emerald-700 hover:underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div
          role="alert"
          className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
        >
          {error}
        </div>
      )}

      {/* Filter bar */}
      <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label
              htmlFor="user-search"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Search
            </label>
            <input
              id="user-search"
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Name, email, or ID"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label
              htmlFor="user-role-filter"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Role
            </label>
            <select
              id="user-role-filter"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">All roles</option>
              <option value="ADMIN">ADMIN</option>
              <option value="SELLER">SELLER</option>
              <option value="BUYER">BUYER</option>
            </select>
          </div>
          <div>
            <label
              htmlFor="user-status-filter"
              className="block text-xs font-semibold uppercase tracking-wider text-slate-600 mb-1"
            >
              Status
            </label>
            <select
              id="user-status-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="">All statuses</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
            </select>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-500">
            Loading users...
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            No users match the current filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                <tr>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Roles</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Created</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {filteredUsers.map((u) => {
                  const isSelf = currentUser?.id === u.id;
                  const isBusy = deactivatingId === u.id;
                  return (
                    <tr key={u.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <div className="font-medium text-slate-900">
                          {u.fullName}{" "}
                          <span className="text-xs text-slate-400">#{u.id}</span>
                        </div>
                        <div className="text-xs text-slate-500">{u.email}</div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-1">
                          {(u.roles || []).map((role) => (
                            <span
                              key={role}
                              className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold border ${
                                ROLE_BADGE_CLASSES[role] ||
                                "bg-slate-100 text-slate-700 border-slate-300"
                              }`}
                            >
                              {role}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${
                            u.active
                              ? "bg-emerald-100 text-emerald-800 border-emerald-200"
                              : "bg-slate-200 text-slate-700 border-slate-300"
                          }`}
                        >
                          {u.active ? "ACTIVE" : "INACTIVE"}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                        {u.createdAt
                          ? new Date(u.createdAt).toLocaleString()
                          : "—"}
                      </td>
                      <td className="px-4 py-3 text-right whitespace-nowrap">
                        {u.active ? (
                          <button
                            type="button"
                            disabled={isSelf || isBusy}
                            onClick={() => setConfirmTarget(u)}
                            title={
                              isSelf
                                ? "Cannot deactivate your own active admin session"
                                : "Deactivate user"
                            }
                            className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-red-200 text-red-700 bg-red-50 hover:bg-red-100 disabled:opacity-40 disabled:cursor-not-allowed"
                          >
                            {isBusy
                              ? "Deactivating..."
                              : isSelf
                              ? "Current Admin"
                              : "Deactivate"}
                          </button>
                        ) : (
                          <span className="text-xs text-slate-400">
                            Deactivated
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Confirmation Dialog */}
      {confirmTarget && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-deactivate-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4"
        >
          <div className="bg-white rounded-xl border border-slate-200 max-w-md w-full p-6 shadow-xl space-y-4">
            <h2
              id="confirm-deactivate-title"
              className="text-lg font-bold text-slate-900"
            >
              Deactivate user?
            </h2>
            <p className="text-sm text-slate-600">
              <span className="font-semibold text-slate-900">
                {confirmTarget.fullName}
              </span>{" "}
              ({confirmTarget.email}) will no longer be able to log in. Existing
              audit and transaction records remain intact.
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={deactivatingId !== null}
                onClick={() => setConfirmTarget(null)}
                className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deactivatingId !== null}
                onClick={handleConfirmDeactivate}
                className="px-4 py-2 rounded-lg text-sm font-semibold bg-red-600 text-white hover:bg-red-700 disabled:opacity-50"
              >
                {deactivatingId !== null ? "Deactivating..." : "Confirm Deactivate"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
