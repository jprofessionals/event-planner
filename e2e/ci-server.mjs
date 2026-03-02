// Lightweight static file server + API reverse proxy for CI.
// Serves the SvelteKit static build and proxies /api/* to the backend.
// Usage: node ci-server.mjs <static-dir> [port]

import { createServer, request as httpRequest } from 'node:http';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';

const STATIC = process.argv[2] || 'frontend/build';
const PORT = parseInt(process.argv[3] || '4173');
const BACKEND = 'http://localhost:8080';

const MIME = {
	'.html': 'text/html',
	'.js': 'application/javascript',
	'.css': 'text/css',
	'.json': 'application/json',
	'.png': 'image/png',
	'.svg': 'image/svg+xml',
	'.ico': 'image/x-icon',
	'.woff2': 'font/woff2',
	'.woff': 'font/woff',
};

createServer((req, res) => {
	if (req.url.startsWith('/api')) {
		const proxy = httpRequest(
			`${BACKEND}${req.url}`,
			{ method: req.method, headers: { ...req.headers, host: 'localhost:8080' } },
			(proxyRes) => {
				res.writeHead(proxyRes.statusCode, proxyRes.headers);
				proxyRes.pipe(res);
			},
		);
		proxy.on('error', () => res.writeHead(502).end('Backend unavailable'));
		req.pipe(proxy);
		return;
	}

	let file = join(STATIC, req.url);
	if (!existsSync(file) || statSync(file).isDirectory()) {
		file = join(STATIC, '200.html');
	}
	const mime = MIME[extname(file)] || 'application/octet-stream';
	res.writeHead(200, { 'Content-Type': mime });
	createReadStream(file).pipe(res);
}).listen(PORT, () => console.log(`CI server on :${PORT}`));
