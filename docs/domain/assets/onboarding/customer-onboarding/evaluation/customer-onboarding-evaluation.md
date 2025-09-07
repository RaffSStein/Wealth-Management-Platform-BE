@startuml
actor FE as "Frontend (WMP)"
participant "customer-service" as Customer
queue "customer-created-topic" as customerCreatedTopic
queue "customer-rejected-topic" as customerRejectedTopic
participant "email-service" as Email

== Final Step - Onboarding evaluation ==
activate Customer
Customer -> Customer: Evaluate AML verification
alt AML verification approved
Customer -> customerCreatedTopic: publish create user request\n(customer ID, user data)
customerCreatedTopic --> Customer: consume create user request\n(customer ID, user data)
Customer -> Customer:Save onboarding finalize\ndata and step
deactivate Customer
customerCreatedTopic --> Email: consume create user request
activate Email
Email -> Email: send customer onboarding email\n(customer ID, user data)
deactivate Email
else AML verification rejected
note right of Customer: Onboarding is rejected,\nno further actions are taken
activate Customer
Customer -> customerRejectedTopic: publish customer rejection\n(customer ID, reason)
customerRejectedTopic --> Customer: consume customer rejection event\n(customer ID, reason)
Customer -> Customer: Save onboarding finalize\ndata and step
deactivate Customer
customerRejectedTopic --> Email: consume customer rejection event
activate Email
Email -> Email: send customer rejection email\n(customer ID, reason)
deactivate Email
end
@enduml