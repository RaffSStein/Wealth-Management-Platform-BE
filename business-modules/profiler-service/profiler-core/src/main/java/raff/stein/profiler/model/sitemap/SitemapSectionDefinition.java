package raff.stein.profiler.model.sitemap;

import lombok.Data;

import java.util.List;

@Data
public class SitemapSectionDefinition {
    private String sectionCode;
    private String sectionName;
    private List<SitemapFeatureDefinition> features;
}

