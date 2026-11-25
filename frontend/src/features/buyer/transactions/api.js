import client from "../../../shared/api/client.js";

export async function fetchTransactions(params = {}) {
  const query = {};
  if (params.page !== undefined) query.page = params.page;
  if (params.size !== undefined) query.size = params.size;
  if (params.status) query.status = params.status;
  if (params.channel) query.channel = params.channel;
  if (params.from) query.from = params.from;
  if (params.to) query.to = params.to;

  const response = await client.get("/api/transactions", { params: query });
  const data = response.data;
  return {
    content: Array.isArray(data?.content) ? data.content : [],
    number: data?.number ?? 0,
    size: data?.size ?? (params.size || 20),
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
  };
}

export async function fetchTransactionByRef(reference) {
  const response = await client.get(
    `/api/transactions/${encodeURIComponent(reference)}`
  );
  return response.data;
}
