package raff.stein.customer.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import raff.stein.customer.controller.access.CustomerAccessInterceptor;

@Configuration
@RequiredArgsConstructor
public class CustomerAccessWebConfig implements WebMvcConfigurer {

    private final CustomerAccessInterceptor customerAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(customerAccessInterceptor);
    }
}
