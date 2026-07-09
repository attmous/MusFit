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

The verified Radxa development gateway is:

```text
Host: radxa@192.168.178.113
Base URL: http://192.168.178.113:8080/v1/
Model: hermes-agent
```

## MusFit Behavior

The first implementation is read-only:

- The coach can answer questions using a compact snapshot of today's food,
  hydration, health, training, profile, and goals context.
- The coach cannot log, edit, or delete MusFit data.
- Chat history is stored locally in Room per active account and coach provider.
- API keys stay in the existing local encrypted AI coach secret store.
- Debug builds allow cleartext HTTP for local development endpoints, including
  emulator loopback and LAN hosts such as the Radxa Hermes gateway. Release
  builds keep the default platform network policy.

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
