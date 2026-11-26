import client from "../../../shared/api/client.js";

export async function fetchUsers() {
  const response = await client.get("/api/users");
  return response.data;
}

export async function deactivateUser(id) {
  await client.delete(`/api/users/${id}`);
}
