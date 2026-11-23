import React from "react";
import { Routes, Route } from "react-router-dom";

function Home() {
  return (
    <main className="min-h-screen flex flex-col items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-xl shadow-sm border border-slate-200 p-8 text-center">
        <h1 className="text-3xl font-bold text-slate-900">SwitchPay</h1>
        <p className="mt-2 text-sm text-slate-600">
          Multi-Channel Banking and Payment Platform
        </p>
      </div>
    </main>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="*" element={<Home />} />
    </Routes>
  );
}
