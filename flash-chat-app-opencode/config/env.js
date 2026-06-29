const ENV = 'development'

const config = {
  development: {
    baseUrl: 'http://localhost:1000',
  },
  production: {
    baseUrl: 'https://api.flashchat.com',
  },
}

export const BASE_URL = config[ENV].baseUrl
