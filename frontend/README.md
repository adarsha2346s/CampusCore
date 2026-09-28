# CampusCore frontend

This directory contains the React 19, TypeScript, and Vite client for CampusCore.

For the complete project overview, role capabilities, API contract, security model, local setup, and backend limitations, see the [project README](../README.md).

## Frontend commands

```sh
npm ci
cp .env.example .env
npm run dev
npm run lint
npm run build
```

The development API base URL is configured with `VITE_API_BASE_URL`. The example value points to `http://localhost:8080/api/v1`. Do not put credentials or secrets in `VITE_` variables.
