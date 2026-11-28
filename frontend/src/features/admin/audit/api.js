import client from "../../../shared/api/client.js";

export async function fetchAuditLogs(params = {}) {
  const response = await client.get("/api/ops/audit-logs", { params });
  return response.data;
}
