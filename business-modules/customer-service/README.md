# Customer Service

This is a dummy README for the Customer Service module.

More details will be added soon.

## Capabilities & APIs

### Access rules

Every endpoint on a customer declares what it does with `@CustomerAccess(READ | WRITE | LIST_BY_USER)`, and
`CustomerAccessInterceptor` enforces it through `CustomerAccessPolicy` before the controller runs. Add the annotation to
any new endpoint with a `customerId` path variable: without it the request is refused with 403.

| Active role | Read | Write | Create (`POST /customer/init`) |
|---|---|---|---|
| CUSTOMER | own record, otherwise 404 | own record, otherwise 404 | own record only, one per bank (409) |
| ADVISOR | any in its bank | any in its bank | yes |
| BANK_ADMIN | any in its bank | 403 | 403 |
| PLATFORM_ADMIN | any in every bank | 403 | 403 |

The full rules are in
[roles and permissions](../../docs/domain/process/security/roles-and-permissions.md#who-can-see-which-customers).

## Miscellaneous

- `platform.tenant.root-read-enabled: true` lets a PLATFORM_ADMIN read across banks here (see `core/platform-core/README.md`).

## Work in Progress

- `customers.user_id` holds the creator; it will become the client, and `branch_code` will limit advisors to their branch.

