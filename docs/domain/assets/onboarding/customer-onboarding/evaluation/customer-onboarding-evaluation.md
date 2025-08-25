@startuml
actor FE as "Frontend (WMP)"
participant "customer-service" as Customer
queue "customer-created-topic" as customerCreatedTopic
queue "customer-rejected-topic" as customerRejectedTopic
participant "email-service" as Email

== Final Step - Onboarding evaluation ==
alt Customer onboarding approved
activate Customer
Customer -> Customer: Store customer approval\nreason and status
Customer -> customerCreatedTopic: publish create user request\n(customer ID, user data)
deactivate Customer
customerCreatedTopic --> Email: consume create user request
activate Email
Email -> Email: send customer onboarding email\n(customer ID, user data)
deactivate Email
else Customer onboarding rejected
note right of Customer: Onboarding is rejected,\nno further actions are taken
activate Customer
Customer -> Customer: Store customer rejection\nreason and status
Customer -> customerRejectedTopic: publish customer rejection\n(customer ID, reason)
deactivate Customer
customerRejectedTopic --> Email: consume customer rejection event
activate Email
Email -> Email: send customer rejection email\n(customer ID, reason)
deactivate Email
end
@enduml