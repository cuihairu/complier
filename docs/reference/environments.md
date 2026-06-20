# Environments API

环境管理 API，用于管理游戏的不同部署环境（dev、staging、prod）。

## 端点列表

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/games/{gameId}/environments` | 创建环境 |
| GET | `/api/games/{gameId}/environments` | 获取环境列表 |
| GET | `/api/games/{gameId}/environments/{environmentName}` | 获取环境详情 |
| PUT | `/api/games/{gameId}/environments/{environmentName}` | 更新环境 |
| DELETE | `/api/games/{gameId}/environments/{environmentName}` | 删除环境 |

## 环境类型

| 类型 | 说明 |
|------|------|
| `DEVELOPMENT` | 开发环境 |
| `STAGING` | 预发布环境 |
| `PRODUCTION` | 生产环境 |

## 创建环境

```http
POST /api/games/{gameId}/environments
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "prod",
  "type": "PRODUCTION",
  "displayName": "Production Environment",
  "description": "Production environment for live players"
}
```

**响应:**
```json
{
  "id": "env_game_demo_prod",
  "gameId": "game_demo",
  "name": "prod",
  "type": "PRODUCTION",
  "displayName": "Production Environment",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

## 获取环境列表

```http
GET /api/games/{gameId}/environments
Authorization: Bearer {token}
```

**响应:**
```json
[
  {
    "id": "env_game_demo_dev",
    "name": "dev",
    "type": "DEVELOPMENT"
  },
  {
    "id": "env_game_demo_prod",
    "name": "prod",
    "type": "PRODUCTION"
  }
]
```

## 环境配置

每个环境可以独立配置：

- **API Key** - 环境专属的 API 密钥
- **采样率** - 事件采样比例
- **限流策略** - API 调用频率限制
- **PII 策略** - 个人数据处理规则
- **数据保留** - 数据保留时长

## 推荐环境

| 环境 | 用途 | 数据保留 |
|------|------|----------|
| `dev` | 开发测试 | 7 天 |
| `staging` | 预发布验证 | 30 天 |
| `prod` | 正式上线 | 365 天 |
