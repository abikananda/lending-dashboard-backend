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
