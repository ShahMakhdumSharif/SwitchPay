// Usage: <Toast type="success" message="Merchant saved" onClose={() => setToast(null)} />
import React, { useEffect } from "react";

const styles = {
  success: "bg-green-600",
  error: "bg-red-600",
  info: "bg-slate-800",
};

export default function Toast({ message, type = "info", onClose, duration = 4000 }) {
  useEffect(() => {
    if (!message) return undefined;
    const timer = setTimeout(onClose, duration);
    return () => clearTimeout(timer);
  }, [message, duration, onClose]);

  if (!message) return null;

  return (
    <div
      role="status"
      className={`fixed bottom-4 right-4 z-50 flex items-center gap-3 rounded-md px-4 py-3 text-sm text-white shadow-lg ${styles[type]}`}
    >
      <span>{message}</span>
      <button onClick={onClose} aria-label="Close message" className="font-bold">
        x
      </button>
    </div>
  );
}
