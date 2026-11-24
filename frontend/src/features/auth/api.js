import client from "../../shared/api/client.js";

export async function loginApi(credentials) {
  const response = await client.post("/api/auth/login", credentials);
  return response.data;
}

export async function registerApi(payload) {
  const response = await client.post("/api/auth/register", payload);
  return response.data;
}

export async function logoutApi() {
  await client.post("/api/auth/logout");
}
