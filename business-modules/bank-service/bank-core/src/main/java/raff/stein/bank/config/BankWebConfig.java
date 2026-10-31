package raff.stein.bank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Pages serialize as {content, page: {size, number, totalElements, totalPages}} (Spring Data PagedModel), a stable
 * JSON shape, instead of the raw PageImpl internals.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class BankWebConfig {
}
