import http from 'node:http';
import { existsSync, watch } from 'node:fs';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { bundleProject } from './bundle.mjs';
import { distDir, indexHtml, rootDir } from './shared.mjs';

const clients = new Set();
const reloadScript = `const source = new EventSource('/__reload-events');source.onmessage = () => location.reload();`;
let buildInProgress = false;
let rebuildQueued = false;

function contentType(filePath) {
  const extension = path.extname(filePath).toLowerCase();
  switch (extension) {
    case '.html':
      return 'text/html; charset=utf-8';
    case '.js':
      return 'text/javascript; charset=utf-8';
    case '.css':
      return 'text/css; charset=utf-8';
    case '.json':
      return 'application/json; charset=utf-8';
    case '.svg':
      return 'image/svg+xml';
    case '.png':
      return 'image/png';
    case '.jpg':
    case '.jpeg':
      return 'image/jpeg';
    case '.webp':
      return 'image/webp';
    case '.ico':
      return 'image/x-icon';
    case '.map':
      return 'application/json; charset=utf-8';
    case '.woff2':
      return 'font/woff2';
    default:
      return 'application/octet-stream';
  }
}

function notifyClients() {
  for (const response of clients) {
    response.write('data: reload\n\n');
  }
}

async function readRequestBody(request) {
  const chunks = [];

  for await (const chunk of request) {
    chunks.push(chunk);
  }

  return chunks.length > 0 ? Buffer.concat(chunks) : undefined;
}

async function proxyRequest(request, response, targetOrigin) {
  const targetUrl = new URL(request.url ?? '/', targetOrigin);
  const headers = { ...request.headers };

  delete headers.host;
  delete headers.connection;

  try {
    const proxiedResponse = await fetch(targetUrl, {
      method: request.method,
      headers,
      body: request.method === 'GET' || request.method === 'HEAD' ? undefined : await readRequestBody(request),
    });

    const responseHeaders = {};
    proxiedResponse.headers.forEach((value, key) => {
      if (key.toLowerCase() !== 'transfer-encoding') {
        responseHeaders[key] = value;
      }
    });

    response.writeHead(proxiedResponse.status, responseHeaders);
    response.end(Buffer.from(await proxiedResponse.arrayBuffer()));
  } catch (error) {
    response.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    response.end(
      JSON.stringify({
        message: 'Upstream API service is unavailable',
        target: targetOrigin,
        details: error instanceof Error ? error.message : 'Unknown proxy error',
      }),
    );
  }
}

async function buildProject() {
  if (buildInProgress) {
    rebuildQueued = true;
    return;
  }

  buildInProgress = true;
  try {
    await bundleProject({ mode: 'development', clean: true });
    notifyClients();
  } finally {
    buildInProgress = false;
    if (rebuildQueued) {
      rebuildQueued = false;
      await buildProject();
    }
  }
}

async function start() {
  await buildProject();

  watch(path.join(rootDir, 'src'), { recursive: true }, () => {
    void buildProject();
  });

  watch(indexHtml, () => {
    void buildProject();
  });

  const server = http.createServer(async (request, response) => {
    const requestUrl = new URL(request.url ?? '/', 'http://localhost');

    if (requestUrl.pathname.startsWith('/api/auth/')) {
      await proxyRequest(request, response, process.env.AUTH_SERVICE_URL ?? 'http://localhost:18080');
      return;
    }

    if (requestUrl.pathname.startsWith('/api/marketplace/')) {
      await proxyRequest(request, response, process.env.MARKETPLACE_SERVICE_URL ?? 'http://localhost:18081');
      return;
    }

    if (requestUrl.pathname.startsWith('/api/subscriptions/')) {
      await proxyRequest(request, response, process.env.SUBSCRIPTION_SERVICE_URL ?? 'http://localhost:18086');
      return;
    }

    if (requestUrl.pathname.startsWith('/api/admin/notifications')) {
      await proxyRequest(request, response, process.env.NOTIFICATION_SERVICE_URL ?? 'http://localhost:18083');
      return;
    }

    if (requestUrl.pathname.startsWith('/api/notifications')) {
      await proxyRequest(request, response, process.env.NOTIFICATION_SERVICE_URL ?? 'http://localhost:18083');
      return;
    }

    if (
      requestUrl.pathname.startsWith('/api/client/') ||
      requestUrl.pathname.startsWith('/api/tutor/') ||
      requestUrl.pathname.startsWith('/api/public/tutors/')
    ) {
      await proxyRequest(request, response, process.env.CRM_SERVICE_URL ?? 'http://localhost:18082');
      return;
    }

    if (requestUrl.pathname.startsWith('/api/admin/') || requestUrl.pathname.startsWith('/api/moderator/')) {
      await proxyRequest(request, response, process.env.ADMIN_SERVICE_URL ?? 'http://localhost:18084');
      return;
    }

    if (requestUrl.pathname === '/__reload.js') {
      response.writeHead(200, { 'Content-Type': 'text/javascript; charset=utf-8' });
      response.end(reloadScript);
      return;
    }

    if (requestUrl.pathname === '/__reload-events') {
      response.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache, no-transform',
        Connection: 'keep-alive',
      });
      response.write('\n');
      clients.add(response);
      request.on('close', () => {
        clients.delete(response);
      });
      return;
    }

    const decodedPath = decodeURIComponent(requestUrl.pathname);
    const relativePath = decodedPath === '/' ? 'index.html' : decodedPath.slice(1);
    const filePath = path.join(distDir, relativePath);
    const hasExtension = path.extname(relativePath).length > 0;
    const fallbackToIndex = decodedPath === '/' || (!hasExtension && !existsSync(filePath));
    const resolvedPath = fallbackToIndex ? path.join(distDir, 'index.html') : filePath;

    if (!existsSync(resolvedPath)) {
      response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end('Not found');
      return;
    }

    const fileContents = await readFile(resolvedPath);
    const isHtml = path.extname(resolvedPath).toLowerCase() === '.html';
    const responseBody = isHtml
      ? fileContents
          .toString('utf8')
          .replace('</body>', `<script type="module" src="/__reload.js"></script></body>`)
      : fileContents;

    response.writeHead(200, { 'Content-Type': contentType(resolvedPath) });
    response.end(responseBody);
  });

  const port = Number(process.env.PORT ?? 4173);
  server.listen(port, () => {
    console.log(`Dev server running at http://localhost:${port}`);
  });
}

await start();
