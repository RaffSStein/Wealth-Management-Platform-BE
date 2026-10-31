package raff.stein.profiler.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.profiler.model.Permission;
import raff.stein.profiler.model.UserPermission;
import raff.stein.profiler.model.entity.FeatureEntity;
import raff.stein.profiler.model.entity.PermissionEntity;
import raff.stein.profiler.model.entity.RoleFeaturePermissionEntity;
import raff.stein.profiler.model.entity.SectionEntity;
import raff.stein.profiler.repository.RoleFeaturePermissionRepository;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionServiceTest {

    // ids deliberately in the opposite order of display_order: the response must follow display_order
    private final SectionEntity workspace = SectionEntity.builder().id(1).displayOrder(4).sectionCode("WORKSPACE").sectionName("Workspace").application("WMP").build();
    private final SectionEntity account = SectionEntity.builder().id(9).displayOrder(1).sectionCode("ACCOUNT").sectionName("Account").application("WMP").build();
    private final FeatureEntity clients = FeatureEntity.builder().id(20).displayOrder(2).featureCode("CLIENTS").featureName("Clients").section(workspace).build();
    private final FeatureEntity home = FeatureEntity.builder().id(30).displayOrder(1).featureCode("HOME").featureName("Home").section(account).build();
    private final FeatureEntity transactions = FeatureEntity.builder().id(3).displayOrder(3).featureCode("TRANSACTIONS").featureName("Transactions").section(account).build();
    private final PermissionEntity view = PermissionEntity.builder().id(1).permissionCode("VIEW").permissionName("VIEW_PERMISSION").build();
    private final PermissionEntity use = PermissionEntity.builder().id(2).permissionCode("USE").permissionName("USE_PERMISSION").build();

    private RoleFeaturePermissionRepository repository;
    private PermissionService service;

    @BeforeEach
    void setUp() {
        repository = mock(RoleFeaturePermissionRepository.class);
        service = new PermissionService(repository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldMergeThePermissionsOfTheTokenRolesInDisplayOrder() {
        givenToken(Set.of("ADVISOR", "CUSTOMER"));
        when(repository.findAllByRoleIn(Set.of("ADVISOR", "CUSTOMER"))).thenReturn(List.of(
                grant("ADVISOR", clients, use),
                grant("ADVISOR", clients, view),
                grant("ADVISOR", transactions, view),
                grant("ADVISOR", home, view),
                // the same permission from a second role appears once
                grant("CUSTOMER", home, view)));

        final UserPermission permissions = service.getUserSitemapByClaims();

        assertEquals("BANK_A", permissions.getBankCode());
        assertEquals(List.of("ACCOUNT", "WORKSPACE"),
                permissions.getSectionPermissions().stream().map(UserPermission.SectionPermission::getSectionCode).toList());
        assertEquals(List.of("HOME", "TRANSACTIONS"), permissions.getSectionPermissions().get(0).getFeaturePermissions().stream()
                .map(UserPermission.FeaturePermission::getFeatureCode).toList());
        assertEquals(List.of("VIEW"), codes(permissions.getSectionPermissions().get(0).getFeaturePermissions().get(0)));
        assertEquals(List.of("VIEW", "USE"), codes(permissions.getSectionPermissions().get(1).getFeaturePermissions().get(0)));
    }

    @Test
    void shouldReturnNothingForATokenWithoutBank() {
        SecurityContextHolder.setContext(WMPContext.builder().email("new@user.test").roles(Set.of("CUSTOMER")).build());

        final UserPermission permissions = service.getUserSitemapByClaims();

        assertNull(permissions.getSectionPermissions());
        verify(repository, never()).findAllByRoleIn(any());
    }

    @Test
    void shouldReturnNoSectionsForARoleWithoutPermissions() {
        givenToken(Set.of("BANK_ADMIN"));
        when(repository.findAllByRoleIn(Set.of("BANK_ADMIN"))).thenReturn(List.of());

        final UserPermission permissions = service.getUserSitemapByClaims();

        assertEquals("BANK_A", permissions.getBankCode());
        assertNull(permissions.getSectionPermissions());
    }

    private static List<String> codes(UserPermission.FeaturePermission feature) {
        return feature.getPermissions().stream().map(Permission::getPermissionCode).toList();
    }

    private static RoleFeaturePermissionEntity grant(String role, FeatureEntity feature, PermissionEntity permission) {
        return RoleFeaturePermissionEntity.builder().role(role).feature(feature).permission(permission).build();
    }

    private static void givenToken(Set<String> roles) {
        SecurityContextHolder.setContext(WMPContext.builder()
                .email("ada@bank.test").roles(roles).tenantId("BANK_A").branchCode("BR_01").build());
    }
}
