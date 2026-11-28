// Usage: <Select id="type" label="Type" value={type} onChange={e => setType(e.target.value)} options={[{ value: "ATM", label: "ATM" }]} />
import React from "react";

export default function Select({ id, label, options = [], placeholder, error, className = "", ...rest }) {
  return (
    <div className={className}>
      <label htmlFor={id} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <select
        id={id}
        aria-invalid={error ? "true" : "false"}
        className={`mt-1 block w-full rounded-md border bg-white px-3 py-2 text-sm
          focus:outline-none focus:ring-2 focus:ring-indigo-500
          ${error ? "border-red-500" : "border-slate-300"}`}
        {...rest}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}
