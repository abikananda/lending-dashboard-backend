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
