@startuml
actor FE as "Frontend (CRM/BO)"
participant "user-service" as User
queue "user-created-topic" as userCreatedTopic
participant "email-service" as Reporting
participant "profiler-service" as Profiler
queue "user-permission-created-topic" as userPermissionCreatedTopic

activate FE
FE -> FE: fill in  registration\nuser data
FE -> User: POST /auth/register\nregister user
activate User
User -> User: validate user data
User -> User: save user to DB
User -> userCreatedTopic: publish user-created event
User -> FE: 201 Created\nuser data
deactivate User
FE -> FE: show registration\nsuccess message
deactivate FE
userCreatedTopic --> Profiler: consume user-created event
activate Profiler
Profiler -> Profiler: save profile user data\n(customer/advisor)
Profiler -> userPermissionCreatedTopic: publish user-permission-created event
deactivate Profiler
userCreatedTopic --> Reporting: consume user-created event
activate Reporting
Reporting -> Reporting: send onboarding email to user
deactivate Reporting

@enduml