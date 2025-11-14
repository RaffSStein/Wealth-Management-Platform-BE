package raff.stein.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${wmp.onboarding.password-setup-link}")
    private String passwordSetupLink;

    public void sendOnboardingEmail(
            @NotNull String to,
            @NotNull String userName) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("passwordSetupLink", passwordSetupLink);
        String htmlBody = templateEngine.process("onboarding-email-template", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Welcome to WMP " + userName + "!");
        helper.setText(htmlBody, true);

        mailSender.send(mimeMessage);
    }
}
