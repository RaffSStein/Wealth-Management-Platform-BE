package raff.stein.customer.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Pages serialize as {content, page: {size, number, totalElements, totalPages}} (Spring Data PagedModel), a stable
 * JSON shape, instead of the raw PageImpl internals.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class CustomerWebConfig {

    // here and not in spring.data.web.pageable.*: @EnableSpringDataWebSupport makes those properties back off
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer customerPageableCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, 10));
            resolver.setMaxPageSize(100);
        };
    }
}
