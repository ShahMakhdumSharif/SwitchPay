import client from "../../../shared/api/client.js";
import { CHANNELS } from "../../../shared/config.js";

/**
 * Submits a MOBILE banking transaction (TRANSFER, BALANCE_INQUIRY, ACCOUNT_VALIDATION).
 * Calls POST /api/channels/mobile/transactions (F1 adapter endpoint) and falls back
 * transparently to POST /api/transactions (F2 switch endpoint) if the channel adapter
 * slice has not yet been merged on the current environment.
 */
export async function submitMobileTransaction({
  type,
  sourceAccountNo,
  destinationAccountNo,
  amount,
  idempotencyKey,
}) {
  const payload = {
    channelCode: CHANNELS.MOBILE,
    type,
    sourceAccountNo: sourceAccountNo.trim(),
    destinationAccountNo: destinationAccountNo
      ? destinationAccountNo.trim()
      : undefined,
    amount: amount !== undefined && amount !== null ? Number(amount) : 0,
    idempotencyKey,
  };

  try {
    const response = await client.post(
      "/api/channels/mobile/transactions",
      payload
    );
    return response.data;
  } catch (err) {
    if (err?.status === 404 || err?.status === 405) {
      const fallback = await client.post("/api/transactions", payload);
      return fallback.data;
    }
    throw err;
  }
}

export async function fetchBuyerAccountsForMobile() {
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
