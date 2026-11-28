// Usage: <StatusBadge status="APPROVED" />
import React from "react";

// colours from the UI standards; the text is always shown so colour is never the only signal
const colors = {
  APPROVED: "bg-green-100 text-green-800",
  DECLINED: "bg-amber-100 text-amber-800",
  FAILED: "bg-red-100 text-red-800",
  PENDING: "bg-slate-200 text-slate-700",
  SETTLED: "bg-blue-100 text-blue-800",
  REFUNDED: "bg-purple-100 text-purple-800",
  REVERSED: "bg-slate-300 text-slate-800",
  ACTIVE: "bg-green-100 text-green-800",
  INACTIVE: "bg-slate-200 text-slate-700",
};

export default function StatusBadge({ status }) {
  const color = colors[status] || "bg-slate-100 text-slate-700";
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-semibold ${color}`}>
      {status}
    </span>
  );
}
