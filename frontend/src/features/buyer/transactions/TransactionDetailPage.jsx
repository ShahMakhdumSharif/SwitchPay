import React, { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { formatBdt } from "../../../shared/config.js";
import { TransactionStatusBadge } from "./BuyerTransactionsPage.jsx";
import { fetchTransactionByRef } from "./api.js";

function buildTimelineSteps(status) {
  const current = (status || "PENDING").toUpperCase();

  const steps = [
    {
      key: "SUBMITTED",
      title: "1. Received by Channel Adapter",
      description: "Normalized into canonical TransactionRequest with idempotency key.",
      state: "complete",
    },
    {
      key: "PENDING",
      title: "2. Switch Persistence & Authorization Chain",
      description: "Persisted as PENDING; evaluated AccountActive, TransactionLimit, and SufficientBalance rules.",
      state: "complete",
    },
  ];

  if (current === "PENDING") {
    steps[1].state = "current";
    steps.push({
      key: "OUTCOME",
      title: "3. Routing & Decision",
      description: "Awaiting handler completion.",
      state: "upcoming",
    });
    return steps;
  }

  if (current === "DECLINED") {
    steps.push({
      key: "DECLINED",
      title: "3. Declined by Authorization Rule",
      description: "Pre-authorization rule declined the request cleanly without debiting funds.",
      state: "declined",
    });
    return steps;
  }

  if (current === "FAILED") {
    steps.push({
      key: "FAILED",
      title: "3. Handler Execution Failed",
      description: "Downstream handler reported a processing failure.",
      state: "failed",
    });
    return steps;
  }

  // APPROVED or post-approval states (SETTLED, REFUNDED, REVERSED)
  steps.push({
    key: "APPROVED",
    title: "3. Approved & Ledger Posted",
    description: "Routed via TransactionRouter, balance updated, and TransactionCompletedEvent published.",
    state: current === "APPROVED" ? "current" : "complete",
  });

  if (current === "SETTLED") {
    steps.push({
      key: "SETTLED",
      title: "4. Merchant Settlement Completed",
      description: "Included in a merchant settlement batch and moved from SYS-MERCHANT-PAYABLE to merchant account.",
      state: "current",
    });
  } else if (current === "REFUNDED") {
    steps.push({
      key: "REFUNDED",
      title: "4. Refunded to Customer",
      description: "Opposing double-entry refund lines posted and customer balance credited.",
      state: "current",
    });
  } else if (current === "REVERSED") {
    steps.push({
      key: "REVERSED",
      title: "4. Reversed by Operations",
      description: "Opposing double-entry reversal lines posted and original balance restored.",
      state: "current",
    });
  }

  return steps;
}

export default function TransactionDetailPage() {
  const { ref } = useParams();
  const [txn, setTxn] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadDetail = useCallback(async () => {
    if (!ref) return;
    setLoading(true);
    setError(null);
    try {
      const data = await fetchTransactionByRef(ref);
      setTxn(data);
    } catch (err) {
      setError(err?.message || `Unable to load transaction ${ref}.`);
    } finally {
      setLoading(false);
    }
  }, [ref]);

  useEffect(() => {
    loadDetail();
  }, [loadDetail]);

  const steps = buildTimelineSteps(txn?.status);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <Link
            to="/transactions"
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800"
          >
            &larr; Back to Transaction History
          </Link>
          <h1 className="mt-1 text-2xl font-bold text-slate-900">
            Transaction Status Tracking
          </h1>
          <p className="text-xs font-mono text-slate-500 mt-0.5">
            Reference: {ref}
          </p>
        </div>
        <button
          type="button"
          onClick={loadDetail}
          disabled={loading}
          className="px-4 py-2 rounded-lg text-sm font-medium border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50"
        >
          {loading ? "Refreshing..." : "Refresh Status"}
        </button>
      </div>

      {loading ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-500 shadow-sm">
          Loading transaction details...
        </div>
      ) : error ? (
        <div
          role="alert"
          className="rounded-xl border border-red-200 bg-red-50 p-6 text-sm text-red-700 space-y-3"
        >
          <p className="font-semibold">Could not load transaction details</p>
          <p>{error}</p>
          <button
            type="button"
            onClick={loadDetail}
            className="px-4 py-2 rounded-lg text-xs font-semibold bg-red-100 text-red-800 hover:bg-red-200"
          >
            Retry
          </button>
        </div>
      ) : !txn ? (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center text-sm text-slate-600 shadow-sm">
          Transaction not found.
        </div>
      ) : (
        <>
          {/* Outcome Banner */}
          {txn.status === "DECLINED" ? (
            <div className="rounded-xl border border-amber-300 bg-amber-50 p-5 text-amber-900">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-amber-700">
                    Transaction Declined (Normal Business Rule Outcome)
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    Reason: {txn.declineReason || "Declined by authorization rule"}
                  </p>
                </div>
                <TransactionStatusBadge status={txn.status} />
              </div>
            </div>
          ) : (
            <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-5 text-emerald-950">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-emerald-700">
                    Switch Outcome
                  </p>
                  <p className="mt-1 text-lg font-bold font-mono">
                    {formatBdt(txn.amount)}
                  </p>
                </div>
                <TransactionStatusBadge status={txn.status} />
              </div>
            </div>
          )}

          {/* Metadata Grid */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">
              Transaction Metadata
            </h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 text-sm">
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Reference
                </dt>
                <dd className="mt-1 font-mono text-xs font-semibold text-slate-900 break-all">
                  {txn.reference}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Status
                </dt>
                <dd className="mt-1">
                  <TransactionStatusBadge status={txn.status} />
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Amount (BDT)
                </dt>
                <dd className="mt-1 font-mono font-bold text-slate-900">
                  {formatBdt(txn.amount)}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Channel
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {txn.channelCode || "—"}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Transaction Type
                </dt>
                <dd className="mt-1 font-semibold text-slate-800">
                  {txn.type || "—"}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Timestamp
                </dt>
                <dd className="mt-1 text-slate-800">
                  {txn.createdAt
                    ? new Date(txn.createdAt).toLocaleString()
                    : "—"}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Source Account
                </dt>
                <dd className="mt-1 font-mono text-slate-900">
                  {txn.sourceAccountNo || "—"}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Destination Account
                </dt>
                <dd className="mt-1 font-mono text-slate-900">
                  {txn.destinationAccountNo || "—"}
                </dd>
              </div>
              <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200">
                <dt className="text-xs font-semibold uppercase text-slate-500">
                  Decline Reason
                </dt>
                <dd className="mt-1 text-slate-800">
                  {txn.declineReason || "None"}
                </dd>
              </div>
            </dl>
          </div>

          {/* Status Lifecycle Timeline */}
          <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-900 mb-4">
              Switch Lifecycle Timeline
            </h2>
            <ol className="relative border-l-2 border-slate-200 ml-3 space-y-6">
              {steps.map((step) => {
                let dotClass = "bg-emerald-600 ring-emerald-100";
                if (step.state === "current") {
                  dotClass = "bg-indigo-600 ring-indigo-100";
                } else if (step.state === "declined") {
                  dotClass = "bg-amber-500 ring-amber-100";
                } else if (step.state === "failed") {
                  dotClass = "bg-red-600 ring-red-100";
                } else if (step.state === "upcoming") {
                  dotClass = "bg-slate-300 ring-slate-100";
                }
                return (
                  <li key={step.key} className="ml-6">
                    <span
                      className={`absolute -left-2.5 flex h-5 w-5 items-center justify-center rounded-full ring-4 ${dotClass}`}
                    />
                    <h3 className="text-sm font-semibold text-slate-900">
                      {step.title}
                    </h3>
                    <p className="mt-0.5 text-xs text-slate-600">
                      {step.description}
                    </p>
                  </li>
                );
              })}
            </ol>
          </div>
        </>
      )}
    </div>
  );
}
