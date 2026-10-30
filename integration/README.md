Here all the services that are integrated with the system

## Local provider stubs (WireMock)

External providers are stubbed locally by the `wiremock` container in `docker-compose.yml`
(host port `8094`, admin API at `http://localhost:8094/__admin`).
Stubs live in `integration/wiremock/mappings/`, one path prefix per provider (`/<provider>/...`).
Response bodies use WireMock response templating to echo request ids.

```bash
docker compose -f docker-compose.yml up -d wiremock
```

### Sanction screening (customer-service AML step)

| Endpoint | Behaviour |
|---|---|
| `POST /sanction/oauth/token` | Client-credentials token, always succeeds |
| `POST /sanction/screen-client` | `CLEAR`, no matches (default) |
| same, `lastName` ends with `-REVIEW` | `REVIEW`, one PEP match with score 40, so the AML step returns *review* |
| same, `lastName` ends with `-HIT` | `HIT`, one sanctions match with score 90, so the AML step *fails* |

The suffixes are case-sensitive, so real surnames (e.g. "Whitman") always get `CLEAR`.
