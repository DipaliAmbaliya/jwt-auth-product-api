// Minimal Express layer: serves the forms and forwards /api/* calls to the Spring Boot backend.
// Requires Node 18+ (built-in fetch).
const express = require('express');
const path = require('path');

const PORT = process.env.PORT || 3000;
const BACKEND = process.env.BACKEND_URL || 'http://localhost:8080';

const app = express();
app.use(express.json({ limit: '10kb' }));
app.use(express.static(path.join(__dirname, 'public')));

app.use('/api', async (req, res) => {
  try {
    const headers = { 'Content-Type': 'application/json', 'X-Forwarded-For': req.ip };
    if (req.headers.authorization) headers['Authorization'] = req.headers.authorization;

    const hasBody = !['GET', 'HEAD'].includes(req.method);
    const upstream = await fetch(BACKEND + req.originalUrl, {
      method: req.method,
      headers,
      body: hasBody ? JSON.stringify(req.body ?? {}) : undefined,
    });

    res.status(upstream.status);
    res.set('Content-Type', upstream.headers.get('content-type') || 'application/json');
    res.send(await upstream.text());
  } catch (err) {
    res.status(502).json({ message: 'Backend not reachable. Is the Spring Boot app running?' });
  }
});

app.listen(PORT, () => console.log(`Frontend on http://localhost:${PORT} -> backend ${BACKEND}`));
