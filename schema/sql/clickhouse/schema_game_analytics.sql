-- Game analytics views built on top of the raw `events` table.
-- These views expose game-native datasets for progression, economy,
-- monetization, and health using the v1 game_id + environment contract.

CREATE OR REPLACE VIEW v_level_progress_events AS
SELECT
  game_id,
  environment,
  event_date,
  ts_server,
  if(player_id != '', player_id, if(user_id != '', user_id, device_id)) AS uid,
  event_name,
  coalesce(nullIf(level_id, ''), props['level_id']) AS level_id,
  coalesce(nullIf(game_mode, ''), props['game_mode']) AS game_mode,
  toUInt32OrZero(props['attempt']) AS attempt,
  toUInt32OrZero(props['duration_sec']) AS duration_sec,
  toInt64OrZero(props['score']) AS score,
  props['fail_reason'] AS fail_reason
FROM events
WHERE event_name IN ('level_start', 'level_fail', 'level_complete');

CREATE OR REPLACE VIEW v_level_progress_daily AS
SELECT
  game_id,
  environment,
  event_date,
  level_id,
  game_mode,
  countIf(event_name = 'level_start') AS starts,
  countIf(event_name = 'level_fail') AS fails,
  countIf(event_name = 'level_complete') AS completes,
  uniqExactIf(uid, event_name = 'level_start') AS starters,
  uniqExactIf(uid, event_name = 'level_complete') AS completers,
  round(completes / nullIf(starts, 0), 4) AS completion_rate,
  avgIf(duration_sec, duration_sec > 0 AND event_name = 'level_complete') AS avg_complete_duration_sec,
  avgIf(attempt, attempt > 0 AND event_name = 'level_complete') AS avg_attempts_to_complete
FROM v_level_progress_events
GROUP BY game_id, environment, event_date, level_id, game_mode;

CREATE OR REPLACE VIEW v_economy_flows AS
SELECT
  game_id,
  environment,
  event_date,
  ts_server,
  if(player_id != '', player_id, if(user_id != '', user_id, device_id)) AS uid,
  event_name,
  coalesce(nullIf(props['currency_code'], ''), nullIf(virtual_currency, ''), nullIf(resource_id, '')) AS currency_code,
  if(virtual_amount != 0, toFloat64(virtual_amount), if(resource_amount != 0, toFloat64(resource_amount), toFloat64OrZero(props['amount']))) AS amount,
  props['source'] AS source,
  props['sink'] AS sink,
  props['reason'] AS reason,
  toFloat64OrZero(props['balance_after']) AS balance_after
FROM events
WHERE event_name IN ('currency_source', 'currency_sink');

CREATE OR REPLACE VIEW v_economy_daily AS
SELECT
  game_id,
  environment,
  event_date,
  currency_code,
  sumIf(amount, event_name = 'currency_source') AS produced_amount,
  sumIf(amount, event_name = 'currency_sink') AS consumed_amount,
  sumIf(amount, event_name = 'currency_source') - sumIf(amount, event_name = 'currency_sink') AS net_amount,
  uniqExact(uid) AS affected_users
FROM v_economy_flows
GROUP BY game_id, environment, event_date, currency_code;

CREATE OR REPLACE VIEW v_monetization_events AS
SELECT
  game_id,
  environment,
  event_date,
  ts_server,
  if(player_id != '', player_id, if(user_id != '', user_id, device_id)) AS uid,
  event_name,
  revenue_amount,
  coalesce(nullIf(toString(revenue_currency), ''), props['currency']) AS revenue_currency,
  coalesce(nullIf(order_id, ''), props['order_id']) AS order_id,
  coalesce(nullIf(product_id, ''), props['product_id']) AS product_id,
  props['store'] AS store,
  coalesce(nullIf(ad_placement, ''), props['placement_id']) AS placement_id,
  coalesce(nullIf(ad_network, ''), props['network']) AS network,
  coalesce(nullIf(ad_format, ''), props['ad_format']) AS ad_format
FROM events
WHERE event_name IN ('revenue', 'iap_order', 'webshop_order', 'ad_impression');

CREATE OR REPLACE VIEW v_monetization_daily AS
SELECT
  game_id,
  environment,
  event_date,
  event_name AS monetization_type,
  revenue_currency,
  count() AS orders_or_impressions,
  uniqExact(uid) AS paying_or_viewing_users,
  sum(revenue_amount) AS revenue,
  avgIf(revenue_amount, revenue_amount > 0) AS avg_revenue_per_event
FROM v_monetization_events
GROUP BY game_id, environment, event_date, monetization_type, revenue_currency;

CREATE OR REPLACE VIEW v_tech_health_daily AS
SELECT
  game_id,
  environment,
  event_date,
  uniqExact(if(player_id != '', player_id, if(user_id != '', user_id, device_id))) AS active_users,
  countIf(event_name = 'crash') AS crashes,
  uniqExactIf(if(player_id != '', player_id, if(user_id != '', user_id, device_id)), event_name = 'crash') AS crashed_users,
  countIf(event_name = 'fps_drop') AS fps_drop_events,
  avgIf(toFloat64OrZero(props['fps']), event_name = 'fps_drop') AS avg_reported_fps_on_drop,
  countIf(event_name = 'network_timeout') AS network_timeouts,
  round(1 - crashed_users / nullIf(active_users, 0), 4) AS crash_free_user_rate
FROM events
GROUP BY game_id, environment, event_date;
