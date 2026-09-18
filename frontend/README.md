# Cinevora Frontend

React 18 + Vite + TypeScript SPA for the Cinevora movie streaming application.

## Run locally

Install Node.js 20+ and run:

```powershell
npm install
Copy-Item .env.example .env.local
npm run dev
```

The default API URL is `http://localhost:8080/api/v1`. Override it with
`VITE_API_URL` when the backend is hosted elsewhere. The backend must allow the
frontend origin through `CORS_ALLOWED_ORIGINS`.

## Available checks

```powershell
npm run lint
npm run build
```

Authentication is persisted in Zustand, API data is cached with TanStack Query,
and the Axios client attaches the JWT as a Bearer token for protected requests.
