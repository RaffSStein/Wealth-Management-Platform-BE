@startuml
actor FE as "Frontend (WMP)"
participant "customer-service" as Customer

== On-demand Step - check onboarding result/status ==
activate FE
note right of FE: Customer can check\nonboarding status at any time
FE -> Customer: GET /customers/{customerId}/onboarding/status\n
deactivate FE
activate Customer
Customer -> Customer: Retrieve onboarding status
note right of Customer: Onboarding status can be:\n- In progress\n- Approved\n- Rejected\nbased on intermediate\nchecks (AML/document...)
Customer -> FE: 200 OK\n(onboarding status)
deactivate Customer
activate FE
FE -> FE: Display onboarding status
alt Onboarding approved
note right of FE: Onboarding is approved\nand customer can use the platform,\nonboarding status cannot be checked\nand FE section will be hidden
else Onboarding rejected
FE -> FE: Display onboarding rejection message
FE -> FE: Redirect to Home page
FE -> FE: Customer can reapply
else Onboarding in progress
FE -> FE: Display onboarding in progress message
end
deactivate FE

@enduml