// Usage: <MoneyText amount={1500.5} />  ->  BDT 1,500.50
import React from "react";

const formatter = new Intl.NumberFormat("en-BD", {
  style: "currency",
  currency: "BDT",
  minimumFractionDigits: 2,
});

export default function MoneyText({ amount, className = "" }) {
  // the backend sends amounts as numbers or strings, we only format them, never calculate
  const value = Number(amount ?? 0);
  return <span className={`tabular-nums ${className}`}>{formatter.format(value)}</span>;
}
