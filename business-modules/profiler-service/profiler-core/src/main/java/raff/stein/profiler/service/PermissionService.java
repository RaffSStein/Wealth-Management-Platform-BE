package raff.stein.profiler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import raff.stein.platformcore.security.context.SecurityContextHolder;
import raff.stein.platformcore.security.context.WMPContext;
import raff.stein.profiler.model.UserPermission;
import raff.stein.profiler.model.entity.FeatureEntity;
import raff.stein.profiler.model.entity.PermissionEntity;
import raff.stein.profiler.model.entity.UserFeaturePermissionEntity;
import raff.stein.profiler.model.entity.mapper.UserFeaturePermissionToUserPermissionMapper;
import raff.stein.profiler.model.sitemap.SitemapDefinition;
import raff.stein.profiler.model.sitemap.SitemapFeatureDefinition;
import raff.stein.profiler.model.sitemap.SitemapSectionDefinition;
import raff.stein.profiler.repository.FeatureRepository;
import raff.stein.profiler.repository.PermissionRepository;
import raff.stein.profiler.repository.UserFeaturePermissionRepository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

/**
 * Functional permissions (feature + permission code) of a user in a bank branch. They are derived from the roles the
 * user holds there (role assignments) through the per-role sitemap files {@code sitemap/{ROLE}_SITEMAP.json}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final UserFeaturePermissionRepository userFeaturePermissionRepository;
    private final FeatureRepository featureRepository;
    private final PermissionRepository permissionRepository;
    private final ObjectMapper objectMapper;
    private static final UserFeaturePermissionToUserPermissionMapper userFeaturePermissionMapper = UserFeaturePermissionToUserPermissionMapper.MAPPER;

    /**
     * Permissions of the current user in the bank and branch of its token (the active role assignment).
     * Empty when the context, the bank or the branch is missing, or when nothing is stored.
     */
    public UserPermission getUserSitemapByClaims() {
        final WMPContext context = SecurityContextHolder.getContext();
        if (context == null) {
            log.info("User context is not available.");
            return new UserPermission();
        }
        final String email = context.getEmail();
        final String bankCode = context.getTenantId();
        final String branchCode = context.getBranchCode();
        if (StringUtils.isAnyBlank(email, bankCode, branchCode)) {
            log.info("User email, bank or branch code is not available in the context.");
            return new UserPermission();
        }
        final List<UserFeaturePermissionEntity> userPermissions =
                userFeaturePermissionRepository.findAllByUserEmailAndBankCodeAndBranchCode(email, bankCode, branchCode);
        if (userPermissions.isEmpty()) {
            log.info("No user permissions found for email: [{}] in [{}/{}]", email, bankCode, branchCode);
            return new UserPermission();
        }
        return userFeaturePermissionMapper.toUserPermission(email, branchCode, userPermissions);
    }

    /**
     * Replaces the permissions of a user in a bank branch with those of the roles it still holds there.
     * Called after every assignment and revocation; an empty role list removes them all.
     * A role without a sitemap file (e.g. BANK_ADMIN) contributes no feature permission.
     */
    @Transactional
    public void syncUserPermissions(String userEmail, String bankCode, String branchCode, Collection<String> roles) {
        if (StringUtils.isAnyBlank(userEmail, bankCode, branchCode)) {
            log.warn("Skipping permission sync: user email, bank or branch code is blank");
            return;
        }
        userFeaturePermissionRepository.deleteByUserEmailAndBankCodeAndBranchCode(userEmail, bankCode, branchCode);
        final Set<String> keys = new HashSet<>();
        final List<UserFeaturePermissionEntity> toPersist = new ArrayList<>();
        for (String role : Optional.ofNullable(roles).orElse(List.of())) {
            loadSitemapForRole(normalizeRole(role)).ifPresent(sitemap ->
                    toPersist.addAll(computePermissions(sitemap, userEmail, bankCode, branchCode, keys)));
        }
        userFeaturePermissionRepository.saveAll(toPersist);
        log.info("Synced [{}] feature permissions for user [{}] in [{}/{}] from roles {}",
                toPersist.size(), userEmail, bankCode, branchCode, roles);
    }

    /**
     * Normalizes a role to uppercase using ROOT locale to guarantee stable classpath resource names.
     */
    private String normalizeRole(String role) {
        return Optional.ofNullable(role).map(r -> r.trim().toUpperCase(Locale.ROOT)).orElse("");
    }

    /**
     * Loads the sitemap JSON for a normalized role from the classpath ({@code sitemap/{ROLE}_SITEMAP.json}).
     * Returns empty if the resource is missing or can't be parsed.
     */
    private Optional<SitemapDefinition> loadSitemapForRole(String normalizedRole) {
        final String resourcePath = String.format("sitemap/%s_SITEMAP.json", normalizedRole);
        try {
            final ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.info("No sitemap for role [{}] at [{}]: no feature permissions", normalizedRole, resourcePath);
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(resource.getInputStream(), SitemapDefinition.class));
        } catch (IOException e) {
            log.error("Failed to load sitemap for role [{}]", normalizedRole, e);
            return Optional.empty();
        }
    }

    /**
     * Translates a sitemap into permissions to persist, skipping (feature, permission) pairs already collected
     * for another role in the same branch.
     */
    private List<UserFeaturePermissionEntity> computePermissions(
            SitemapDefinition sitemap, String userEmail, String bankCode, String branchCode, Set<String> keys) {
        final List<UserFeaturePermissionEntity> result = new ArrayList<>();
        for (SitemapSectionDefinition section : Optional.ofNullable(sitemap.getSections()).orElse(List.of())) {
            for (SitemapFeatureDefinition feature : Optional.ofNullable(section.getFeatures()).orElse(List.of())) {
                final Optional<FeatureEntity> featureEntity = featureRepository
                        .findBySection_SectionCodeAndFeatureCode(section.getSectionCode(), feature.getFeatureCode());
                if (featureEntity.isEmpty()) {
                    log.warn("Feature not found for sectionCode=[{}], featureCode=[{}]",
                            section.getSectionCode(), feature.getFeatureCode());
                    continue;
                }
                for (String permissionCode : Optional.ofNullable(feature.getPermissions()).orElse(List.of())) {
                    addPermission(featureEntity.get(), permissionCode, userEmail, bankCode, branchCode, keys, result);
                }
            }
        }
        return result;
    }

    private void addPermission(
            FeatureEntity feature, String rawPermissionCode, String userEmail, String bankCode, String branchCode,
            Set<String> keys, List<UserFeaturePermissionEntity> acc) {
        final String permissionCode = StringUtils.trimToEmpty(rawPermissionCode).toUpperCase(Locale.ROOT);
        if (permissionCode.isEmpty()) {
            return;
        }
        final Optional<PermissionEntity> permission = permissionRepository.findByPermissionCode(permissionCode);
        if (permission.isEmpty()) {
            log.warn("Permission not found for code=[{}]", permissionCode);
            return;
        }
        if (keys.add(feature.getId() + ":" + permission.get().getId())) {
            acc.add(UserFeaturePermissionEntity.builder()
                    .userEmail(userEmail)
                    .bankCode(bankCode)
                    .branchCode(branchCode)
                    .feature(feature)
                    .permission(permission.get())
                    .build());
        }
    }
}
