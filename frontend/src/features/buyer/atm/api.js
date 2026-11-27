import client from "../../../shared/api/client.js";
import { CHANNELS } from "../../../shared/config.js";

/**
 * Submits an ATM transaction (CASH_WITHDRAWAL, BALANCE_INQUIRY, ACCOUNT_VALIDATION).
 * Calls POST /api/channels/atm/transactions (F1 adapter endpoint) and falls back
 * transparently to POST /api/transactions (F2 switch endpoint) if the channel adapter
 * slice has not yet been merged on the current environment.
 */
export async function submitAtmTransaction({
  type,
  sourceAccountNo,
  amount,
  idempotencyKey,
}) {
  const payload = {
    channelCode: CHANNELS.ATM,
    type,
    sourceAccountNo: sourceAccountNo.trim(),
    amount: amount !== undefined && amount !== null ? Number(amount) : 0,
    idempotencyKey,
  };

  try {
    const response = await client.post("/api/channels/atm/transactions", payload);
    return response.data;
  } catch (err) {
    if (err?.status === 404 || err?.status === 405) {
      const fallback = await client.post("/api/transactions", payload);
      return fallback.data;
    }
    throw err;
  }
}

export async function fetchBuyerAccountsForAtm() {
  try {
    const res = await client.get("/api/accounts");
    const raw = res.data;
    return Array.isArray(raw)
      ? raw
      : Array.isArray(raw?.content)
      ? raw.content
      : [];
  } catch {
    return [];
  }
}
