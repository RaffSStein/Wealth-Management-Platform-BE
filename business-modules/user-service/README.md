# user-service

This service manages users and provides both administrative and self-signup authentication endpoints.

## API Surfaces
- Administrative (secured):
  - POST /user (create user) — requires role ADMIN
  - PUT /user/{id} (update) — requires role ADMIN
  - POST /user/{id}/enable|disable — requires role ADMIN
  - GET /user/{id}, GET /me — requires authenticated role ADMIN or USER (as configured)
- Self-signup (public):
  - POST /auth/register — registers a new user and emits a JWT
  - POST /auth/login — authenticates and emits a JWT

All REST interfaces and DTOs are defined in `user-api-data/user-api-data.yaml` and generated via OpenAPI. Controllers in `user-core` implement those generated interfaces.

## Security
- Spring Security (stateless), OAuth2 Resource Server (JWT) configured in platform-core.
- Roles are mapped from the `roles` claim; method-level authorization is enforced with `@PreAuthorize`.
- CORS enabled and CSRF disabled for APIs.

## Flows
### 1) Administrative flow (Back-office)
- Intended for authenticated operators.
- Create/Update/Enable/Disable users via secured endpoints.
- Publishes `UserCreatedEvent` upon creation.
- Does not handle passwords from the back-office payload.

### 2) Self-signup flow (Public)
- `POST /auth/register` accepts registration payload, hashes password, persists user, publishes `UserCreatedEvent`, and returns a JWT.
- `POST /auth/login` verifies credentials and returns a JWT. The request carries a `bankCode` to scope the session.
- Permissions are not embedded in the JWT; they are provisioned asynchronously by profiler-service based on `UserCreatedEvent` and retrieved at runtime by other services.

## Integration with profiler-service
- After user creation, an event is emitted; profiler-service consumes it and materializes permissions.
- Services resolve fine-grained permissions via profiler-service using email + bankCode from the token.

## Development
- Edit `user-api-data.yaml` and run the build to regenerate interfaces/DTOs.
- Ensure `security.jwt.publicKeyPath` and (for user-service) `security.jwt.private-key-path` are configured.
