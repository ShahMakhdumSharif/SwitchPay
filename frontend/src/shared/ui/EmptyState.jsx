// Usage: <EmptyState title="No merchants yet" message="Register your shop to start." action={<Button>Register</Button>} />
import React from "react";

export default function EmptyState({ title, message, action }) {
  return (
    <div className="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
      <p className="font-medium text-slate-800">{title}</p>
      {message && <p className="mt-1 text-sm text-slate-500">{message}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
