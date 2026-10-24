package org.app.backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.app.backend.modules.utils.AuthUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    String requestId = java.util.UUID.randomUUID().toString();
    response.setHeader("X-Request-ID", requestId);
    org.slf4j.MDC.put("requestId", requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - start;

      log.info(
          "[LOG] http_request requestId={} method={} path={} status={} duration={}ms ip={} user={}",
          requestId,
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs,
          AuthUtils.resolveIp(request),
          AuthUtils.resolveCaller());
      org.slf4j.MDC.remove("requestId");
    }
  }
}
