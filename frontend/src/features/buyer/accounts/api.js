import client from "../../../shared/api/client.js";

export async function fetchMyAccounts() {
  const response = await client.get("/api/accounts");
  const data = response.data;
  if (Array.isArray(data)) {
    return data;
  }
  if (data && Array.isArray(data.content)) {
    return data.content;
  }
  return [];
}

export async function fetchAccountByNo(accountNo) {
  const response = await client.get(`/api/accounts/${encodeURIComponent(accountNo)}`);
  return response.data;
}

export async function fetchAccountBalance(accountNo) {
  const response = await client.get(
    `/api/accounts/${encodeURIComponent(accountNo)}/balance`
  );
  return response.data;
}
