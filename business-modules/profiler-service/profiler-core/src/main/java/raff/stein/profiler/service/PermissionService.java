package raff.stein.profiler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.profiler.model.UserPermission;
import raff.stein.profiler.model.entity.RoleFeaturePermissionEntity;
import raff.stein.profiler.model.entity.mapper.RoleFeaturePermissionToUserPermissionMapper;
import raff.stein.profiler.repository.RoleFeaturePermissionRepository;

import java.util.List;

/**
 * Functional permissions (feature + permission code) of the current user: those of the role(s) in its token, i.e. of
 * the active role assignment, read from role_feature_permission on each request. Nothing is stored per user, so a
 * change to a role's permissions applies to everyone immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final RoleFeaturePermissionRepository roleFeaturePermissionRepository;
    private static final RoleFeaturePermissionToUserPermissionMapper mapper = RoleFeaturePermissionToUserPermissionMapper.MAPPER;

    /**
     * Permissions of the current user in the bank and branch of its token. Empty when the context, the bank, the
     * branch or the roles are missing (e.g. a user not linked to a bank yet).
     */
    @Transactional(readOnly = true)
    public UserPermission getUserSitemapByClaims() {
        final WMPContext context = SecurityContextHolder.getContext();
        if (context == null) {
            log.info("User context is not available.");
            return new UserPermission();
        }
        final String email = context.getEmail();
        if (StringUtils.isAnyBlank(email, context.getTenantId(), context.getBranchCode())
                || context.getRoles() == null || context.getRoles().isEmpty()) {
            log.info("User email, bank, branch or roles are not available in the context.");
            return new UserPermission();
        }
        final List<RoleFeaturePermissionEntity> rolePermissions =
                roleFeaturePermissionRepository.findAllByRoleIn(context.getRoles());
        if (rolePermissions.isEmpty()) {
            log.info("No feature permissions for roles {}", context.getRoles());
        }
        return mapper.toUserPermission(email, context.getTenantId(), rolePermissions);
    }
}
