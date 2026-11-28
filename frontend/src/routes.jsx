import React from "react";
import { Route, Routes } from "react-router-dom";
import ProtectedRoute from "./shared/auth/ProtectedRoute.jsx";
import { ROLES } from "./shared/auth/roles.js";
import AppShell from "./shared/layout/AppShell.jsx";
import Forbidden from "./shared/layout/Forbidden.jsx";
import NotFound from "./shared/layout/NotFound.jsx";
import LoginPage from "./features/auth/LoginPage.jsx";
import RegisterPage from "./features/auth/RegisterPage.jsx";
import DashboardPage from "./features/dashboard/DashboardPage.jsx";
import BuyerAccountsPage from "./features/buyer/accounts/BuyerAccountsPage.jsx";
import AtmPage from "./features/buyer/atm/AtmPage.jsx";
import TransferPage from "./features/buyer/transfer/TransferPage.jsx";
import CheckoutPayPage from "./features/buyer/checkout-pay/CheckoutPayPage.jsx";
import BuyerTransactionsPage from "./features/buyer/transactions/BuyerTransactionsPage.jsx";
import TransactionDetailPage from "./features/buyer/transactions/TransactionDetailPage.jsx";
import AdminUsersPage from "./features/admin/users/AdminUsersPage.jsx";
import AdminAuditPage from "./features/admin/audit/AdminAuditPage.jsx";

function PlaceholderScreen({ title, description }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm">
      <h1 className="text-xl font-bold text-slate-900">{title}</h1>
      <p className="mt-2 text-sm text-slate-600">{description}</p>
    </div>
  );
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="forbidden" element={<Forbidden />} />

        {/* Buyer routes */}
        <Route
          path="accounts"
          element={
            <ProtectedRoute roles={[ROLES.BUYER]}>
              <BuyerAccountsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="atm"
          element={
            <ProtectedRoute roles={[ROLES.BUYER]}>
              <AtmPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="transfer"
          element={
            <ProtectedRoute roles={[ROLES.BUYER]}>
              <TransferPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="pay/:token"
          element={
            <ProtectedRoute roles={[ROLES.BUYER]}>
              <CheckoutPayPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="transactions"
          element={
            <ProtectedRoute roles={[ROLES.BUYER, ROLES.ADMIN]}>
              <BuyerTransactionsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="transactions/:ref"
          element={
            <ProtectedRoute roles={[ROLES.BUYER, ROLES.SELLER, ROLES.ADMIN]}>
              <TransactionDetailPage />
            </ProtectedRoute>
          }
        />

        {/* Seller routes */}
        <Route
          path="merchant"
          element={
            <ProtectedRoute roles={[ROLES.SELLER]}>
              <PlaceholderScreen
                title="Merchant Profile"
                description="Register and manage merchant account details."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="checkout-sessions"
          element={
            <ProtectedRoute roles={[ROLES.SELLER]}>
              <PlaceholderScreen
                title="Checkout Sessions"
                description="Create checkout sessions and copy payment links."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="settlements"
          element={
            <ProtectedRoute roles={[ROLES.SELLER, ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Settlements"
                description="View settlement batches and trigger settlement runs."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="refunds"
          element={
            <ProtectedRoute roles={[ROLES.SELLER, ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Refunds"
                description="Issue partial or full refunds on completed merchant payments."
              />
            </ProtectedRoute>
          }
        />

        {/* Admin routes */}
        <Route
          path="admin/ops"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Operational Monitoring"
                description="Transaction summary counts, failed transactions, and stuck pending queue."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/ledger"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Double-Entry Ledger"
                description="Reconcile immutable debit and credit ledger entries."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/adjustments"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Adjustments & Reversals"
                description="Review transaction adjustments and execute reversals."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/accounts"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Account Administration"
                description="Create, freeze, or close customer banking accounts."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/channels"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <PlaceholderScreen
                title="Channel Registry"
                description="Manage ATM, Mobile, and Web channels."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/audit"
          element={
            <ProtectedRoute roles={[ROLES.ADMIN]}>
              <AdminAuditPage />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
