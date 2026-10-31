@startuml
' profiler-service data model. Narrative, rules and a Mermaid version:
' docs/domain/process/security/roles-and-permissions.md (sections 9 and 12)

' =====================
' Permission catalogue (reference data, mirrors the FE navigation)
' =====================
class SectionEntity <<sections>> {
    Integer id
    String application
    String sectionCode {unique}
    String sectionName {unique}
    Integer displayOrder
}

class FeatureEntity <<features>> {
    Integer id
    String featureCode
    String featureName
    Integer displayOrder
    SectionEntity section
    -- unique (section, featureCode) --
}

class PermissionEntity <<permissions>> {
    Integer id
    String permissionCode {unique}
    String permissionName
}

' =====================
' What a role may do: users get the rows of the role in their token, computed on read
' =====================
class RoleFeaturePermissionEntity <<role_feature_permission>> {
    Integer id
    String role
    FeatureEntity feature
    PermissionEntity permission
    -- unique (role, feature, permission) --
}

' =====================
' Who holds which role where (source of truth; user-service keeps a read-only copy)
' =====================
class RoleAssignmentEntity <<role_assignments>> {
    Long id
    UUID userId
    String userEmail
    String bankCode
    String branchCode
    String role
    String grantedBy
    String approvedBy
    Long requestId
    -- unique (userId, bankCode, branchCode, role) --
}

class RoleAssignmentRequestEntity <<role_assignment_requests>> {
    Long id
    UUID userId
    String userEmail
    String bankCode
    String branchCode
    String role
    RoleAssignmentRequestStatus status
    String requestedBy
    String requestedByUserId
    Instant requestedAt
    String decidedBy
    Instant decidedAt
    String reason
}

class KnownUserEntity <<known_users>> {
    UUID userId
    String email
}

enum RoleAssignmentRequestStatus {
    PENDING
    APPROVED
    REJECTED
    EXPIRED
}

' =====================
' Relationships
' =====================
SectionEntity "1" --o "*" FeatureEntity : features
FeatureEntity "1" --o "*" RoleFeaturePermissionEntity : granted in
PermissionEntity "1" --o "*" RoleFeaturePermissionEntity : granted as
KnownUserEntity "1" --o "*" RoleAssignmentEntity : holds
KnownUserEntity "1" --o "*" RoleAssignmentRequestEntity : target of
RoleAssignmentRequestEntity "0..1" -- "0..1" RoleAssignmentEntity : approved into
RoleAssignmentEntity .. RoleFeaturePermissionEntity : same role value (WmpRole)

@enduml
