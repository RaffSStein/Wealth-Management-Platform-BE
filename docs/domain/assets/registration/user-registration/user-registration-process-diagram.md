@startuml
actor EndUser as "End User"
actor FE as "Frontend (CRM/BO)"
participant "user-service" as User
participant "bank-service" as Bank
queue "user-created-topic" as userCreatedTopic
participant "email-service" as Reporting
participant "profiler-service" as Profiler
queue "user-permission-created-topic" as userPermissionCreatedTopic

activate EndUser
EndUser -> FE: open registration\npage
EndUser -> FE: submit registration\nform
activate FE
FE -> FE: fill in  registration\nuser data
FE -> User: POST /auth/register\nregister user
activate User
User -> User: validate user data
User -> User: save user to DB
User -> userCreatedTopic: publish user-created event
User -> FE: 201 Created\nuser data
deactivate User
FE -> EndUser: show registration\nsuccess message
deactivate FE
deactivate EndUser
userCreatedTopic --> Profiler: consume user-created event
activate Profiler
Profiler -> Profiler: save profile user data\n(customer/advisor)
Profiler -> userPermissionCreatedTopic: publish user-permission-created event
deactivate Profiler
userCreatedTopic --> Reporting: consume user-created event
activate Reporting
Reporting -> Reporting: send onboarding email to user
deactivate Reporting
activate EndUser
EndUser -> EndUser: open email\ninbox
EndUser -> EndUser: click verification\nlink
EndUser -> FE: open bank selection\nand password setup page
activate FE
FE -> Bank: GET /banks\nlist banks
activate Bank
Bank -> FE: 200 OK\nbank list
deactivate Bank
FE -> EndUser: show bank\nselection page
EndUser -> FE: select or create bank\nand submit
EndUser -> FE: set password and\nsubmit
FE -> User: PUT /user/{id}\nupdate user bank\nand password
activate User
User -> User: update user
User -> FE: 200 OK\nuser data
deactivate User
FE -> EndUser: show setup\nsuccess message
FE -> EndUser: prompt to\nlogin
deactivate FE
deactivate EndUser

@enduml