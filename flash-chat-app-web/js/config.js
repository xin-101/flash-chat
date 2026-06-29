const ENV = 'development';

const config = {
  development: {
    baseUrl: 'http://localhost:1000',
    wsUrl: 'ws://localhost:1000',
  },
  production: {
    baseUrl: 'https://api.flashchat.com',
    wsUrl: 'wss://api.flashchat.com',
  },
};

// served from gateway port 1000 → same-origin, use relative URLs (localStorage works)
// otherwise → use absolute URLs pointing to the gateway
const _cfg = config[ENV];
const _isGateway = window.location.port === '1000';
const AppConfig = {
  baseUrl: _isGateway ? '' : _cfg.baseUrl,
  wsUrl: _isGateway ? '' : _cfg.wsUrl,
};
