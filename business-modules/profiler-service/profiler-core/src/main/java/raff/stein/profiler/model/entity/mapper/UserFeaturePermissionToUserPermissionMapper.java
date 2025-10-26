package raff.stein.profiler.model.entity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import raff.stein.profiler.model.UserPermission;
import raff.stein.profiler.model.entity.FeatureEntity;
import raff.stein.profiler.model.entity.PermissionEntity;
import raff.stein.profiler.model.entity.UserFeaturePermissionEntity;
import raff.stein.profiler.model.mapper.common.ProfilerCommonMapperConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(
        config = ProfilerCommonMapperConfiguration.class,
        uses = {PermissionEntityToPermissionMapper.class}
)
public interface UserFeaturePermissionToUserPermissionMapper {

    UserFeaturePermissionToUserPermissionMapper MAPPER = Mappers.getMapper(UserFeaturePermissionToUserPermissionMapper.class);

    // Mapping from entity to model
    default UserPermission toUserPermission(
            String userEmail,
            String bankCode,
            List<UserFeaturePermissionEntity> userFeaturePermissionEntities) {

        UserPermission.UserPermissionBuilder builder = UserPermission.builder()
                .userEmail(userEmail)
                .bankCode(bankCode);
        // If no permissions are found, return an empty UserPermission
        if (userFeaturePermissionEntities == null || userFeaturePermissionEntities.isEmpty()) {
            return builder.build();
        }

        // group user permissions by feature
        Map<FeatureEntity, List<UserFeaturePermissionEntity>> featureMap = userFeaturePermissionEntities
                .stream()
                .collect(Collectors.groupingBy(UserFeaturePermissionEntity::getFeature));
        // group features by section code
        Map<String, List<FeatureEntity>> sectionToFeatures = featureMap.keySet()
                .stream()
                .collect(Collectors.groupingBy(f -> f.getSection().getSectionCode()));

        List<UserPermission.SectionPermission> sectionPermissions = sectionToFeatures.entrySet()
                .stream()
                .map(sectionEntry -> {
                    String sectionCode = sectionEntry.getKey();
                    List<FeatureEntity> features = sectionEntry.getValue();
                    // section name is the same for all features in the section
                    String sectionName = features.get(0).getSection().getSectionName();
                    List<UserPermission.FeaturePermission> featurePermissions = features.stream()
                            .map(feature -> {
                                List<PermissionEntity> permissions = featureMap.get(feature).stream()
                                        .map(UserFeaturePermissionEntity::getPermission)
                                        .distinct()
                                        .toList();
                                return UserPermission.FeaturePermission.builder()
                                        .featureCode(feature.getFeatureCode())
                                        .featureName(feature.getFeatureName())
                                        .permissions(permissions.stream()
                                                .map(PermissionEntityToPermissionMapper.MAPPER::toPermission)
                                                .toList())
                                        .build();
                            })
                            .toList();

                    return UserPermission.SectionPermission.builder()
                            .sectionCode(sectionCode)
                            .sectionName(sectionName)
                            .featurePermissions(featurePermissions)
                            .build();
                })
                .toList();
        builder.sectionPermissions(sectionPermissions);
        return builder.build();
    }


    // Mapping from model to entity (not always needed, but for completeness)
}
