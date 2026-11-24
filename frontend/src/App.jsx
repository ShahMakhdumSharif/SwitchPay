import React from "react";
import { AuthProvider } from "./shared/auth/AuthContext.jsx";
import AppRoutes from "./routes.jsx";

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}
