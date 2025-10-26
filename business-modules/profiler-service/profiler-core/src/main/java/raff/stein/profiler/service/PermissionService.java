package raff.stein.profiler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
     * Builds the UserPermission view for the current authenticated user by reading the security context
     * and mapping persisted UserFeaturePermissionEntity records into a DTO.
     * <p>
     * Behavior:
     * - If context, email or bank code are missing, returns an empty UserPermission.
     * - If no persisted permissions exist for the user/bank pair, returns an empty UserPermission.
     * - Otherwise maps all records to a consolidated UserPermission.
     */
    public UserPermission getUserSitemapByClaims() {
        // This method should retrieve user claims and generate a sitemap based on those claims.
        // The implementation will depend on how user claims are stored and accessed.
        final WMPContext context = SecurityContextHolder.getContext();
        if (context == null) {
            // Handle the case where the user context is not available
            log.info("User context is not available.");
            return new UserPermission();
        }
        // Retrieve user permissions based on the user ID from the context
        final String email = context.getEmail();
        final String bankCode = context.getBankCode();
        if (StringUtils.isBlank(email) || StringUtils.isBlank(bankCode)) {
            log.info("User email or bank code is not available in the context.");
            return new UserPermission();
        }
        List<UserFeaturePermissionEntity> userPermissions =
                userFeaturePermissionRepository.findAllByUserEmailAndBranchCode(email, bankCode);
        if (userPermissions.isEmpty()) {
            log.info("No user permissions found for email: [{}] and bankCode: [{}]", email, bankCode);
            return new UserPermission();
        }
        return userFeaturePermissionMapper.toUserPermission(email, bankCode, userPermissions);
    }

    /**
     * Persists user permissions derived from role-based sitemaps for one user across multiple bank codes.
     * <p>
     * Contract:
     * - Input: userEmail (non-blank), list of pairs (bankCode, role) possibly with duplicates or blanks.
     * - For each pair, loads the corresponding sitemap JSON from classpath and saves missing (feature, permission) tuples.
     * - Idempotent per (feature, permission): existing tuples are not duplicated.
     * - Logs warnings for invalid input or missing resources and continues with other pairs.
     */
    @Transactional
    public void saveUserPermissions(String userEmail, List<ImmutablePair<String, String>> bankCodeRolePairs) {
        if (isInvalidInput(userEmail, bankCodeRolePairs)) {
            log.warn("Skipping saveUserPermissions: invalid input - userEmail or role pairs are empty");
            return;
        }

        for (ImmutablePair<String, String> pair : bankCodeRolePairs) {
            handleBankRolePair(userEmail, pair);
        }
    }

    /**
     * Handles a single (bankCode, role) pair:
     * - Validates inputs and normalizes role.
     * - Loads role sitemap from classpath (sitemap/{ROLE}_SITEMAP.json).
     * - Computes the delta against existing user permissions and persists only the missing ones.
     */
    private void handleBankRolePair(String userEmail, ImmutablePair<String, String> pair) {
        String bankCode = Optional.ofNullable(pair.getLeft()).map(String::trim).orElse("");
        String role = Optional.ofNullable(pair.getRight()).map(String::trim).orElse("");
        if (StringUtils.isAnyBlank(bankCode, role)) {
            log.warn("Skipping entry with blank bankCode or role: [{}]", pair);
            return;
        }

        String normalizedRole = normalizeRole(role);

        Optional<SitemapDefinition> sitemapOpt = loadSitemapForRole(normalizedRole);
        if (sitemapOpt.isEmpty()) {
            // logging handled in loadSitemapForRole
            return;
        }

        Set<String> existingKeys = preloadExistingKeys(userEmail, bankCode);
        List<UserFeaturePermissionEntity> toPersist = computePermissionsToPersist(
                sitemapOpt.get(), userEmail, bankCode, existingKeys);

        if (!toPersist.isEmpty()) {
            userFeaturePermissionRepository.saveAll(toPersist);
            log.info("Saved [{}] new user feature permissions for user [{}] and bank [{}] from role [{}]",
                    toPersist.size(), userEmail, bankCode, normalizedRole);
        } else {
            log.info("No new permissions to save for user [{}], bank [{}], role [{}]", userEmail, bankCode, normalizedRole);
        }
    }


    /**
     * Lightweight input check for the orchestration method.
     */
    private boolean isInvalidInput(String userEmail, List<ImmutablePair<String, String>> bankCodeRolePairs) {
        return StringUtils.isBlank(userEmail) || bankCodeRolePairs == null || bankCodeRolePairs.isEmpty();
    }

    /**
     * Normalizes a role to uppercase using ROOT locale to guarantee stable classpath resource names.
     */
    private String normalizeRole(String role) {
        return Optional.ofNullable(role).map(r -> r.toUpperCase(Locale.ROOT)).orElse("");
    }

    /**
     * Loads the sitemap JSON for a normalized role from the classpath.
     * <p>
     * Resource pattern: sitemap/{ROLE}_SITEMAP.json
     * Returns empty if resource is missing or parsing fails; logs details for observability.
     */
    private Optional<SitemapDefinition> loadSitemapForRole(String normalizedRole) {
        String resourcePath = String.format("sitemap/%s_SITEMAP.json", normalizedRole);
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("Sitemap resource not found for role [{}] at [{}]", normalizedRole, resourcePath);
                return Optional.empty();
            }
            SitemapDefinition sitemap = objectMapper.readValue(resource.getInputStream(), SitemapDefinition.class);
            return Optional.ofNullable(sitemap);
        } catch (IOException e) {
            log.error("Failed to load sitemap for role [{}]", normalizedRole, e);
            return Optional.empty();
        }
    }

    /**
     * Preloads the set of (featureId:permissionId) keys already persisted for a user/bank pair.
     * This allows constant-time duplicate checks when computing the delta to persist.
     */
    private Set<String> preloadExistingKeys(String userEmail, String bankCode) {
        List<UserFeaturePermissionEntity> existing = userFeaturePermissionRepository
                .findAllByUserEmailAndBranchCode(userEmail, bankCode);
        return existing.stream()
                .map(ufp -> key(ufp.getFeature().getId(), ufp.getPermission().getId()))
                .collect(Collectors.toSet());
    }

    /**
     * Translates a sitemap into a list of UserFeaturePermissionEntity to persist, skipping what already exists.
     * Iterates sections -> features -> permissions, resolving codes to entities and performing idempotent checks.
     */
    private List<UserFeaturePermissionEntity> computePermissionsToPersist(
            SitemapDefinition sitemap,
            String userEmail,
            String bankCode,
            Set<String> existingKeys) {
        List<UserFeaturePermissionEntity> toPersist = new ArrayList<>();
        if (sitemap.getSections() == null) {
            return toPersist;
        }
        for (SitemapSectionDefinition section : sitemap.getSections()) {
            if (section.getFeatures() == null) {
                continue;
            }
            for (SitemapFeatureDefinition feature : section.getFeatures()) {
                processFeature(section, feature, userEmail, bankCode, existingKeys, toPersist);
            }
        }
        return toPersist;
    }

    /**
     * Resolves a feature by section/feature codes and forwards to permission expansion.
     * Logs and skips if the feature reference cannot be resolved.
     */
    private void processFeature(
            SitemapSectionDefinition section,
            SitemapFeatureDefinition feature,
            String userEmail,
            String bankCode,
            Set<String> existingKeys,
            List<UserFeaturePermissionEntity> acc) {
        Optional<FeatureEntity> featureOpt = featureRepository
                .findBySection_SectionCodeAndFeatureCode(section.getSectionCode(), feature.getFeatureCode());
        if (featureOpt.isEmpty()) {
            log.warn("Feature not found for sectionCode=[{}], featureCode=[{}]", section.getSectionCode(), feature.getFeatureCode());
            return;
        }
        FeatureEntity featureEntity = featureOpt.get();
        List<String> permissionCodes = Optional.ofNullable(feature.getPermissions()).orElse(Collections.emptyList());
        appendPermissionsForFeature(featureEntity, permissionCodes, userEmail, bankCode, existingKeys, acc);
    }

    /**
     * Expands permission codes for a resolved feature, maps them to PermissionEntity and creates
     * UserFeaturePermissionEntity entries only if not already present.
     * Uses a preloaded set of composite keys to enforce idempotency and avoid duplicates.
     */
    private void appendPermissionsForFeature(
            FeatureEntity featureEntity,
            List<String> permissionCodes,
            String userEmail,
            String bankCode,
            Set<String> existingKeys,
            List<UserFeaturePermissionEntity> acc) {
        for (String permCodeRaw : permissionCodes) {
            String permCode = StringUtils.trimToEmpty(permCodeRaw).toUpperCase(Locale.ROOT);
            if (!permCode.isEmpty()) {
                Optional<PermissionEntity> permOpt = permissionRepository.findByPermissionCode(permCode);
                if (permOpt.isPresent()) {
                    PermissionEntity permissionEntity = permOpt.get();
                    // Build a composite key (featureId:permissionId) to check existence in O(1)
                    String k = key(featureEntity.getId(), permissionEntity.getId());
                    if (!existingKeys.contains(k)) {
                        // Prepare an insert only when the pair is missing from the existing set
                        acc.add(UserFeaturePermissionEntity.builder()
                                .userEmail(userEmail)
                                .branchCode(bankCode)
                                .feature(featureEntity)
                                .permission(permissionEntity)
                                .build());
                        // Update the in-memory set to prevent duplicates within the same batch
                        existingKeys.add(k);
                    }
                } else {
                    log.warn("Permission not found for code=[{}]", permCode);
                }
            }
        }
    }
    // endregion

    /**
     * Builds a stable composite key used for quick duplicate detection.
     */
    private static String key(Integer featureId, Integer permissionId) {
        return featureId + ":" + permissionId;
    }
}
