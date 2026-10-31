package raff.stein.profiler.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.profiler.model.UserPermission;
import raff.stein.profiler.model.entity.FeatureEntity;
import raff.stein.profiler.model.entity.PermissionEntity;
import raff.stein.profiler.model.entity.RoleFeaturePermissionEntity;
import raff.stein.profiler.model.entity.SectionEntity;
import raff.stein.profiler.model.mapper.common.ProfilerCommonMapperConfiguration;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(
        config = ProfilerCommonMapperConfiguration.class,
        uses = {PermissionEntityToPermissionMapper.class}
)
public interface RoleFeaturePermissionToUserPermissionMapper {

    RoleFeaturePermissionToUserPermissionMapper MAPPER = Mappers.getMapper(RoleFeaturePermissionToUserPermissionMapper.class);

    // display order first (rows without one last), id as tie-breaker
    Comparator<SectionEntity> SECTION_ORDER = Comparator
            .comparing(SectionEntity::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SectionEntity::getId);
    Comparator<FeatureEntity> FEATURE_ORDER = Comparator
            .comparing(FeatureEntity::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(FeatureEntity::getId);

    /**
     * Groups the permissions of the user's roles by section and feature. A permission granted by several roles appears
     * once. Sections and features follow the catalogue's display order (FE navigation order).
     */
    default UserPermission toUserPermission(
            String userEmail,
            String bankCode,
            List<RoleFeaturePermissionEntity> rolePermissions) {
        final UserPermission.UserPermissionBuilder builder = UserPermission.builder()
                .userEmail(userEmail)
                .bankCode(bankCode);
        if (rolePermissions == null || rolePermissions.isEmpty()) {
            return builder.build();
        }
        final Map<FeatureEntity, List<PermissionEntity>> permissionsByFeature = rolePermissions.stream()
                .sorted(Comparator.comparing((RoleFeaturePermissionEntity rp) -> rp.getFeature().getId())
                        .thenComparing(rp -> rp.getPermission().getId()))
                .collect(Collectors.groupingBy(
                        RoleFeaturePermissionEntity::getFeature,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.mapping(RoleFeaturePermissionEntity::getPermission, Collectors.toList()),
                                permissions -> permissions.stream().distinct().toList())));
        final Map<Integer, List<FeatureEntity>> featuresBySection = permissionsByFeature.keySet().stream()
                .sorted(Comparator.comparing((FeatureEntity f) -> f.getSection(), SECTION_ORDER).thenComparing(FEATURE_ORDER))
                .collect(Collectors.groupingBy(f -> f.getSection().getId(), LinkedHashMap::new, Collectors.toList()));
        final List<UserPermission.SectionPermission> sectionPermissions = featuresBySection.values().stream()
                .map(features -> UserPermission.SectionPermission.builder()
                        // all features of the group share the section
                        .sectionCode(features.getFirst().getSection().getSectionCode())
                        .sectionName(features.getFirst().getSection().getSectionName())
                        .featurePermissions(features.stream()
                                .map(feature -> UserPermission.FeaturePermission.builder()
                                        .featureCode(feature.getFeatureCode())
                                        .featureName(feature.getFeatureName())
                                        .permissions(permissionsByFeature.get(feature).stream()
                                                .map(PermissionEntityToPermissionMapper.MAPPER::toPermission)
                                                .toList())
                                        .build())
                                .toList())
                        .build())
                .toList();
        return builder.sectionPermissions(sectionPermissions).build();
    }
}
