package io.oddsmaker.common.model;

import java.util.Map;

/**
 * Oddsmaker v1 event model.
 *
 * Note: game_id and environment are routing fields used by Gateway to determine
 * the target Kafka topic and database. They are NOT stored in ClickHouse as they
 * are implicit from the database/table name (e.g., game_demo_prod.events).
 */
public class Event {
    // Core identifiers
    public String eventId;
    public String gameId;           // Routing field (NOT stored in ClickHouse)
    public String environment;       // Routing field (NOT stored in ClickHouse)

    // Event classification
    public String eventType;         // session, user, business, resource, progression, design, error, ad, risk
    public String eventName;

    // Identity fields
    public String userId;            // User account identifier (optional)
    public String deviceId;          // Device identifier (required)
    public String playerId;          // Player identifier (game-specific, optional)
    public String characterId;       // Character identifier (for games with multiple characters, optional)
    public String sessionId;         // Session identifier (optional)

    // Timestamp fields
    public long tsClient;            // Client timestamp (epoch millis)
    public Long tsServer;           // Server timestamp (epoch millis, optional)

    // Client context
    public String platform;          // ios, android, web, pc, console
    public String appVersion;        // Application version
    public String sdkVersion;        // SDK version
    public String country;           // ISO 3166-1 alpha-2 country code
    public String clientIp;          // Derived from request
    public String userAgent;         // Derived from request

    // Game context (MMORPG support)
    public String serverId;          // Server/shard identifier (for multi-server games)
    public String guildId;           // Guild/clan identifier
    public String matchId;           // Match/session identifier (for matchmaking games)
    public String levelId;           // Level/stage identifier
    public String gameMode;          // pvp, pve, ranked, casual

    // Revenue fields
    public String orderId;           // Order/transaction identifier
    public String productId;         // Product/Item identifier
    public Double revenueAmount;
    public String revenueCurrency;
    public String receiptHash;       // Payment receipt hash for fraud detection

    // Resource flow fields
    public String resourceId;        // Resource identifier
    public Long resourceAmount;      // Resource amount
    public String flowType;          // source or sink
    public String operationId;       // Operation identifier (for linking related resource changes)
    public String operationType;     // Operation type (e.g., daily_reward, purchase_item)

    // Ad fields
    public String adNetwork;         // Ad network identifier
    public String adPlacement;       // Ad placement identifier
    public String adFormat;          // rewarded, interstitial, banner
    public String adImpressionId;    // Ad impression identifier

    // Experiment fields
    public Map<String, String> experiments;  // Active experiments and variants

    // Additional properties
    public Map<String, Object> props;
}
