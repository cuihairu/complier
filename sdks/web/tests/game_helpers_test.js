import { Oddsmaker } from '../dist/index.js';

function assert(cond, msg) {
  if (!cond) {
    console.error('FAIL:', msg);
    process.exitCode = 1;
  }
}

let capturedBody = '';
const originalFetch = globalThis.fetch;
const originalCompressionStream = globalThis.CompressionStream;

globalThis.CompressionStream = undefined;
globalThis.fetch = async (_url, init) => {
  capturedBody = String(init?.body || '');
  return {
    ok: true,
    status: 200,
    headers: new Headers()
  };
};

const pt = new Oddsmaker({
  apiKey: 'pk_test_example',
  endpoint: 'http://localhost:8080',
  gameId: 'game_demo',
  environment: 'prod',
  deviceId: 'd1',
  flushIntervalMs: 60_000
});

pt.setUserId('u1');
pt.levelComplete('3-1', { duration_sec: 91, stars: 3 });
pt.iapOrder('ord_1001', 4.99, 'usd', { product_id: 'gem_pack_s', store: 'app_store' });
pt.crash('NullReferenceException', { fatal: true, scene: 'battle' });
await pt.flush();
pt.shutdown();

const lines = capturedBody.trim().split('\n').map(line => JSON.parse(line));
assert(lines.length === 3, `expected 3 events, got ${lines.length}`);

const [levelComplete, iapOrder, crash] = lines;

assert(levelComplete.game_id === 'game_demo', 'levelComplete helper game_id');
assert(levelComplete.environment === 'prod', 'levelComplete helper environment');
assert(levelComplete.event_name === 'level_complete', 'levelComplete helper event name');
assert(levelComplete.event_type === 'progression', 'levelComplete helper event_type');
assert(levelComplete.level_id === '3-1', 'levelComplete helper top-level level_id');
assert(levelComplete.props.level_id === '3-1', 'levelComplete helper level_id');
assert(levelComplete.props.stars === 3, 'levelComplete helper stars');

assert(iapOrder.event_name === 'iap_order', 'iapOrder helper event name');
assert(iapOrder.event_type === 'business', 'iapOrder helper event_type');
assert(iapOrder.order_id === 'ord_1001', 'iapOrder top-level order_id');
assert(iapOrder.product_id === 'gem_pack_s', 'iapOrder top-level product_id');
assert(iapOrder.revenue_amount === 4.99, 'iapOrder top-level revenue_amount');
assert(iapOrder.revenue_currency === 'USD', 'iapOrder top-level revenue_currency');
assert(iapOrder.props.order_id === 'ord_1001', 'iapOrder order_id');
assert(iapOrder.props.product_id === 'gem_pack_s', 'iapOrder product_id');
assert(iapOrder.props.currency === 'USD', 'iapOrder props currency normalized');

assert(crash.event_name === 'crash', 'crash helper event name');
assert(crash.event_type === 'error', 'crash helper event_type');
assert(crash.props.error_name === 'NullReferenceException', 'crash helper error_name');
assert(crash.props.scene === 'battle', 'crash helper scene');

globalThis.fetch = originalFetch;
globalThis.CompressionStream = originalCompressionStream;

console.log('game_helpers_test: OK');
