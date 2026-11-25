import React, { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  CHANNELS,
  CURRENCY,
  createIdempotencyKey,
  formatBdt,
} from "../../../shared/config.js";
import {
  fetchBuyerAccountsForCheckout,
  fetchCheckoutSessionByToken,
  submitCheckoutPayment,
} from "./api.js";

export default function CheckoutPayPage() {
  const { token } = useParams();

  const [session, setSession] = useState(null);
  const [sessionLoading, setSessionLoading] = useState(true);
  const [sessionError, setSessionError] = useState(null);

  const [accounts, setAccounts] = useState([]);
  const [sourceAccountNo, setSourceAccountNo] = useState("ACC-1001");
  const [fallbackAmount, setFallbackAmount] = useState("1250.00");
  const [idempotencyKey, setIdempotencyKey] = useState(() =>
    createIdempotencyKey()
  );

  const [submitting, setSubmitting] = useState(false);
  const [outcome, setOutcome] = useState(null);
  const [payError, setPayError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const loadSessionAndAccounts = useCallback(async () => {
    setSessionLoading(true);
    setSessionError(null);
    const [sessResult, accs] = await Promise.allSettled([
      fetchCheckoutSessionByToken(token),
      fetchBuyerAccountsForCheckout(),
    ]);

    if (accs.status === "fulfilled" && Array.isArray(accs.value)) {
      setAccounts(accs.value);
      if (accs.value.length > 0) {
        const firstNo = accs.value[0].accountNo || accs.value[0].account_no;
        if (firstNo) setSourceAccountNo(firstNo);
      }
    }

    if (sessResult.status === "fulfilled") {
      setSession(sessResult.value);
    } else {
      setSession(null);
      setSessionError(
        sessResult.reason?.message ||
          `Checkout session "${token}" could not be loaded from /api/checkout-sessions/${token}.`
      );
    }
    setSessionLoading(false);
  }, [token]);

  useEffect(() => {
    loadSessionAndAccounts();
  }, [loadSessionAndAccounts]);

  const effectiveAmount = session?.amount ?? fallbackAmount;
  const sessionStatus = (session?.status || "CREATED").toUpperCase();
  const isAlreadyPaid =
    sessionStatus === "PAID" || outcome?.status === "APPROVED";
  const isExpiredOrCancelled =
    sessionStatus === "EXPIRED" || sessionStatus === "CANCELLED";

  const handleAccountChange = (value) => {
    setSourceAccountNo(value);
    if (outcome) {
      setIdempotencyKey(createIdempotencyKey());
      setOutcome(null);
    }
    setPayError(null);
    setFieldErrors({});
  };

  const handlePay = async (e) => {
    if (e) e.preventDefault();
    if (submitting || isAlreadyPaid || isExpiredOrCancelled) return;

    if (!sourceAccountNo || !sourceAccountNo.trim()) {
      setFieldErrors({
        sourceAccountNo: "Please choose or enter a source account number.",
      });
      return;
    }

    setSubmitting(true);
    setPayError(null);
    setFieldErrors({});

    try {
      const res = await submitCheckoutPayment({
        token,
        sourceAccountNo,
        amount: effectiveAmount,
        idempotencyKey,
      });
      setOutcome(res);
      if (res?.status === "APPROVED" && session) {
        setSession((prev) =>
          prev
            ? {
                ...prev,
                status: "PAID",
                transactionRef: res.reference,
              }
            : prev
        );
      }
    } catch (err) {
      if (err?.fieldErrors && typeof err.fieldErrors === "object") {
        setFieldErrors(err.fieldErrors);
      }
      setPayError({
        status: err?.status,
        message:
          err?.message ||
          "Checkout payment could not be processed. You can safely retry with the same idempotency key.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Web Checkout Header */}
        <div className="bg-slate-900 px-6 py-5 text-white flex flex-wrap items-center justify-between gap-3">
          <div>
            <span className="text-xs font-mono uppercase tracking-wider text-indigo-300">
              SwitchPay Web Checkout • Channel {CHANNELS.WEB}
            </span>
            <h1 className="mt-1 text-xl font-bold">
              {session?.merchantName || "Merchant Web Checkout"}
            </h1>
          </div>
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-slate-800 border border-slate-700 text-slate-200">
            Session: {token}
          </span>
        </div>

        <div className="p-6 space-y-6">
          {sessionLoading ? (
            <div className="py-8 text-center text-sm text-slate-500">
              Loading checkout session details...
            </div>
          ) : (
            <>
              {/* C5 Session Details Summary */}
              {session ? (
                <div className="rounded-xl bg-slate-50 border border-slate-200 p-5 space-y-3">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                      Payable to Merchant
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${
                        sessionStatus === "PAID"
                          ? "bg-emerald-100 text-emerald-800"
                          : sessionStatus === "CREATED"
                          ? "bg-blue-100 text-blue-800"
                          : "bg-amber-100 text-amber-800"
                      }`}
                    >
                      {sessionStatus}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-baseline justify-between gap-2">
                    <span className="text-base font-bold text-slate-900">
                      {session.merchantName || "Verified SwitchPay Merchant"}
                    </span>
                    <span className="text-2xl font-mono font-bold text-slate-900">
                      {formatBdt(session.amount)}
                    </span>
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500 pt-2 border-t border-slate-200">
                    <span>Currency: {session.currency || CURRENCY}</span>
                    {session.expiresAt && (
                      <span>
                        Expires:{" "}
                        {new Date(session.expiresAt).toLocaleString()}
                      </span>
                    )}
                  </div>
                  {session.transactionRef && (
                    <div className="pt-2 text-xs font-mono text-emerald-800">
                      Transaction Reference:{" "}
                      <Link
                        to={`/transactions/${session.transactionRef}`}
                        className="font-bold underline"
                      >
                        {session.transactionRef}
                      </Link>
                    </div>
                  )}
                </div>
              ) : (
                <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-900 space-y-2">
                  <p className="font-semibold">{sessionError}</p>
                  <p>
                    You can still test the Web Checkout payment switch flow for token{" "}
                    <code className="font-mono font-bold">{token}</code> below:
                  </p>
                  <div>
                    <label
                      htmlFor="checkout-fallback-amount"
                      className="block text-[11px] font-bold uppercase text-amber-800 mb-1"
                    >
                      Checkout Amount (BDT)
                    </label>
                    <input
                      id="checkout-fallback-amount"
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={fallbackAmount}
                      disabled={submitting}
                      onChange={(e) => setFallbackAmount(e.target.value)}
                      className="w-full rounded-lg border border-amber-300 bg-white px-3 py-1.5 text-sm font-mono font-bold text-slate-900"
                    />
                  </div>
                </div>
              )}

              {/* Payment Form */}
              <form onSubmit={handlePay} className="space-y-4" noValidate>
                <div>
                  <label
                    htmlFor="checkout-source-account"
                    className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1"
                  >
                    Pay From Customer Account
                  </label>
                  {accounts.length > 0 && (
                    <select
                      aria-label="Choose customer bank account"
                      value={sourceAccountNo}
                      disabled={submitting || isAlreadyPaid}
                      onChange={(e) => handleAccountChange(e.target.value)}
                      className="mb-2 w-full rounded-lg border border-slate-300 bg-slate-50 px-3 py-2 text-xs text-slate-900"
                    >
                      {accounts.map((acc) => {
                        const no = acc.accountNo || acc.account_no;
                        return (
                          <option key={acc.id || no} value={no}>
                            {no} ({acc.type || "SAVINGS"} •{" "}
                            {formatBdt(acc.balance)})
                          </option>
                        );
                      })}
                    </select>
                  )}
                  <input
                    id="checkout-source-account"
                    type="text"
                    value={sourceAccountNo}
                    disabled={submitting || isAlreadyPaid}
                    onChange={(e) => handleAccountChange(e.target.value)}
                    placeholder="e.g. ACC-1001"
                    className="w-full rounded-xl border border-slate-300 px-3.5 py-2.5 text-sm font-mono text-slate-900 focus:border-indigo-600 focus:outline-none"
                  />
                  {fieldErrors.sourceAccountNo && (
                    <p className="mt-1 text-xs text-red-600">
                      {fieldErrors.sourceAccountNo}
                    </p>
                  )}
                </div>

                <div className="rounded-lg bg-slate-100 border border-slate-200 px-3.5 py-2 text-xs text-slate-600 flex flex-wrap items-center justify-between gap-2">
                  <span className="font-mono truncate">
                    Idempotency Key: {idempotencyKey}
                  </span>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={() => {
                      setIdempotencyKey(createIdempotencyKey());
                      setOutcome(null);
                      setPayError(null);
                    }}
                    className="text-indigo-600 font-semibold hover:underline"
                  >
                    New Key
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={
                    submitting || isAlreadyPaid || isExpiredOrCancelled
                  }
                  className="w-full py-3 rounded-xl text-sm font-bold bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 shadow-sm"
                >
                  {submitting
                    ? "Authorizing Payment..."
                    : isAlreadyPaid
                    ? "Checkout Session Paid"
                    : isExpiredOrCancelled
                    ? `Session ${sessionStatus}`
                    : `Authorize & Pay ${formatBdt(effectiveAmount)}`}
                </button>
              </form>

              {payError && (
                <div
                  role="alert"
                  className="rounded-xl border border-red-200 bg-red-50 p-4 text-xs text-red-800 space-y-2"
                >
                  <p className="font-bold">
                    {payError.status === 422
                      ? "Checkout Session Rule Error (HTTP 422)"
                      : payError.status === 409
                      ? "Duplicate Payment Attempt (HTTP 409)"
                      : "Payment Error"}
                  </p>
                  <p>{payError.message}</p>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handlePay}
                    className="px-3 py-1.5 rounded-lg font-bold bg-red-100 text-red-900 hover:bg-red-200"
                  >
                    Retry Payment (Same Idempotency Key)
                  </button>
                </div>
              )}

              {outcome && outcome.status === "APPROVED" && (
                <div className="rounded-xl border-2 border-emerald-400 bg-emerald-50 p-5 text-emerald-950 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="px-3 py-0.5 rounded-full text-xs font-bold bg-emerald-200 text-emerald-900">
                      APPROVED • PAID
                    </span>
                    <span className="text-xs font-mono text-emerald-800">
                      Channel: {CHANNELS.WEB}
                    </span>
                  </div>
                  <h2 className="text-base font-bold">
                    Merchant Payment Approved ({formatBdt(effectiveAmount)})
                  </h2>
                  <p className="text-xs font-mono text-emerald-800 break-all">
                    Transaction Reference: <strong>{outcome.reference}</strong>
                  </p>
                  <div className="pt-2">
                    <Link
                      to={`/transactions/${outcome.reference}`}
                      className="inline-flex items-center px-4 py-2 rounded-lg text-xs font-bold bg-emerald-700 text-white hover:bg-emerald-800"
                    >
                      Track Transaction Status &rarr;
                    </Link>
                  </div>
                </div>
              )}

              {outcome && outcome.status === "DECLINED" && (
                <div className="rounded-xl border-2 border-amber-400 bg-amber-50 p-5 text-amber-950 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="px-3 py-0.5 rounded-full text-xs font-bold bg-amber-200 text-amber-900">
                      DECLINED
                    </span>
                    <span className="text-xs font-mono text-amber-800">
                      Normal Business Rule Outcome
                    </span>
                  </div>
                  <h2 className="text-base font-bold">
                    Payment Declined by Switch Authorization Chain
                  </h2>
                  <p className="text-sm font-semibold text-amber-900">
                    Reason:{" "}
                    {outcome.declineReason ||
                      "Insufficient funds or account limit exceeded."}
                  </p>
                  {outcome.reference && (
                    <Link
                      to={`/transactions/${outcome.reference}`}
                      className="inline-block text-xs font-bold text-amber-900 underline"
                    >
                      Inspect Declined Transaction &rarr;
                    </Link>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
