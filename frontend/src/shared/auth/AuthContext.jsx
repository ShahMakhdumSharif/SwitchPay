import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { SESSION_STORAGE_KEY } from "../api/errors.js";
import { loginApi, logoutApi, registerApi } from "../../features/auth/api.js";

const AuthContext = createContext(null);

function loadStoredSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session?.token) {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
    if (session.expiresAt && new Date(session.expiresAt).getTime() <= Date.now()) {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => loadStoredSession());

  useEffect(() => {
    const handleUnauthorized = () => {
      setSession(null);
    };
    window.addEventListener("switchpay:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("switchpay:unauthorized", handleUnauthorized);
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await loginApi({ email, password });
    const user = data.user ?? {
      id: data.userId,
      email: data.email,
      fullName: data.fullName,
      roles: data.roles ?? [],
    };
    const nextSession = {
      token: data.token,
      expiresAt: data.expiresAt,
      user,
    };
    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
    return nextSession;
  }, []);

  const register = useCallback(async (payload) => {
    return registerApi(payload);
  }, []);

  const logout = useCallback(async () => {
    try {
      if (session?.token) {
        await logoutApi();
      }
    } catch {
      // Clear local session even if token was already expired/revoked
    } finally {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      setSession(null);
      window.location.hash = "#/login";
    }
  }, [session]);

  const roles = useMemo(() => session?.user?.roles ?? [], [session]);

  const hasRole = useCallback(
    (role) => roles.includes(role),
    [roles]
  );

  const hasAnyRole = useCallback(
    (allowedRoles) => {
      if (!allowedRoles || allowedRoles.length === 0) return true;
      return allowedRoles.some((r) => roles.includes(r));
    },
    [roles]
  );

  const value = useMemo(
    () => ({
      token: session?.token ?? null,
      user: session?.user ?? null,
      roles,
      expiresAt: session?.expiresAt ?? null,
      isAuthenticated: Boolean(session?.token),
      hasRole,
      hasAnyRole,
      login,
      register,
      logout,
    }),
    [session, roles, hasRole, hasAnyRole, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside an AuthProvider");
  }
  return ctx;
}
