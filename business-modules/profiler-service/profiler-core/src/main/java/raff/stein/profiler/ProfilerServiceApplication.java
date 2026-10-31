package raff.stein.profiler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients
@ComponentScan(basePackages = {
        "raff.stein.platformcore", // Include platform core for shared components like security
        "raff.stein.profiler"
})
public class ProfilerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfilerServiceApplication.class, args);
    }
}
