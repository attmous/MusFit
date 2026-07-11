# Hermes Coach AI

MusFit can connect the Today tab's Ask coach sheet to a local Hermes Agent
gateway through Hermes' OpenAI-compatible API server.

## Local Gateway

The expected Hermes API surface is:

```text
GET  /v1/health
GET  /v1/models
POST /v1/chat/completions
```

On the current development machine Hermes is exposed on port `8080` and requires
the bearer token from `API_SERVER_KEY` in `~/.hermes/.env`.

For the Android emulator, use:

```text
Base URL: http://10.0.2.2:8080/v1/
Model: hermes-agent
API key: API_SERVER_KEY
```

For a physical phone, replace `10.0.2.2` with the LAN IP of the machine running
Hermes, for example:

```text
http://192.168.1.50:8080/v1/
```

Hermes must be reachable from the device network and the API server key must
match the gateway configuration.

These cleartext examples apply only to `internalDebug`. Use literal IP
addresses: arbitrary hostnames (including single-label, `.local`, and `.lan`
names) are rejected for HTTP so DNS rebinding cannot change the destination
after validation. Production configuration must use a trusted HTTPS endpoint or
HTTPS tunnel.

The verified Radxa development gateway is:

```text
Host: radxa@192.168.178.113
Base URL: http://192.168.178.113:8080/v1/
Model: hermes-agent
```

## Internal Build Defaults

Only `internalDebug` can auto-configure Hermes after a fresh install, app-data
reset, or seeded setup. Keep the secret in ignored `local.properties` or in
environment variables; never commit the API server key.

```properties
MUSFIT_DEBUG_HERMES_BASE_URL=http://192.168.178.113:8080/v1/
MUSFIT_DEBUG_HERMES_MODEL_NAME=hermes-agent
MUSFIT_DEBUG_HERMES_API_KEY=<API_SERVER_KEY from ~/.hermes/.env>
```

When `MUSFIT_DEBUG_HERMES_API_KEY` is present, an empty internal install exposes
Hermes as the default AI coach connection. If the user saves explicit AI coach
settings, those settings take precedence. Clearing the key in-app disables the
persisted connection until the app data is reset or the user configures it again.
The non-debuggable `productionRelease` variant leaves these fields blank.

## MusFit Behavior

The first implementation is read-only:

- The coach can answer questions using a compact snapshot of today's food,
  hydration, health, training, profile, and goals context.
- The coach cannot log, edit, or delete MusFit data.
- Chat history is stored locally in Room per active account and coach provider.
- API keys stay in the existing local encrypted AI coach secret store.
- Endpoint validation occurs before settings can create account/DAO/secret side
  effects and is repeated for stored/default connections and immediately before
  request construction. Invalid chat endpoints therefore dispatch no bearer,
  system prompt, or message body.
- `internalDebug` permits HTTP only for exact `localhost`, literal IPv4
  loopback (`127/8`) or RFC1918 (`10/8`, `172.16/12`, `192.168/16`), and literal
  IPv6 loopback/ULA (`::1`, `fc00::/7`). Link-local, public, reserved,
  multicast, obfuscated, hostname, and zone-id forms are rejected. HTTPS is
  valid for ordinary public/private/loopback IPv4, IPv6, and DNS hosts.
- Private internal endpoints bind to an active Wi-Fi or Ethernet network. They
  fail closed rather than falling back to cellular/default routing. Loopback is
  left unbound. Redirect following is disabled so sensitive headers and chat
  bodies cannot cross to a follow-up endpoint.
- `productionRelease` explicitly disables platform cleartext, omits Local
  Network permission/configuration, and rejects every HTTP endpoint in code.
- Android network-security XML cannot express IP CIDRs. The internal manifest
  therefore enables platform cleartext broadly, but trusts only system CAs; the
  pure, DNS-free request-boundary policy above is the actual host gate.

## Verification

Manual Hermes checks:

```bash
curl http://127.0.0.1:8080/v1/health
curl http://127.0.0.1:8080/v1/models \
  -H "Authorization: Bearer $API_SERVER_KEY"
curl http://127.0.0.1:8080/v1/chat/completions \
  -H "Authorization: Bearer $API_SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"hermes-agent","messages":[{"role":"user","content":"connection test"}],"stream":false}'
```

App verification should run on an x86 Android toolchain or on a real Android
device with native `adb`. The ARM64 board can compile Kotlin, but the Google SDK
tools installed there include x86-64 `aapt2`/`adb`, which blocks full APK build
and emulator verification unless native ARM tools or x86 emulation are added.
