import axios from "axios";
import { normalizeApiError, SESSION_STORAGE_KEY } from "./errors.js";

const client = axios.create({
  baseURL: "",
  headers: {
    "Content-Type": "application/json",
  },
});

client.interceptors.request.use((config) => {
  try {
    const raw = sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed?.token) {
        config.headers.Authorization = `Bearer ${parsed.token}`;
      }
    }
  } catch {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError = normalizeApiError(error);
    const reqUrl = error?.config?.url || "";
    const isAuthLogin = reqUrl.includes("/api/auth/login");

    if (apiError.status === 401 && !isAuthLogin) {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      window.dispatchEvent(new CustomEvent("switchpay:unauthorized"));
      if (!window.location.hash.startsWith("#/login")) {
        window.location.hash = "#/login?expired=1";
      }
    }

    return Promise.reject(apiError);
  }
);

export default client;
