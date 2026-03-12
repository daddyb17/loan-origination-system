# API Examples

Base URL:

```bash
http://localhost:8080/api
```

## 1. Create a Loan Application

```bash
curl -X POST "http://localhost:8080/api/loan-applications" \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: demo-req-1001" \
  -d '{
    "userId": 1001,
    "loanType": "PERSONAL_LOAN",
    "amount": 50000.00,
    "termInMonths": 48,
    "annualIncome": 150000.00,
    "existingMonthlyDebt": 2000.00,
    "creditScore": 730
  }'
```

Example response:

```json
{
  "applicationId": "a4c0f4b2-4f06-4b1e-8fd1-8ab2d4fe187d",
  "userId": 1001,
  "loanType": "PERSONAL_LOAN",
  "amount": 50000.00,
  "termInMonths": 48,
  "annualIncome": 150000.00,
  "existingMonthlyDebt": 2000.00,
  "creditScore": 730,
  "status": "SUBMITTED",
  "applicationDate": "2026-03-12 15:40:31",
  "lastUpdated": "2026-03-12 15:40:31"
}
```

## 2. List Applications with Filters + Pagination

```bash
curl "http://localhost:8080/api/loan-applications?status=SUBMITTED&page=0&size=10"
```

## 3. Trigger Underwriting Process

```bash
curl -X POST "http://localhost:8080/api/loan-applications/{applicationId}/process"
```

## 4. Update Status

```bash
curl -X PUT "http://localhost:8080/api/loan-applications/{applicationId}/status" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "UNDER_REVIEW"
  }'
```

## 5. Delete Finalized Application

```bash
curl -X DELETE "http://localhost:8080/api/loan-applications/{applicationId}"
```

## 6. Validation Error Example

```bash
curl -X POST "http://localhost:8080/api/loan-applications" \
  -H "Content-Type: application/json" \
  -d '{}'
```

Example error response:

```json
{
  "timestamp": "2026-03-12T15:40:31.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/loan-applications",
  "correlationId": "f5e615ce-31cf-4af4-8f96-cf81f2ca981b",
  "details": [
    "userId: User ID is required",
    "loanType: Loan type is required",
    "amount: Amount is required"
  ]
}
```
