import client from "../../shared/api/client.js";

export async function fetchBuyerDashboardData() {
  const [accountsResult, txnsResult] = await Promise.allSettled([
    client.get("/api/accounts"),
    client.get("/api/transactions", { params: { page: 0, size: 5 } }),
  ]);

  let accounts = [];
  let accountsError = null;
  if (accountsResult.status === "fulfilled") {
    const raw = accountsResult.value?.data;
    accounts = Array.isArray(raw)
      ? raw
      : Array.isArray(raw?.content)
      ? raw.content
      : [];
  } else {
    accountsError =
      accountsResult.reason?.message || "Accounts service not yet available.";
  }

  let recentTransactions = [];
  let transactionsError = null;
  if (txnsResult.status === "fulfilled") {
    const raw = txnsResult.value?.data;
    recentTransactions = Array.isArray(raw?.content) ? raw.content : [];
  } else {
    transactionsError =
      txnsResult.reason?.message || "Could not load recent transactions.";
  }

  return {
    accounts,
    accountsError,
    recentTransactions,
    transactionsError,
  };
}
