package org.app.backend.modules.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class AppController {
  @GetMapping("/reset-password")
  public String resetPasswordPage(HttpServletResponse response) {
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("Cache-Control", "no-store");
    return "reset-password";
  }

  @GetMapping("/activate")
  public String activatePage(@RequestParam("token") String token, Model model) {
    model.addAttribute("token", token);
    return "activate";
  }
}
