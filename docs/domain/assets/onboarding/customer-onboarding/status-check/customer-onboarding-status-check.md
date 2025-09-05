@startuml
actor FE as "Frontend (WMP)"
participant "customer-service" as Customer

== On-demand Step - check onboarding result/status ==
activate FE
note right of FE: Customer can check\nonboarding status of latest\n onboarding instance at any time
FE -> Customer: GET /customers/{customerId}/onboarding/getActiveOnboarding\n
deactivate FE
activate Customer
Customer -> Customer: Retrieve active onboarding instance
note right of Customer: Onboarding status can be:\n- In progress\n- Done\n- Failed\nbased on intermediate\nchecks (AML/document...)
Customer -> FE: 200 OK\n(onboarding instance)
deactivate Customer
activate FE
FE -> FE: Display onboarding status and details
alt Onboarding approved
note right of FE: Onboarding is approved/done\nand customer can use the platform,\nonboarding status cannot be checked\nand FE section will be hidden
else Onboarding rejected
FE -> FE: Display onboarding rejection message
FE -> FE: Redirect to Home page
FE -> FE: Customer can reapply
else Onboarding in progress
FE -> FE: Display onboarding in progress message
end
deactivate FE

@enduml