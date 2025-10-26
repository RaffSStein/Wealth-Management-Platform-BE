package raff.stein.profiler.model.sitemap;

import lombok.Data;

import java.util.List;

@Data
public class SitemapDefinition {
    private String application;
    private List<SitemapSectionDefinition> sections;
}

