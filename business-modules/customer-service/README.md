# Customer Service

This is a dummy README for the Customer Service module.

More details will be added soon.

## Capabilities & APIs

### Access rules

Every endpoint on a customer declares what it does with
`@CustomerAccess(READ | WRITE | LIST_BY_USER | LIST | ADVISOR_DASHBOARD)`, and `CustomerAccessInterceptor` enforces it
through `CustomerAccessPolicy` before the controller runs. Add the annotation to any new endpoint with a `customerId` path
variable: without it the request is refused with 403.

| Active role | Read | Write | Create (`POST /customer/init`) | List (`GET /customer`) | `GET /customer/advisor-dashboard-statistics` |
|---|---|---|---|---|---|
| CUSTOMER | own record, otherwise 404 | own record, otherwise 404 | own record only, one per bank (409) | 403 | 403 |
| ADVISOR | the branch in its token, otherwise 404 | the branch in its token, otherwise 404 | sends the client's email; `userId` is ignored | the branch in its token | the branch in its token |
| BANK_ADMIN | any in its bank | 403 | 403 | its bank | 403 |
| PLATFORM_ADMIN | any in every bank | 403 | 403 | every bank | 403 |

`GET /customer` takes Spring's `Pageable` (`page`, `size`, repeatable `sort`), like bank-service: the default size is 10, a
larger size than 100 is capped, and an unknown sort property answers 400.

### Linking a record to a client's login

`customers.user_id` is the client's login, never the staff member who created the record. An advisor's record starts with
no user, and nothing in a request can set it: it follows the role assignment events. See
[customer onboarding](../../docs/domain/process/onboarding/customer-onboarding/customer-onboarding-process.md).

The full rules are in
[roles and permissions](../../docs/domain/process/security/roles-and-permissions.md#who-can-see-which-customers).

## Topics & Events

| Topic | Direction | Meaning |
|---|---|---|
| `...user-service.create-user` | produced | An advisor created a client, after the commit |
| `...profiler-service.role-assignment-granted` | consumed | Links the record waiting for that email in that branch (CUSTOMER only) |
| `...profiler-service.role-assignment-revoked` | consumed | Removes the link; the email stays (CUSTOMER only) |

## Miscellaneous

- `platform.tenant.root-read-enabled: true` lets a PLATFORM_ADMIN read across banks here (see `core/platform-core/README.md`).

## Work in Progress

- `email` can't be changed after init, so a wrong address needs the role revoked and a new record.

