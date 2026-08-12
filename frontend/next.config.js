/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: false, // Turned false to avoid double WS connections during development mount
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
};

module.exports = nextConfig;
