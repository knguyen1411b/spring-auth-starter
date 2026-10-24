# Security Policy

## Supported code

Security fixes target the latest code on this repository's default branch. Older snapshots and downstream forks must update or apply the relevant fixes. Using this starter does not transfer responsibility for securing your deployment to its maintainers.

## Reporting a vulnerability

Do not disclose exploitable vulnerabilities, credentials, tokens, or personal data in public issues, pull requests, or discussions.

Use this repository's **Security â†’ Report a vulnerability** option to submit a private report. Repository owners should enable GitHub private vulnerability reporting before publishing this template. If that option is unavailable, use a private contact method listed by the repository maintainer. Do not post sensitive details publicly.

Include the affected commit, relevant configuration with secrets removed, reproduction steps, expected and observed behavior, and the likely impact. Use synthetic accounts and data. Coordinate disclosure with the maintainer after a fix is available.

## Authentication guarantees and boundaries

- Passwords are BCrypt hashes. New passwords have a 72-byte UTF-8 limit.
- Refresh and password-reset tokens are random; only their SHA-256 hashes are stored.
- Access tokens reference server-side sessions. Protected requests check session ownership, expiration, revocation, and account state against the database without a principal/session cache.
- Logout, password recovery, and administrative account locking revoke the relevant sessions. Revocation applies to subsequently authenticated requests; it cannot cancel an operation already in progress.
- Recovery tokens are single-use and expiring. Password recovery invalidates all sessions and outstanding recovery links. Issuing another recovery link invalidates the previous one.
- Recovery requests use a generic HTTP response and a database-backed per-account resend cooldown. These measures do not guarantee identical response timing and do not replace IP-based rate limits.
- Permission checks are enforced in application services. CORS is not an authorization mechanism, and client-provided device names/IDs are not independent authentication factors.

There is no MFA, OAuth login, or refresh-token-family replay detection. The included tests verify implemented behavior; they are not a penetration test or a security certification.

## Deployment responsibilities

- Replace local example passwords, generate a unique signing secret, and use your deployment's secret manager. Never bake secrets into an image or commit `.env`. Rotate any credential that has been exposed; deleting the file does not revoke it.
- Terminate HTTPS at a trusted ingress, use the correct public base URL for email links, and configure explicit allowed frontend origins.
- Apply request limits and monitoring to sign-in, sign-up, refresh, activation, and recovery endpoints. Protect SMTP from abuse and configure a reliable mail provider.
- Keep the session checks on a consistent database source; caching or lagging replicas can weaken immediate revocation guarantees.
- Disable administrator bootstrap after initial provisioning. Do not grant administrative roles automatically to registered users.
- Run all application instances with the same updated session-validation behavior when upgrading. Legacy JWTs without session identity must not be accepted; require a new sign-in when adopting session-backed authentication.
- Keep application, JVM, action, and image dependencies patched. Review the source and configuration changes before adopting an update; back up and test database migrations.
- Restrict database and monitoring access. The included Compose ports bind to localhost, Mailpit captures local mail, and its database defaults are for development only.
- Bearer credentials are sent through the Authorization header and CSRF is disabled for that model. Switching authentication to cookies requires a separate cookie/CSRF design.

## Repository and CI settings

Enable private vulnerability reporting, require review and passing CI on the default branch, and restrict workflow token permissions. Enable secret scanning and dependency alerts where supported by your hosting plan. This repository's CI uses commit-pinned actions, wrapper validation, integration tests, and a Docker startup smoke test; it does not configure hosting-account protections for you.

See GitHub's [private vulnerability reporting documentation](https://docs.github.com/en/code-security/security-advisories/working-with-repository-security-advisories/about-coordinated-disclosure-of-security-vulnerabilities) and [Gradle Wrapper verification](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification).
