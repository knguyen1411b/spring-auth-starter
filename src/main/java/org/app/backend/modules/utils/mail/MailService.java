package org.app.backend.modules.utils.mail;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailService {
  JavaMailSender mailSender;

  @org.springframework.beans.factory.annotation.Value("${application.public-base-url}")
  @lombok.experimental.NonFinal
  String publicBaseUrl;

  @org.springframework.beans.factory.annotation.Value("${application.mail-from}")
  @lombok.experimental.NonFinal
  String from;

  public void sendActivationEmail(String to, String token) {
    String activationLink =
        org.springframework.web.util.UriComponentsBuilder.fromUriString(publicBaseUrl)
            .path("/activate")
            .queryParam("token", token)
            .build()
            .toUriString();
    sendMail(
        to,
        "Kích hoạt tài khoản",
        """
            Chào bạn,

            Vui lòng click link bên dưới để kích hoạt tài khoản:
            %s

            Link có hiệu lực trong 24 giờ.
            """
            .formatted(activationLink));
  }

  public void sendPasswordResetEmail(String to, String token) {
    String link =
        org.springframework.web.util.UriComponentsBuilder.fromUriString(publicBaseUrl)
            .path("/reset-password")
            .fragment("token=" + token)
            .build()
            .toUriString();
    sendMail(
        to,
        "Reset your password",
        "Use this link to reset your password:\n"
            + link
            + "\nThis link is single-use and expires shortly. If you did not request it, ignore this email.");
  }

  public void sendPasswordChangedEmail(String to) {
    sendMail(
        to,
        "Your password was changed",
        "Your password has been reset and all sessions have been signed out. If you did not do this, contact your administrator immediately.");
  }

  public void sendMail(String to, String subject, String content) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(content);
    mailSender.send(message);
  }
}
