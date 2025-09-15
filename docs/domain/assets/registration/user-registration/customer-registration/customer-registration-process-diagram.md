@startuml
actor FE as "Frontend (CRM/BO)"
participant "bank-service" as Bank
participant "user-service" as User
queue "user-created-topic" as userCreatedTopic
participant "email-service" as Reporting

activate FE
FE -> Bank: GET /branches\nretrieve branches by filters
activate Bank
Bank -> FE: 200 OK\nlist of branches
deactivate Bank
FE -> FE: fill in user data
FE -> User: POST /users\ncreate user
activate User
User -> User: validate user data
User -> User: save user to DB
User -> userCreatedTopic: publish user-created event
User -> FE: 201 Created\nuser data
deactivate FE
deactivate User
userCreatedTopic --> Reporting: consume user-created event
activate Reporting
Reporting -> Reporting: send onboarding email to user
deactivate Reporting

@enduml