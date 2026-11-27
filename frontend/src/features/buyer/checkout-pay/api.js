import client from "../../../shared/api/client.js";
import { CHANNELS } from "../../../shared/config.js";

/**
 * C5: Fetches checkout session details before payment.
 * GET /api/checkout-sessions/{token} -> { merchantName, amount, currency, status, expiresAt, transactionRef? }
 */
export async function fetchCheckoutSessionByToken(token) {
  const response = await client.get(
    `/api/checkout-sessions/${encodeURIComponent(token)}`
  );
  return response.data;
}

/**
 * Submits a WEB checkout payment for the given token.
 * Calls POST /api/channels/web/checkout/{token}/pay (F4 endpoint) and falls back
 * transparently to POST /api/transactions if the gateway endpoint is not yet merged.
 */
export async function submitCheckoutPayment({
  token,
  sourceAccountNo,
  amount,
  idempotencyKey,
}) {
  const payload = {
    channelCode: CHANNELS.WEB,
    type: "MERCHANT_PAYMENT",
    sourceAccountNo: sourceAccountNo.trim(),
    destinationAccountNo: "SYS-MERCHANT-PAYABLE",
    amount: Number(amount),
    idempotencyKey,
  };

  try {
    const response = await client.post(
      `/api/channels/web/checkout/${encodeURIComponent(token)}/pay`,
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

export async function fetchBuyerAccountsForCheckout() {
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
