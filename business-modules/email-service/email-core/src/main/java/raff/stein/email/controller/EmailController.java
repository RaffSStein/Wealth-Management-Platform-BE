package raff.stein.email.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import raff.stein.email.service.EmailService;

@RestController
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

}
