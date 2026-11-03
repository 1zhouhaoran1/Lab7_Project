# Lab 7 - Bug & Troubleshooting Record

Three real problems were hit while building and running the multi-container
calculator. Each entry follows: **Symptom → Diagnosis → Fix**.

---

## Bug 1: `docker compose build` cannot pull `nginx:1.27-alpine`

**Symptom**

```
target web: failed to resolve source metadata for docker.io/library/nginx:1.27-alpine:
failed to do request ... dialing registry-1.docker.io:443
container connecting via app settings HTTPS proxy socks5://127.0.0.1:10808:
connectex: No connection could be made because the target machine actively refused it
```

**Diagnosis**

1. The error message shows Docker Desktop routes registry traffic through the
   configured SOCKS5 proxy `127.0.0.1:10808` — but nothing was listening on that
   port: v2rayN was not running.
2. After starting v2rayN the error changed to TLS timeouts. Testing with `curl`
   through the proxy showed `www.baidu.com` = 200 but `google.com`,
   `auth.docker.io`, `registry-1.docker.io` = TLS failures → the proxy **node**
   itself was unstable, not the configuration.
3. DNS check: `Resolve-DnsName auth.docker.io` returned `31.13.86.21` (a Facebook
   IP) → DNS poisoning for docker.io domains, so a direct (proxy-less) pull can
   never work either.

**Fix**

* Start v2rayN and switch to a working node (verified with
  `curl --proxy socks5h://127.0.0.1:10808 https://www.google.com/generate_204` → 204).
* As a proxy-independent fallback the nginx base image was pre-pulled from a
  Chinese mirror and tagged with the official name:

```bash
docker pull docker.1ms.run/library/nginx:1.27-alpine
docker tag docker.1ms.run/library/nginx:1.27-alpine nginx:1.27-alpine
```

  `docker compose build` then finds `nginx:1.27-alpine` locally and never touches
  Docker Hub. CI on GitHub-hosted runners pulls the official image directly and
  is not affected.

---

## Bug 2: Maven downloads inside the build fail with TLS resets

**Symptom**

```
[ERROR] Could not transfer artifact ... from/to central (https://repo.maven.apache.org/maven2):
Remote host terminated the handshake
ERROR: process "RUN mvn -B dependency:go-offline" did not complete successfully: exit code: 1
```

**Diagnosis**

1. Direct access to `repo.maven.apache.org` is blocked in this network, so a
   Maven mirror was added (`backend/settings.xml`, Aliyun mirror of `central`).
   The mirror helped but did **not** fully fix it — many artifacts still failed
   with `Remote host terminated the handshake` / `SSL peer shut down incorrectly`.
2. Isolation test, same URL, 5 attempts each:

   | Source | Result |
   |---|---|
   | Host, direct | 5/5 OK (0.05 s) |
   | Host, via the SOCKS5 proxy | 2/5 OK |
   | Inside a container (`wget`) | 2/5 OK |

3. The container has **no** `HTTP_PROXY` env (`docker run ... env | grep -i proxy`
   is empty), yet it shows exactly the same failure rate as a connection that
   goes through the proxy → Docker Desktop applies its "app settings proxy"
   transparently to container traffic, and the unstable node resets TLS
   handshakes. Container DNS (`223.111.170.29` for `maven.aliyun.com`) and the
   route table (WLAN metric 55 < Radmin VPN 9256) were checked and are fine.

**Fix**

1. `backend/settings.xml` → all `central` requests go to
   `https://maven.aliyun.com/repository/central`, copied into `/root/.m2/settings.xml`
   before any `mvn` call.
2. Retry loops in `backend/Dockerfile`:

```dockerfile
RUN i=1; until mvn -B -U dependency:go-offline; do \
        i=$((i+1)); \
        if [ "$i" -gt 40 ]; then echo "dependency download failed 40 times"; exit 1; fi; \
        echo "=== dependency:go-offline failed, retry $i/40 ==="; \
        sleep 5; \
    done
```

   Every retry keeps the artifacts already downloaded in `/root/.m2`, so each
   attempt makes progress — the log shows the number of missing artifacts
   shrinking every round (30+ → 1 → success). Same loop wraps `mvn package`.

---

## Bug 3: `java -cp ... calculator.ApiServer` → "could not find main method"

**Symptom**: starting the API locally failed with
`错误: 在类 calculator.ApiServer 中找不到 main 方法`.

**Diagnosis**: `ApiServer` only exposes `start(int port)`; the `main` method lives
in `Main`, which in Lab 7 gained a `server` mode. The command used the wrong class.

**Fix**: start it with `java -cp ... calculator.Main server`; the backend
Dockerfile does the same via `CMD ["server"]` appended to the `ENTRYPOINT`.

---

## Design note: cross-service addressing (not localhost, not a bug)

The frontend JavaScript never writes `http://localhost:8000` or
`http://backend:8000`. `backend` is a DNS name that only exists **inside** the
compose network — a browser on the host cannot resolve it. The page therefore
calls the relative path `/api/calc`, and `web/nginx.conf` forwards it with
`proxy_pass http://backend:8000/`. The service name `backend` is used exactly
where it belongs (inside the compose network, in the nginx config), the browser
stays on one origin, and no CORS or DNS problem can occur.
