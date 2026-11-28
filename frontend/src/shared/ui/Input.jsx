// Usage: <Input id="email" label="Email" value={email} onChange={e => setEmail(e.target.value)} error={errors.email} />
import React from "react";

export default function Input({ id, label, error, className = "", ...rest }) {
  return (
    <div className={className}>
      <label htmlFor={id} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? "true" : "false"}
        aria-describedby={error ? `${id}-error` : undefined}
        className={`mt-1 block w-full rounded-md border px-3 py-2 text-sm
          focus:outline-none focus:ring-2 focus:ring-indigo-500
          ${error ? "border-red-500" : "border-slate-300"}`}
        {...rest}
      />
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
