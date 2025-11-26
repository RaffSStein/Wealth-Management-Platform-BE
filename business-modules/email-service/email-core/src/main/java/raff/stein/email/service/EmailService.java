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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${wmp.onboarding.password-setup-link}")
    private String passwordSetupLink;

    public void sendOnboardingEmail(
            @NotNull String to,
            @NotNull String userName,
            @NotNull String token) throws MessagingException {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("passwordSetupLink", buildPasswordSetupLink(token));
        String htmlBody = templateEngine.process("onboarding-email-template", context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Welcome to WMP " + userName + "!");
        helper.setText(htmlBody, true);

        mailSender.send(mimeMessage);
    }

    // Build the final FE link by embedding the token (supports {token} placeholder or query param)
    private String buildPasswordSetupLink(String token) {
        String base = passwordSetupLink;
        if (token == null || token.isBlank()) {
            return base;
        }
        String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        if (base.contains("{token}")) {
            return base.replace("{token}", encoded);
        }
        char sep = base.contains("?") ? '&' : '?';
        return base + sep + "token=" + encoded;
    }
}
