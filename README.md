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


## Manual upload email notification

After a new manual-lending report commits successfully, the backend sends an asynchronous email
containing the dashboard statistics before and after the upload and details of borrowers newly
moved to NPA. Duplicate uploads and rolled-back uploads do not send a notification.

Configure SMTP with a Gmail app password:

```powershell
$env:UPLOAD_NOTIFICATION_ENABLED="true"
$env:MAIL_USERNAME="sender@gmail.com"
$env:MAIL_APP_PASSWORD = Read-Host "Gmail app password" -MaskInput
$env:UPLOAD_NOTIFICATION_FROM="sender@gmail.com"
```

The recipient is the authenticated dashboard user's registered email address.
## Email repayment reconciliation

The backend can read LenDenClub repayment emails and Jana Bank or Slice Bank credit alerts from the Gmail
account belonging to a registered dashboard user. It validates the LenDenClub breakdown
(`principal + interest = total`) and matches the payout to an equal bank credit for the same
account within five weekdays.

Configure Gmail IMAP with an app password; do not use the normal Gmail password:

```powershell
$env:EMAIL_RECONCILIATION_ENABLED="true"
$env:GMAIL_IMAP_USERNAME="your-dashboard-email@example.com"
$env:GMAIL_IMAP_APP_PASSWORD = Read-Host "Gmail app password" -MaskInput
$env:BANK_EMAIL_SENDERS="noreply@jana.bank.in,noreply@slice.bank.in"
$env:EMAIL_CREDENTIAL_ENCRYPTION_KEY="<base64-encoded-32-byte-key>"
```

The scheduled sync runs daily at 11:00 PM in `Asia/Kolkata`. These authenticated endpoints are
also available:

```http
POST /api/v1/email-reconciliation/sync
GET  /api/v1/email-reconciliation?from=2026-09-01&to=2026-09-30
```

Statuses are `MATCHED`, `PENDING_BANK_CREDIT`, `BANK_CREDIT_MISSING`, `AMOUNT_MISMATCH`, or
`INVALID_EMI_BREAKDOWN`. Emails are stored idempotently using their Gmail message ID.

Multiple LenDenClub accounts can be mapped to separate Gmail inboxes and bank accounts through:

```http
GET    /api/v1/profile/reconciliation-accounts
POST   /api/v1/profile/reconciliation-accounts
PUT    /api/v1/profile/reconciliation-accounts/{id}
DELETE /api/v1/profile/reconciliation-accounts/{id}
```

```json
{
  "label": "Second LenDenClub account",
  "lenderId": "IAKI7TL1UT6K",
  "mailboxEmail": "your-second-email@gmail.com",
  "gmailAppPassword": "your-google-app-password",
  "bankSender": "noreply@slice.bank.in",
  "bankAccountLast4": "3003",
  "enabled": true
}
```

The app password is write-only and encrypted with AES-256-GCM. Omit it during an update to retain
the existing credential. Adding another account uses the same API; a new bank email layout requires
a corresponding parser template.

For a user's primary mailbox, save the Gmail app password once through the authenticated profile API:

```http
PUT /api/v1/profile/email-credentials
Content-Type: application/json

{"emailPassword":"your-google-app-password"}
```

The IMAP username comes from `users.email`. The password is encrypted before being stored in the
`users.email_password` column and is never returned by the API. Dedicated reconciliation-account
configuration continues to take precedence when the user has additional LenDenClub accounts.
