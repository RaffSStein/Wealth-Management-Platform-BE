package raff.stein.customer.client.eu.sanctions;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import raff.stein.customer.client.eu.sanctions.config.SanctionClientConfig;
import raff.stein.customer.client.eu.sanctions.model.screen.ScreenRequest;
import raff.stein.customer.client.eu.sanctions.model.screen.ScreenResponse;

@FeignClient(
        name = "sanctionClient",
        url = "${spring.application.rest.client.sanction.host}",
        configuration = SanctionClientConfig.class
)
public interface SanctionClient {

    @PostMapping(value = "/screen-client", consumes = "application/json", produces = "application/json")
    ScreenResponse screenClient(@RequestBody @Valid ScreenRequest request);

}
