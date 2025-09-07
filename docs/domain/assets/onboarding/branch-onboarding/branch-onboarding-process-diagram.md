@startuml
actor FE as "Frontend (CRM/BO)"
participant "bank-service" as Bank
queue "branch-created-topic" as branchCreatedTopic
queue "create-user-topic" as createUserTopic
participant "user-service" as User
queue "user-created-topic" as userCreatedTopic
participant "profiler-service" as Profiler
queue "permissions-created-topic" as permissionsCreatedTopic
participant "email-service" as Reporting

activate FE
FE -> Bank: POST /branches\n(branch data, user data)
deactivate FE
activate Bank
Bank -> Bank: Validate branch data
Bank -> Bank: Save branch to DB
Bank -> branchCreatedTopic: publish branch info
Bank -> createUserTopic: publish create root or custom user request
deactivate Bank

createUserTopic --> User: consume user\ncreation request
activate User
User -> User: Validate user data
User -> User: Save user and bank\nassociation to DB
User -> userCreatedTopic: publish user info
deactivate User

userCreatedTopic --> Profiler: consume user permissions data
activate Profiler
Profiler -> Profiler: Save permissions\nto DB
Profiler -> permissionsCreatedTopic: publish user\npermissions info
deactivate Profiler

userCreatedTopic --> Reporting: consume user creation event and send onboarding email
activate Reporting
Reporting -> Reporting: Send onboarding email to user


@enduml