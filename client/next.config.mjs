/** @type {import('next').NextConfig} */
const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8525';

// Parse host/port for Next remotePatterns
const remotePatterns = [
  { protocol: 'http', hostname: 'localhost', port: '8525' },
  { protocol: 'http', hostname: '127.0.0.1' },
  { protocol: 'https', hostname: 'backend.haryanabusinfo.in' },
  { protocol: 'https', hostname: 'haryanabusinfo.in' },
  { protocol: 'https', hostname: '*.haryanabusinfo.in' },
];

try {
  const parsed = new URL(API_URL);
  const pattern = {
    protocol: parsed.protocol.replace(':', ''),
    hostname: parsed.hostname,
  };
  if (parsed.port) {
    pattern.port = parsed.port;
  }
  remotePatterns.push(pattern);
} catch (e) {
  // ignore — defaults remain
}

const nextConfig = {
  output: 'standalone',
  images: {
    remotePatterns,
  },
  // Proxy API and asset requests to backend
  async rewrites() {
    const targetUrl = API_URL.replace(/\/$/, '');
    return [
      {
        source: '/api/:path*',
        destination: `${targetUrl}/api/:path*`,
      },
      {
        source: '/tickets/:path*',
        destination: `${targetUrl}/tickets/:path*`,
      },
      {
        source: '/uploads/:path*',
        destination: `${targetUrl}/uploads/:path*`,
      },
    ];
  },
};

export default nextConfig;
