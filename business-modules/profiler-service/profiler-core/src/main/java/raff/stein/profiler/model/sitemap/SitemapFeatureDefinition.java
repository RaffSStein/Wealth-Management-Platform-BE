package raff.stein.profiler.model.sitemap;

import lombok.Data;

import java.util.List;

@Data
public class SitemapFeatureDefinition {
    private String featureCode;
    private String featureName;
    private List<String> permissions;
}

