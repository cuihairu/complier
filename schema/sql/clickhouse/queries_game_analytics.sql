-- Progression: daily level completion by level_id
SELECT game_id, environment, event_date, level_id, starts, completes, fails, completion_rate
FROM v_level_progress_daily
ORDER BY game_id, environment, event_date, level_id;

-- Economy: currency production vs consumption
SELECT game_id, environment, event_date, currency_code, produced_amount, consumed_amount, net_amount, affected_users
FROM v_economy_daily
ORDER BY game_id, environment, event_date, currency_code;

-- Monetization: IAP / webshop / ads daily revenue
SELECT game_id, environment, event_date, monetization_type, revenue_currency, revenue, paying_or_viewing_users, orders_or_impressions
FROM v_monetization_daily
ORDER BY game_id, environment, event_date, monetization_type;

-- Technical health: crash-free users and network problems
SELECT game_id, environment, event_date, active_users, crashes, crashed_users, crash_free_user_rate, fps_drop_events, network_timeouts
FROM v_tech_health_daily
ORDER BY game_id, environment, event_date;
