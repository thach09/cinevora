import assert from "node:assert/strict";
const backend = new URL(process.env.BACKEND_ORIGIN);
const frontend = new URL(process.env.FRONTEND_ORIGIN);
assert.equal(backend.protocol, "https:");
assert.equal(frontend.protocol, "https:");
for (const path of [
  "/",
  "/login",
  "/movies/1",
  "/robots.txt",
  "/favicon.svg",
]) {
  const response = await fetch(new URL(path, frontend), {
    signal: AbortSignal.timeout(90_000),
  });
  assert.equal(response.status, 200, path);
}
for (const allowed of [true, false]) {
  const response = await fetch(new URL("/api/v1/users/me", backend), {
    method: "OPTIONS",
    headers: {
      Origin: allowed ? frontend.origin : "https://random-origin.invalid",
      "Access-Control-Request-Method": "GET",
      "Access-Control-Request-Headers":
        "Authorization,Content-Type,X-Profile-Id",
    },
    signal: AbortSignal.timeout(90_000),
  });
  assert.equal(response.status, allowed ? 200 : 403);
  assert.equal(
    response.headers.get("access-control-allow-origin"),
    allowed ? frontend.origin : null,
  );
}
console.log(
  "PASS HTTPS frontend routes/assets and exact production CORS; production browser/media E2E remains a separate release gate",
);
