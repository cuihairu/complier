# API Reference

Welcome to the Oddsmaker API Reference. This section provides detailed documentation for all API endpoints.

## Base URL

```
http://localhost:8085/api
```

For production deployments, replace with your actual domain.

## Authentication

Most endpoints require authentication using JWT tokens:

```bash
Authorization: Bearer YOUR_JWT_TOKEN
```

Some endpoints use API keys:

```bash
x-api-key: YOUR_API_KEY
```

## API Versioning

The API uses URL versioning:

- **v1**: `/v1/` - Current stable version
- **No version**: `/api/` - Latest version (may change)

## Common Response Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 429 | Too Many Requests |
| 500 | Internal Server Error |

## Error Response Format

```json
{
  "error": "Error Type",
  "message": "Human-readable error message",
  "timestamp": "2024-01-01T00:00:00Z",
  "path": "/api/resource"
}
```

## Pagination

List endpoints support pagination:

```bash
GET /api/games?page=0&size=20&sort=name,asc
```

Response format:

```json
{
  "items": [...],
  "total": 100,
  "page": 0,
  "size": 20
}
```

## Rate Limiting

API endpoints are rate-limited:

- **Default**: 1000 requests per minute per API key
- **Burst**: 100 requests per second

Rate limit headers:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1700000060
```

## API Sections

### Core APIs

- [Games API](/reference/games) - Game management
- [Environments API](/reference/environments) - Environment management
- [API Keys API](/reference/api-keys) - API key management

### Analytics APIs

- [Experiments API](/reference/experiments) - A/B testing
- [Events API](/reference/events) - Event tracking
- [Reports API](/reference/reports) - Report generation

### Risk Management APIs

- [Risk Rules API](/reference/risk-rules) - Risk rule management
- [Risk Cases API](/reference/risk-cases) - Risk case management
- [Block Lists API](/reference/block-lists) - Block list management

### Advanced APIs

- [ML Models API](/reference/ml-models) - Machine learning models
- [Integrations API](/reference/integrations) - External integrations
- [Developer API](/reference/developer) - SDK and developer tools

### System APIs

- [Health API](/reference/health) - Health checks
- [Metrics API](/reference/metrics) - Prometheus metrics
- [Admin API](/reference/admin) - Administration

## SDKs

Official SDKs are available for:

- [JavaScript/TypeScript](https://github.com/cuihairu/oddsmaker-sdk-js)
- [Android (Kotlin)](https://github.com/cuihairu/oddsmaker-sdk-android)
- [iOS (Swift)](https://github.com/cuihairu/oddsmaker-sdk-ios)
- [Unity (C#)](https://github.com/cuihairu/oddsmaker-sdk-unity)

## OpenAPI Specification

The OpenAPI specification is available at:

```
http://localhost:8085/v3/api-docs
```

Swagger UI is available at:

```
http://localhost:8085/swagger-ui.html
```

## Support

For API support:

- [GitHub Issues](https://github.com/cuihairu/oddsmaker/issues)
- [Email](mailto:api@oddsmaker.local)
