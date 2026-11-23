# SwitchPay

Multi-Channel Banking and Payment Platform (CSE 3220 - Software Engineering Lab Project).

## Overview
SwitchPay is a centralized banking and payment switch supporting ATM terminals, mobile banking, and merchant web checkout with role-based access control (`ADMIN`, `SELLER`, `BUYER`), double-entry ledger, settlement, refunds, reversals, and operational monitoring.

## Quick Start
```bash
cp .env.example .env
docker compose up --build
```
Health check: `http://localhost:8080/actuator/health`
