# Lending Dashboard API

Requires Java 21 and Maven 3.9+.

```bash
mvn test
mvn spring-boot:run
```

The import pipeline is: checksum/import batch → raw staging row → validation → `BigDecimal` calculation → idempotent final upsert.

Characterization formula retained from PFMP:

`principalEmi = investedAmount / tenureMonths`

`principalReceived = min(floor(amountReceived / principalEmi) × principalEmi, investedAmount)`

`interestReceived = amountReceived - principalReceived`

Production deployments must supply database credentials and a strong JWT secret through environment variables.

## Email repayment reconciliation

The backend can read LenDenClub repayment emails and Jana Bank credit alerts from the Gmail
account belonging to a registered dashboard user. It validates the LenDenClub breakdown
(`principal + interest = total`) and matches the payout to an equal bank credit for the same
account within five weekdays.

Configure Gmail IMAP with an app password; do not use the normal Gmail password:

```powershell
$env:EMAIL_RECONCILIATION_ENABLED="true"
$env:GMAIL_IMAP_USERNAME="your-dashboard-email@example.com"
$env:GMAIL_IMAP_APP_PASSWORD = Read-Host "Gmail app password" -MaskInput
```

The scheduled sync runs daily at 11:00 PM in `Asia/Kolkata`. These authenticated endpoints are
also available:

```http
POST /api/v1/email-reconciliation/sync
GET  /api/v1/email-reconciliation?from=2026-09-01&to=2026-09-30
```

Statuses are `MATCHED`, `PENDING_BANK_CREDIT`, `BANK_CREDIT_MISSING`, `AMOUNT_MISMATCH`, or
`INVALID_EMI_BREAKDOWN`. Emails are stored idempotently using their Gmail message ID.
