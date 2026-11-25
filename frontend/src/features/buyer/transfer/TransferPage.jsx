import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  CHANNELS,
  createIdempotencyKey,
  formatBdt,
} from "../../../shared/config.js";
import {
  fetchBuyerAccountsForMobile,
  submitMobileTransaction,
} from "./api.js";

export default function TransferPage() {
  const [accounts, setAccounts] = useState([]);
  const [mode, setMode] = useState("TRANSFER");
  const [sourceAccountNo, setSourceAccountNo] = useState("ACC-2001");
  const [destinationAccountNo, setDestinationAccountNo] = useState("ACC-2002");
  const [amount, setAmount] = useState("500.00");
  const [idempotencyKey, setIdempotencyKey] = useState(() =>
    createIdempotencyKey()
  );

  const [submitting, setSubmitting] = useState(false);
  const [outcome, setOutcome] = useState(null);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    let mounted = true;
    fetchBuyerAccountsForMobile().then((list) => {
      if (!mounted) return;
      setAccounts(list);
      if (list.length > 0) {
        const firstNo = list[0].accountNo || list[0].account_no;
        if (firstNo) setSourceAccountNo(firstNo);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const rotateKeyIfCompleted = () => {
    if (outcome) {
      setIdempotencyKey(createIdempotencyKey());
      setOutcome(null);
    }
    setError(null);
    setFieldErrors({});
  };

  const validateForm = () => {
    const errs = {};
    if (!sourceAccountNo || !sourceAccountNo.trim()) {
      errs.sourceAccountNo = "From account is required.";
    }
    if (mode === "TRANSFER") {
      if (!destinationAccountNo || !destinationAccountNo.trim()) {
        errs.destinationAccountNo = "Recipient account number is required.";
      } else if (
        sourceAccountNo.trim() === destinationAccountNo.trim()
      ) {
        errs.destinationAccountNo =
          "Source and destination accounts must be different.";
      }
      const parsed = Number(amount);
      if (!amount || Number.isNaN(parsed) || parsed <= 0) {
        errs.amount = "Transfer amount must be greater than 0 BDT.";
      } else if (!/^\d+(\.\d{1,2})?$/.test(String(amount).trim())) {
        errs.amount = "Use at most two decimal places.";
      }
    }
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    if (submitting) return;
    if (!validateForm()) return;

    setSubmitting(true);
    setError(null);

    try {
      const res = await submitMobileTransaction({
        type: mode,
        sourceAccountNo,
        destinationAccountNo:
          mode === "TRANSFER" ? destinationAccountNo : undefined,
        amount: mode === "TRANSFER" ? amount : "0.01",
        idempotencyKey,
      });
      setOutcome(res);
    } catch (err) {
      if (err?.fieldErrors && typeof err.fieldErrors === "object") {
        setFieldErrors(err.fieldErrors);
      }
      setError({
        status: err?.status,
        message:
          err?.message ||
          "Mobile banking request could not be completed. Retry safely.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setIdempotencyKey(createIdempotencyKey());
    setOutcome(null);
    setError(null);
    setFieldErrors({});
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            Mobile Banking Simulator
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Channel{" "}
            <span className="font-mono font-semibold">{CHANNELS.MOBILE}</span> •
            Instant account-to-account BDT fund transfers and mobile balance checks.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/accounts"
            className="px-3.5 py-2 rounded-lg text-xs font-semibold border border-slate-300 text-slate-700 hover:bg-slate-50"
          >
            My Accounts
          </Link>
          <Link
            to="/transactions"
            className="px-3.5 py-2 rounded-lg text-xs font-semibold bg-indigo-600 text-white hover:bg-indigo-700"
          >
            Transaction History
          </Link>
        </div>
      </div>

      {/* Smartphone Device Frame (usable down to 375px) */}
      <div className="max-w-md mx-auto">
        <div className="rounded-[2.5rem] bg-slate-900 p-3.5 shadow-2xl border-4 border-slate-800">
          {/* Phone Notch / Status Bar */}
          <div className="flex items-center justify-between px-4 pt-1 pb-3 text-[11px] font-semibold text-slate-300">
            <span>9:41</span>
            <div className="h-3.5 w-24 rounded-full bg-black" />
            <span>5G • 100%</span>
          </div>

          {/* Mobile App Screen */}
          <div className="rounded-[1.75rem] bg-white overflow-hidden border border-slate-200">
            {/* App Top Banner */}
            <div className="bg-indigo-600 px-5 py-4 text-white">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-200">
                  SwitchPay Mobile
                </span>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-indigo-700 text-indigo-100">
                  {CHANNELS.MOBILE}
                </span>
              </div>
              <h2 className="mt-1 text-lg font-bold">
                {mode === "TRANSFER"
                  ? "Send Money (BDT)"
                  : "Mobile Balance Check"}
              </h2>
            </div>

            <div className="p-5 space-y-4">
              {/* Mode Toggle Tabs */}
              <div className="grid grid-cols-2 gap-2 bg-slate-100 p-1 rounded-xl">
                <button
                  type="button"
                  disabled={submitting}
                  onClick={() => {
                    setMode("TRANSFER");
                    rotateKeyIfCompleted();
                  }}
                  className={`py-2 rounded-lg text-xs font-bold transition-colors ${
                    mode === "TRANSFER"
                      ? "bg-white text-indigo-700 shadow-sm"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Fund Transfer
                </button>
                <button
                  type="button"
                  disabled={submitting}
                  onClick={() => {
                    setMode("BALANCE_INQUIRY");
                    rotateKeyIfCompleted();
                  }}
                  className={`py-2 rounded-lg text-xs font-bold transition-colors ${
                    mode === "BALANCE_INQUIRY"
                      ? "bg-white text-indigo-700 shadow-sm"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  Balance Inquiry
                </button>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4" noValidate>
                <div>
                  <label
                    htmlFor="mobile-source-account"
                    className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1"
                  >
                    From Account
                  </label>
                  {accounts.length > 0 && (
                    <select
                      aria-label="Select source account"
                      value={sourceAccountNo}
                      disabled={submitting}
                      onChange={(e) => {
                        setSourceAccountNo(e.target.value);
                        rotateKeyIfCompleted();
                      }}
                      className="mb-2 w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-xs text-slate-800"
                    >
                      {accounts.map((acc) => {
                        const no = acc.accountNo || acc.account_no;
                        return (
                          <option key={acc.id || no} value={no}>
                            {no} ({formatBdt(acc.balance)})
                          </option>
                        );
                      })}
                    </select>
                  )}
                  <input
                    id="mobile-source-account"
                    type="text"
                    value={sourceAccountNo}
                    disabled={submitting}
                    onChange={(e) => {
                      setSourceAccountNo(e.target.value);
                      rotateKeyIfCompleted();
                    }}
                    placeholder="e.g. ACC-2001"
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm font-mono text-slate-900 focus:border-indigo-600 focus:outline-none"
                  />
                  {fieldErrors.sourceAccountNo && (
                    <p className="mt-1 text-xs text-red-600">
                      {fieldErrors.sourceAccountNo}
                    </p>
                  )}
                </div>

                {mode === "TRANSFER" && (
                  <>
                    <div>
                      <label
                        htmlFor="mobile-dest-account"
                        className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1"
                      >
                        To Recipient Account
                      </label>
                      <input
                        id="mobile-dest-account"
                        type="text"
                        value={destinationAccountNo}
                        disabled={submitting}
                        onChange={(e) => {
                          setDestinationAccountNo(e.target.value);
                          rotateKeyIfCompleted();
                        }}
                        placeholder="e.g. ACC-2002"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm font-mono text-slate-900 focus:border-indigo-600 focus:outline-none"
                      />
                      {fieldErrors.destinationAccountNo && (
                        <p className="mt-1 text-xs text-red-600">
                          {fieldErrors.destinationAccountNo}
                        </p>
                      )}
                    </div>

                    <div>
                      <label
                        htmlFor="mobile-amount"
                        className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-1"
                      >
                        Amount (BDT)
                      </label>
                      <input
                        id="mobile-amount"
                        type="number"
                        step="0.01"
                        min="0.01"
                        value={amount}
                        disabled={submitting}
                        onChange={(e) => {
                          setAmount(e.target.value);
                          rotateKeyIfCompleted();
                        }}
                        placeholder="500.00"
                        className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-base font-mono font-bold text-slate-900 focus:border-indigo-600 focus:outline-none"
                      />
                      {fieldErrors.amount && (
                        <p className="mt-1 text-xs text-red-600">
                          {fieldErrors.amount}
                        </p>
                      )}
                    </div>
                  </>
                )}

                <div className="rounded-lg bg-slate-50 border border-slate-200 p-2.5 text-[11px] text-slate-500 flex items-center justify-between gap-2">
                  <span className="truncate font-mono">
                    Key: {idempotencyKey}
                  </span>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handleReset}
                    className="text-indigo-600 font-semibold shrink-0 hover:underline"
                  >
                    Reset
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full py-3 rounded-xl text-sm font-bold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 shadow-sm"
                >
                  {submitting
                    ? "Processing on Switch..."
                    : mode === "TRANSFER"
                    ? `Send ${formatBdt(amount)}`
                    : "Inquire Balance"}
                </button>
              </form>

              {error && (
                <div
                  role="alert"
                  className="rounded-xl border border-red-200 bg-red-50 p-3.5 text-xs text-red-800 space-y-2"
                >
                  <p className="font-bold">{error.message}</p>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handleSubmit}
                    className="px-3 py-1 rounded-md font-bold bg-red-100 text-red-900 hover:bg-red-200"
                  >
                    Retry Safely (Reuses Idempotency Key)
                  </button>
                </div>
              )}

              {outcome && outcome.status === "APPROVED" && (
                <div className="rounded-xl border border-emerald-300 bg-emerald-50 p-4 text-emerald-950 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-emerald-200 text-emerald-900">
                      APPROVED
                    </span>
                    <span className="text-[11px] font-mono text-emerald-700">
                      {CHANNELS.MOBILE}
                    </span>
                  </div>
                  <p className="text-sm font-bold">
                    {mode === "TRANSFER"
                      ? `Transferred ${formatBdt(amount)} to ${destinationAccountNo}`
                      : "Mobile Balance Inquiry Approved"}
                  </p>
                  <p className="text-xs font-mono text-emerald-800 break-all">
                    Ref: {outcome.reference}
                  </p>
                  <div className="pt-1 flex items-center justify-between">
                    <Link
                      to={`/transactions/${outcome.reference}`}
                      className="text-xs font-bold text-emerald-800 underline"
                    >
                      View Receipt &rarr;
                    </Link>
                    <button
                      type="button"
                      onClick={handleReset}
                      className="text-xs font-semibold text-slate-600 hover:underline"
                    >
                      Send Another
                    </button>
                  </div>
                </div>
              )}

              {outcome && outcome.status === "DECLINED" && (
                <div className="rounded-xl border border-amber-300 bg-amber-50 p-4 text-amber-950 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-amber-200 text-amber-900">
                      DECLINED
                    </span>
                    <span className="text-[11px] font-mono text-amber-700">
                      Rule Decision
                    </span>
                  </div>
                  <p className="text-xs font-semibold">
                    Reason:{" "}
                    {outcome.declineReason ||
                      "Declined by pre-authorization rule."}
                  </p>
                  {outcome.reference && (
                    <Link
                      to={`/transactions/${outcome.reference}`}
                      className="inline-block text-xs font-bold text-amber-900 underline"
                    >
                      Inspect Declined Record &rarr;
                    </Link>
                  )}
                </div>
              )}
            </div>

            {/* Phone Home Indicator Bar */}
            <div className="pb-3 pt-1 flex justify-center">
              <div className="h-1.5 w-28 rounded-full bg-slate-300" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
