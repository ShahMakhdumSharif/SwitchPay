import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  CHANNELS,
  createIdempotencyKey,
  formatBdt,
} from "../../../shared/config.js";
import { fetchBuyerAccountsForAtm, submitAtmTransaction } from "./api.js";

const ATM_OPERATIONS = [
  {
    value: "CASH_WITHDRAWAL",
    label: "Cash Withdrawal",
    description: "Withdraw BDT cash from your active account.",
    requiresAmount: true,
  },
  {
    value: "BALANCE_INQUIRY",
    label: "Balance Inquiry",
    description: "Verify your live available balance through the ATM switch.",
    requiresAmount: false,
  },
  {
    value: "ACCOUNT_VALIDATION",
    label: "Account Validation",
    description: "Validate account status (ACTIVE/FROZEN/CLOSED) on the switch.",
    requiresAmount: false,
  },
];

const QUICK_AMOUNTS = [500, 1000, 2000, 5000, 10000, 20000];

export default function AtmPage() {
  const [accounts, setAccounts] = useState([]);
  const [operation, setOperation] = useState("CASH_WITHDRAWAL");
  const [sourceAccountNo, setSourceAccountNo] = useState("ACC-1001");
  const [amount, setAmount] = useState("1500.00");
  const [idempotencyKey, setIdempotencyKey] = useState(() =>
    createIdempotencyKey()
  );

  const [submitting, setSubmitting] = useState(false);
  const [outcome, setOutcome] = useState(null);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    let mounted = true;
    fetchBuyerAccountsForAtm().then((list) => {
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

  const currentOp =
    ATM_OPERATIONS.find((op) => op.value === operation) || ATM_OPERATIONS[0];

  const rotateIdempotencyKeyForNewChange = () => {
    // When user edits inputs after a completed/failed attempt, mint a new key
    if (outcome) {
      setIdempotencyKey(createIdempotencyKey());
      setOutcome(null);
    }
    setError(null);
    setFieldErrors({});
  };

  const validateClientForm = () => {
    const errs = {};
    if (!sourceAccountNo || !sourceAccountNo.trim()) {
      errs.sourceAccountNo = "Source account number is required.";
    }
    if (currentOp.requiresAmount) {
      const parsed = Number(amount);
      if (!amount || Number.isNaN(parsed) || parsed <= 0) {
        errs.amount = "Amount must be greater than 0 BDT.";
      } else if (!/^\d+(\.\d{1,2})?$/.test(String(amount).trim())) {
        errs.amount = "Amount can have at most two decimal places.";
      }
    }
    setFieldErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    if (submitting) return;
    if (!validateClientForm()) return;

    setSubmitting(true);
    setError(null);

    try {
      const result = await submitAtmTransaction({
        type: operation,
        sourceAccountNo,
        amount: currentOp.requiresAmount ? amount : "0.01",
        idempotencyKey,
      });
      setOutcome(result);
    } catch (err) {
      // C4: keep the same idempotencyKey so clicking Retry reuses it safely!
      if (err?.fieldErrors && typeof err.fieldErrors === "object") {
        setFieldErrors(err.fieldErrors);
      }
      setError({
        status: err?.status,
        message:
          err?.message || "ATM terminal could not reach the transaction switch.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleStartFreshTransaction = () => {
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
            ATM Terminal Simulator
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Channel <span className="font-mono font-semibold">{CHANNELS.ATM}</span>{" "}
            • Simulated physical ATM kiosk routing cash withdrawals, balance inquiries, and validations to SwitchFacade.
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

      {/* Physical ATM Device Frame */}
      <div className="max-w-3xl mx-auto">
        <div className="rounded-3xl bg-slate-900 p-4 sm:p-7 shadow-2xl border-4 border-slate-700">
          {/* ATM Bezel Header */}
          <div className="flex flex-wrap items-center justify-between gap-3 pb-4 mb-5 border-b border-slate-700">
            <div className="flex items-center gap-3">
              <div className="h-3 w-3 rounded-full bg-emerald-400 animate-pulse" />
              <span className="text-xs sm:text-sm font-mono font-bold tracking-wider text-slate-200 uppercase">
                SwitchPay ATM Terminal • KIOSK-DHK-01
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-1 rounded bg-slate-800 border border-slate-600 text-xs font-mono text-emerald-400">
                CHANNEL: {CHANNELS.ATM}
              </span>
              <span className="px-2.5 py-1 rounded bg-slate-800 border border-slate-600 text-xs font-mono text-slate-300">
                BDT DISPENSER READY
              </span>
            </div>
          </div>

          {/* Embedded CRT / LCD Screen inside ATM Frame */}
          <div className="rounded-2xl bg-slate-50 border-2 border-slate-400 p-5 sm:p-6 space-y-5 shadow-inner">
            {/* Operation Mode Selector */}
            <div>
              <span className="block text-xs font-bold uppercase tracking-wider text-slate-600 mb-2">
                Select ATM Service
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                {ATM_OPERATIONS.map((op) => {
                  const active = operation === op.value;
                  return (
                    <button
                      key={op.value}
                      type="button"
                      disabled={submitting}
                      onClick={() => {
                        setOperation(op.value);
                        rotateIdempotencyKeyForNewChange();
                      }}
                      className={`text-left p-3 rounded-xl border transition-all ${
                        active
                          ? "bg-indigo-600 text-white border-indigo-700 shadow-sm"
                          : "bg-white text-slate-800 border-slate-300 hover:border-indigo-400"
                      }`}
                    >
                      <div className="text-xs font-bold">{op.label}</div>
                      <div
                        className={`mt-1 text-[11px] leading-snug ${
                          active ? "text-indigo-100" : "text-slate-500"
                        }`}
                      >
                        {op.description}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              {/* Source Account Selector / Input */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label
                    htmlFor="atm-source-account"
                    className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1"
                  >
                    Card / Account Number
                  </label>
                  <input
                    id="atm-source-account"
                    type="text"
                    value={sourceAccountNo}
                    disabled={submitting}
                    onChange={(e) => {
                      setSourceAccountNo(e.target.value);
                      rotateIdempotencyKeyForNewChange();
                    }}
                    placeholder="e.g. ACC-1001"
                    className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-mono text-slate-900 focus:border-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                  />
                  {fieldErrors.sourceAccountNo && (
                    <p className="mt-1 text-xs text-red-600">
                      {fieldErrors.sourceAccountNo}
                    </p>
                  )}
                </div>

                {accounts.length > 0 && (
                  <div>
                    <label
                      htmlFor="atm-quick-account-select"
                      className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1"
                    >
                      Quick Select Your Account
                    </label>
                    <select
                      id="atm-quick-account-select"
                      value={sourceAccountNo}
                      disabled={submitting}
                      onChange={(e) => {
                        setSourceAccountNo(e.target.value);
                        rotateIdempotencyKeyForNewChange();
                      }}
                      className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 focus:border-indigo-600 focus:outline-none"
                    >
                      {accounts.map((acc) => {
                        const no = acc.accountNo || acc.account_no;
                        return (
                          <option key={acc.id || no} value={no}>
                            {no} ({acc.type || "SAVINGS"} • {formatBdt(acc.balance)})
                          </option>
                        );
                      })}
                    </select>
                  </div>
                )}
              </div>

              {/* Amount Input & Fast Cash Buttons */}
              {currentOp.requiresAmount && (
                <div className="space-y-2.5">
                  <div>
                    <label
                      htmlFor="atm-amount"
                      className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1"
                    >
                      Withdrawal Amount (BDT)
                    </label>
                    <input
                      id="atm-amount"
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={amount}
                      disabled={submitting}
                      onChange={(e) => {
                        setAmount(e.target.value);
                        rotateIdempotencyKeyForNewChange();
                      }}
                      placeholder="1500.00"
                      className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-base font-mono font-bold text-slate-900 focus:border-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-500/20"
                    />
                    {fieldErrors.amount && (
                      <p className="mt-1 text-xs text-red-600">
                        {fieldErrors.amount}
                      </p>
                    )}
                  </div>

                  <div>
                    <span className="block text-[11px] font-semibold uppercase tracking-wider text-slate-500 mb-1.5">
                      Fast Cash Notes (BDT)
                    </span>
                    <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                      {QUICK_AMOUNTS.map((val) => (
                        <button
                          key={val}
                          type="button"
                          disabled={submitting}
                          onClick={() => {
                            setAmount(val.toFixed(2));
                            rotateIdempotencyKeyForNewChange();
                          }}
                          className="px-2.5 py-1.5 rounded-lg text-xs font-mono font-semibold border border-slate-300 bg-white text-slate-800 hover:border-indigo-500 hover:bg-indigo-50"
                        >
                          ৳{val.toLocaleString()}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Idempotency Key Indicator (C4) */}
              <div className="rounded-lg bg-slate-100 border border-slate-200 px-3.5 py-2 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-600">
                <span>
                  Idempotency Key:{" "}
                  <code className="font-mono font-semibold text-slate-800">
                    {idempotencyKey}
                  </code>
                </span>
                <button
                  type="button"
                  disabled={submitting}
                  onClick={handleStartFreshTransaction}
                  className="text-indigo-600 font-semibold hover:underline"
                >
                  New Key
                </button>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center justify-end gap-3 pt-1">
                {outcome && (
                  <button
                    type="button"
                    onClick={handleStartFreshTransaction}
                    className="px-4 py-2.5 rounded-xl text-xs font-semibold border border-slate-300 bg-white text-slate-700 hover:bg-slate-100"
                  >
                    New ATM Transaction
                  </button>
                )}
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-6 py-2.5 rounded-xl text-sm font-bold bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 shadow-sm"
                >
                  {submitting
                    ? "Contacting Switch..."
                    : `Execute ${currentOp.label}`}
                </button>
              </div>
            </form>

            {/* HTTP / Network Error Banner (with Retry preserving Idempotency Key) */}
            {error && (
              <div
                role="alert"
                className="rounded-xl border border-red-300 bg-red-50 p-4 text-sm text-red-800 space-y-2"
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-bold">
                    {error.status === 409
                      ? "Duplicate Transaction Request (HTTP 409)"
                      : error.status === 422
                      ? "Business Rule Validation (HTTP 422)"
                      : "Switch Communication Error"}
                  </span>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handleSubmit}
                    className="px-3 py-1 rounded-lg text-xs font-bold bg-red-100 text-red-900 hover:bg-red-200"
                  >
                    Retry with Same Key
                  </button>
                </div>
                <p className="text-xs">{error.message}</p>
              </div>
            )}

            {/* C4 Outcome Panel: APPROVED (Green) vs DECLINED (Amber - Normal Outcome!) */}
            {outcome && outcome.status === "APPROVED" && (
              <div className="rounded-xl border-2 border-emerald-400 bg-emerald-50 p-5 text-emerald-950 space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="inline-flex items-center px-3 py-0.5 rounded-full text-xs font-bold bg-emerald-200 text-emerald-900">
                    APPROVED
                  </span>
                  <span className="text-xs font-mono text-emerald-800">
                    Channel: {CHANNELS.ATM}
                  </span>
                </div>
                <h3 className="text-base font-bold">
                  ATM {currentOp.label} Approved
                </h3>
                {currentOp.requiresAmount && (
                  <p className="text-lg font-mono font-bold text-emerald-900">
                    Dispensed: {formatBdt(amount)}
                  </p>
                )}
                <p className="text-xs font-mono text-emerald-800 break-all">
                  Switch Reference: <strong>{outcome.reference}</strong>
                </p>
                <div className="pt-2 flex flex-wrap items-center gap-3">
                  <Link
                    to={`/transactions/${outcome.reference}`}
                    className="inline-flex items-center px-3.5 py-1.5 rounded-lg text-xs font-bold bg-emerald-700 text-white hover:bg-emerald-800"
                  >
                    Inspect Transaction Receipt &rarr;
                  </Link>
                </div>
              </div>
            )}

            {outcome && outcome.status === "DECLINED" && (
              <div className="rounded-xl border-2 border-amber-400 bg-amber-50 p-5 text-amber-950 space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="inline-flex items-center px-3 py-0.5 rounded-full text-xs font-bold bg-amber-200 text-amber-900">
                    DECLINED
                  </span>
                  <span className="text-xs font-mono text-amber-800">
                    Normal Business Rule Outcome
                  </span>
                </div>
                <h3 className="text-base font-bold">
                  Transaction Declined by Switch Authorization Chain
                </h3>
                <p className="text-sm font-semibold text-amber-900">
                  Decline Reason:{" "}
                  {outcome.declineReason ||
                    "Insufficient balance, inactive account, or limit exceeded."}
                </p>
                {outcome.reference && (
                  <div className="pt-2 flex flex-wrap items-center justify-between gap-2 text-xs font-mono text-amber-800">
                    <span>Reference: {outcome.reference}</span>
                    <Link
                      to={`/transactions/${outcome.reference}`}
                      className="font-bold underline hover:text-amber-950"
                    >
                      View Declined Record &rarr;
                    </Link>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* ATM Cash Dispenser Slot Footer */}
          <div className="mt-5 pt-3 border-t border-slate-700 flex items-center justify-between text-xs font-mono text-slate-400">
            <span>CASH DISPENSER SLOT</span>
            <div className="h-2.5 w-36 sm:w-56 rounded-full bg-slate-800 border border-slate-600" />
            <span>RECEIPT PRINTER</span>
          </div>
        </div>
      </div>
    </div>
  );
}
