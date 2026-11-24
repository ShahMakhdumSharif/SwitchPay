export const CHANNELS = Object.freeze({
  ATM: "ATM",
  MOBILE: "MOBILE",
  WEB: "WEB",
});

export const CHANNEL_CODES = Object.freeze([
  CHANNELS.ATM,
  CHANNELS.MOBILE,
  CHANNELS.WEB,
]);

export const CURRENCY = "BDT";

const bdtFormatter = new Intl.NumberFormat("en-BD", {
  style: "currency",
  currency: CURRENCY,
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatBdt(amount) {
  const num = Number(amount ?? 0);
  if (Number.isNaN(num)) {
    return bdtFormatter.format(0);
  }
  return bdtFormatter.format(num);
}

export function createIdempotencyKey() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `idem-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}
