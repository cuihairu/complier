-- 事件趋势
SELECT game_id, environment, event_date, event_name, countMerge(evts) AS events
FROM mv_events_by_day
GROUP BY game_id, environment, event_date, event_name
ORDER BY game_id, environment, event_date;

-- DAU
SELECT game_id, environment, event_date, uniqExactMerge(dau) AS dau
FROM mv_dau
GROUP BY game_id, environment, event_date
ORDER BY game_id, environment, event_date;
