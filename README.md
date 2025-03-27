# Email Demo

## Errors

### 454 Throttling failure: Maximum sending rate exceeded

```text
Caused by: org.eclipse.angus.mail.smtp.SMTPSendFailedException: 454 Throttling failure: Maximum sending rate exceeded.
```

- [Guava](https://github.com/google/guava) RateLimiter
- [Resilience4j](https://github.com/resilience4j/resilience4j) RateLimiter and CircuitBreaker

### 554 Transaction failed: Recipient count exceeds 50.

```text
Caused by: org.eclipse.angus.mail.smtp.SMTPSendFailedException: 554 Transaction failed: Recipient count exceeds 50.
```