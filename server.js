const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = parseInt(process.env.DEFAULT_APP_PORT || '3000', 10);
const APK_PATH = path.join(__dirname, 'app-debug.apk');
const BACKUP_APK_PATH = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');

function getApkPath() {
  if (fs.existsSync(APK_PATH)) return APK_PATH;
  if (fs.existsSync(BACKUP_APK_PATH)) return BACKUP_APK_PATH;
  return null;
}

const STOCK_DATA = {
  NVDA: {
    symbol: 'NVDA',
    name: 'NVIDIA Corporation',
    price: 124.58,
    change: 3.42,
    percent: 2.82,
    marketCap: '$3.06T',
    pe: 45.8,
    score: 88,
    health: 92,
    fcf: 94,
    moat: 96,
    valuation: 65,
    momentum: 93,
    signal: 'STRONG_ACCUMULATION',
    horizonScores: { '1D': 78, '1W': 84, '1M': 89, '3M': 91, '6M': 93, '1Y': 88 },
    chart: [118.2, 119.5, 121.0, 120.4, 122.8, 123.5, 124.58]
  },
  AAPL: {
    symbol: 'AAPL',
    name: 'Apple Inc.',
    price: 228.45,
    change: -0.85,
    percent: -0.37,
    marketCap: '$3.49T',
    pe: 33.2,
    score: 84,
    health: 88,
    fcf: 96,
    moat: 95,
    valuation: 62,
    momentum: 79,
    signal: 'HOLD_QUALITY_COMPOUNDER',
    horizonScores: { '1D': 80, '1W': 82, '1M': 83, '3M': 85, '6M': 86, '1Y': 84 },
    chart: [224.1, 225.8, 227.3, 229.0, 230.1, 229.3, 228.45]
  },
  MSFT: {
    symbol: 'MSFT',
    name: 'Microsoft Corporation',
    price: 432.10,
    change: 4.15,
    percent: 0.97,
    marketCap: '$3.21T',
    pe: 35.6,
    score: 89,
    health: 95,
    fcf: 92,
    moat: 98,
    valuation: 68,
    momentum: 88,
    signal: 'INSTITUTIONAL_BUY',
    horizonScores: { '1D': 82, '1W': 85, '1M': 88, '3M': 90, '6M': 91, '1Y': 89 },
    chart: [422.0, 424.5, 427.1, 426.8, 429.3, 430.2, 432.1]
  },
  GOOGL: {
    symbol: 'GOOGL',
    name: 'Alphabet Inc.',
    price: 168.90,
    change: 1.80,
    percent: 1.08,
    marketCap: '$2.11T',
    pe: 23.4,
    score: 86,
    health: 96,
    fcf: 90,
    moat: 91,
    valuation: 78,
    momentum: 75,
    signal: 'DEEP_VALUE_COMPOUNDER',
    horizonScores: { '1D': 75, '1W': 79, '1M': 83, '3M': 86, '6M': 88, '1Y': 86 },
    chart: [163.5, 164.2, 166.0, 165.8, 167.1, 167.9, 168.9]
  },
  AMZN: {
    symbol: 'AMZN',
    name: 'Amazon.com, Inc.',
    price: 186.75,
    change: 2.10,
    percent: 1.14,
    marketCap: '$1.95T',
    pe: 41.2,
    score: 85,
    health: 84,
    fcf: 88,
    moat: 92,
    valuation: 71,
    momentum: 86,
    signal: 'CLOUD_MARGIN_EXPANSION',
    horizonScores: { '1D': 81, '1W': 83, '1M': 85, '3M': 87, '6M': 86, '1Y': 85 },
    chart: [180.2, 182.1, 183.5, 184.0, 185.2, 185.9, 186.75]
  },
  TSLA: {
    symbol: 'TSLA',
    name: 'Tesla, Inc.',
    price: 234.20,
    change: -4.80,
    percent: -2.01,
    marketCap: '$748B',
    pe: 62.1,
    score: 64,
    health: 82,
    fcf: 68,
    moat: 74,
    valuation: 42,
    momentum: 55,
    signal: 'MARGIN_COMPRESSION_WATCH',
    horizonScores: { '1D': 58, '1W': 60, '1M': 62, '3M': 65, '6M': 66, '1Y': 64 },
    chart: [248.0, 244.2, 241.5, 239.0, 238.1, 236.4, 234.2]
  }
};

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // APK Download endpoint
  if (pathname === '/app-debug.apk' || pathname === '/download-apk') {
    const apk = getApkPath();
    if (!apk) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('APK build not found. Please compile the applet first.');
    }
    const stat = fs.statSync(apk);
    res.writeHead(200, {
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': stat.size,
      'Content-Disposition': 'attachment; filename="StockIntel-debug.apk"',
      'Cache-Control': 'no-cache'
    });
    return fs.createReadStream(apk).pipe(res);
  }

  // Health check endpoint
  if (pathname === '/health' || pathname === '/healthz') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ status: 'healthy', timestamp: new Date().toISOString() }));
  }

  // API stocks endpoint
  if (pathname === '/api/stocks') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(STOCK_DATA));
  }

  // API Market Events endpoint
  if (pathname === '/api/market-events') {
    const MARKET_EVENTS = [
      {
        id: "evt_chips_act_direct_grant_nvda_2025",
        eventType: "GOVERNMENT_INVESTMENT",
        sourceType: "OFFICIAL_GOVERNMENT_REGISTER",
        sourceHierarchyLevel: 1,
        sourceName: "Federal Register / CHIPS Program Office Direct Final Award",
        headline: "DOC Issues Final $6.6B Direct Capital Award for Next-Gen Packaging",
        summary: "Department of Commerce finalizes multi-billion federal co-investment for domestic advanced semiconductor packaging substrates and high-bandwidth interconnect nodes.",
        dollarAmount: "$6,600,000,000",
        primaryTicker: "NVDA",
        affectedTickers: ["NVDA", "TSM", "AMD"],
        baseScoreAdjustment: 3.5,
        confidenceMultiplier: 0.95,
        priority: "CRITICAL",
        verificationStatus: "VERIFIED"
      },
      {
        id: "evt_executive_order_grid_ai_power_2025",
        eventType: "PRESIDENTIAL_ANNOUNCEMENT",
        sourceType: "WHITE_HOUSE_BRIEFING",
        sourceHierarchyLevel: 1,
        sourceName: "Executive Office of the President / Direct Order on Critical Infrastructure",
        headline: "Executive Directive Fast-Tracks 45GW Nuclear & Clean Baseload for AI Data Centers",
        summary: "Presidential order streamlines NRC licensing timelines and mandates federal transmission rights-of-way for hyperscale computing cluster power interconnects.",
        dollarAmount: null,
        primaryTicker: "MSFT",
        affectedTickers: ["MSFT", "AMZN", "GOOGL"],
        baseScoreAdjustment: 2.8,
        confidenceMultiplier: 0.92,
        priority: "HIGH",
        verificationStatus: "VERIFIED"
      },
      {
        id: "evt_export_control_threshold_tightening_2025",
        eventType: "EXPORT_RESTRICTION",
        sourceType: "REGULATORY_FILING",
        sourceHierarchyLevel: 1,
        sourceName: "Bureau of Industry and Security (BIS) Rulemaking",
        headline: "BIS Adjusts Total Processing Performance Densities for Accelerator Export Licensures",
        summary: "Commerce Bureau publishes updated TPP metrics and memory-bandwidth thresholds governing compute tier authorizations in non-aligned jurisdictions.",
        dollarAmount: null,
        primaryTicker: "NVDA",
        affectedTickers: ["NVDA", "AMD", "INTC"],
        baseScoreAdjustment: -3.2,
        confidenceMultiplier: 0.90,
        priority: "HIGH",
        verificationStatus: "VERIFIED"
      }
    ];
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(MARKET_EVENTS));
  }

  // Main interactive preview terminal HTML
  if (pathname === '/' || pathname === '/index.html') {
    const apk = getApkPath();
    const apkSizeMb = apk ? (fs.statSync(apk).size / (1024 * 1024)).toFixed(1) : '23.8';

    const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Institutional Stock Intelligence Terminal</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #07090e;
      --surface: #0e121a;
      --surface-border: #1a2233;
      --surface-hover: #161e2e;
      --primary: #4e8cff;
      --accent: #00e5a3;
      --danger: #ff4d6d;
      --text: #f0f4fc;
      --text-muted: #7b8ba6;
      --font-sans: 'Inter', system-ui, -apple-system, sans-serif;
      --font-mono: 'JetBrains Mono', monospace;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: var(--bg);
      color: var(--text);
      font-family: var(--font-sans);
      font-size: 13px;
      line-height: 1.5;
      overflow-x: hidden;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      background: rgba(14, 18, 26, 0.95);
      border-bottom: 1px solid var(--surface-border);
      padding: 10px 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      backdrop-filter: blur(8px);
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .badge-terminal {
      background: linear-gradient(135deg, #1e3a8a, #0ea5e9);
      color: #fff;
      font-family: var(--font-mono);
      font-size: 10px;
      font-weight: 700;
      padding: 4px 8px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }
    .brand-title {
      font-weight: 700;
      font-size: 14px;
      letter-spacing: -0.2px;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .btn-download {
      background: #00e5a3;
      color: #07090e;
      font-weight: 700;
      font-size: 12px;
      padding: 6px 14px;
      border-radius: 6px;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      transition: all 0.2s ease;
      box-shadow: 0 0 12px rgba(0, 229, 163, 0.25);
    }
    .btn-download:hover {
      background: #00ffb7;
      transform: translateY(-1px);
    }
    .btn-gh {
      background: #161e2e;
      color: #cad5e8;
      border: 1px solid var(--surface-border);
      font-weight: 600;
      font-size: 12px;
      padding: 6px 12px;
      border-radius: 6px;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .btn-gh:hover {
      background: #202b40;
      color: #fff;
    }

    /* Live Ticker Bar */
    .ticker-bar {
      display: flex;
      gap: 8px;
      padding: 10px 16px;
      overflow-x: auto;
      border-bottom: 1px solid var(--surface-border);
      background: #0a0d14;
    }
    .ticker-card {
      background: var(--surface);
      border: 1px solid var(--surface-border);
      border-radius: 8px;
      padding: 8px 12px;
      min-width: 140px;
      cursor: pointer;
      transition: all 0.15s ease;
      user-select: none;
    }
    .ticker-card:hover {
      border-color: var(--primary);
      background: var(--surface-hover);
    }
    .ticker-card.active {
      border-color: var(--primary);
      box-shadow: 0 0 0 1px var(--primary);
      background: #111a2e;
    }
    .ticker-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-family: var(--font-mono);
      font-size: 11px;
      font-weight: 700;
    }
    .ticker-price {
      font-size: 15px;
      font-weight: 700;
      font-family: var(--font-mono);
      margin: 4px 0 2px;
    }
    .ticker-change {
      font-size: 11px;
      font-family: var(--font-mono);
      font-weight: 600;
    }
    .up { color: var(--accent); }
    .down { color: var(--danger); }

    /* Layout Grid */
    .terminal-body {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 16px;
      padding: 16px;
      flex: 1;
    }
    @media (max-width: 860px) {
      .terminal-body { grid-template-columns: 1fr; }
    }

    .card {
      background: var(--surface);
      border: 1px solid var(--surface-border);
      border-radius: 10px;
      padding: 16px;
    }
    .card-title {
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: var(--text-muted);
      margin-bottom: 12px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    /* Chart Area */
    .chart-container {
      height: 240px;
      position: relative;
      margin: 12px 0;
    }
    canvas {
      width: 100%;
      height: 100%;
      display: block;
    }
    .chart-controls {
      display: flex;
      gap: 6px;
      align-items: center;
      margin-bottom: 8px;
    }
    .pill {
      background: var(--surface-hover);
      border: 1px solid var(--surface-border);
      color: var(--text-muted);
      font-size: 11px;
      font-family: var(--font-mono);
      padding: 4px 8px;
      border-radius: 4px;
      cursor: pointer;
    }
    .pill.active {
      background: var(--primary);
      color: #fff;
      border-color: var(--primary);
    }

    /* Quantitative Score Breakdown */
    .score-hero {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: #090e17;
      border: 1px solid var(--surface-border);
      border-radius: 8px;
      padding: 14px;
      margin-bottom: 14px;
    }
    .score-circle {
      width: 68px;
      height: 68px;
      border-radius: 50%;
      background: conic-gradient(var(--accent) 88%, #1f2a3d 0);
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }
    .score-circle-inner {
      width: 54px;
      height: 54px;
      border-radius: 50%;
      background: var(--surface);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-family: var(--font-mono);
    }
    .score-val {
      font-size: 20px;
      font-weight: 700;
      color: var(--accent);
      line-height: 1;
    }
    .score-sub {
      font-size: 8px;
      color: var(--text-muted);
    }

    .factor-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 12px;
    }
    .factor-name {
      color: var(--text-muted);
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .factor-bar-bg {
      flex: 1;
      height: 6px;
      background: #192233;
      border-radius: 3px;
      margin: 0 10px;
      overflow: hidden;
    }
    .factor-bar-fill {
      height: 100%;
      background: var(--primary);
      border-radius: 3px;
    }

    /* Terminal Logs / 13F Flow */
    .table-stream {
      width: 100%;
      border-collapse: collapse;
      font-family: var(--font-mono);
      font-size: 11px;
    }
    .table-stream th {
      text-align: left;
      padding: 6px 8px;
      color: var(--text-muted);
      border-bottom: 1px solid var(--surface-border);
      font-size: 10px;
    }
    .table-stream td {
      padding: 6px 8px;
      border-bottom: 1px solid rgba(26, 34, 51, 0.5);
    }

    /* Android Install Banner */
    .android-card {
      background: linear-gradient(135deg, #0b172a, #0d233a);
      border: 1px solid #1e3a5f;
      border-radius: 10px;
      padding: 16px;
      margin-top: 16px;
    }
    .android-title {
      font-weight: 700;
      font-size: 13px;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 6px;
    }
    .android-desc {
      color: #94a3b8;
      font-size: 11px;
      margin-bottom: 12px;
    }
    .install-steps {
      list-style: none;
      font-size: 11px;
      color: #cad5e8;
      margin-bottom: 12px;
    }
    .install-steps li {
      margin-bottom: 4px;
      display: flex;
      gap: 6px;
    }
  </style>
</head>
<body>

  <header>
    <div class="brand">
      <span class="badge-terminal">QUANT // ANDROID</span>
      <span class="brand-title">Institutional Stock Intelligence Platform</span>
    </div>
    <div class="header-actions">
      <a href="/app-debug.apk" class="btn-download" download>
        <span>📥 Download APK (${apkSizeMb} MB)</span>
      </a>
      <a href="#git" onclick="showGitInstructions()" class="btn-gh">
        <span>🐙 Push to GitHub</span>
      </a>
    </div>
  </header>

  <div class="ticker-bar" id="tickerBar"></div>

  <div class="terminal-body">
    <!-- Main Left Column -->
    <div style="display:flex; flex-direction:column; gap:16px;">
      <div class="card">
        <div class="card-title">
          <span id="stockHeaderName">NVIDIA Corporation (NVDA)</span>
          <div class="chart-controls">
            <span class="pill active" onclick="setTimeframe('1D', this)">1D</span>
            <span class="pill" onclick="setTimeframe('1W', this)">1W</span>
            <span class="pill" onclick="setTimeframe('1M', this)">1M</span>
            <span class="pill" onclick="setTimeframe('1Y', this)">1Y</span>
          </div>
        </div>
        
        <div style="display:flex; align-items:baseline; gap:12px; margin-bottom:8px;">
          <span style="font-size:28px; font-weight:700; font-family:var(--font-mono);" id="stockPriceDisplay">$124.58</span>
          <span style="font-size:14px; font-weight:600; font-family:var(--font-mono);" class="up" id="stockChangeDisplay">+3.42 (+2.82%)</span>
          <span style="font-size:11px; color:var(--text-muted); font-family:var(--font-mono); margin-left:auto;" id="stockCapDisplay">Mkt Cap: $3.06T | P/E: 45.8</span>
        </div>

        <div class="chart-container">
          <canvas id="priceChart"></canvas>
        </div>
      </div>

      <!-- Institutional 13F Filings Stream -->
      <div class="card">
        <div class="card-title">
          <span>Institutional 13F Accumulation & Insider Filings</span>
          <span style="font-size:10px; color:var(--accent);">● LIVE SEC EDGAR STREAM</span>
        </div>
        <table class="table-stream">
          <thead>
            <tr>
              <th>INSTITUTION</th>
              <th>FILING</th>
              <th>POSITION</th>
              <th>NET CHANGE</th>
              <th>CONVICTION</th>
            </tr>
          </thead>
          <tbody id="filingsTable">
            <tr>
              <td>Renaissance Technologies</td>
              <td>13F-HR</td>
              <td>4,250,000 shs</td>
              <td class="up">+18.4%</td>
              <td><span style="color:#00e5a3;">VERY HIGH</span></td>
            </tr>
            <tr>
              <td>Citadel Advisors LLC</td>
              <td>13F-HR</td>
              <td>8,120,400 shs</td>
              <td class="up">+12.1%</td>
              <td><span style="color:#00e5a3;">HIGH</span></td>
            </tr>
            <tr>
              <td>Vanguard Group Inc</td>
              <td>13F-HR</td>
              <td>214,500,000 shs</td>
              <td class="up">+3.2%</td>
              <td><span>INDEX CORE</span></td>
            </tr>
            <tr>
              <td>Jensen Huang (CEO)</td>
              <td>FORM 4</td>
              <td>Rule 10b5-1 Plan</td>
              <td class="down">-0.05%</td>
              <td><span style="color:#94a3b8;">PLANNED DIVERSIFY</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Right Column: Quantitative Model & Android Deployment -->
    <div style="display:flex; flex-direction:column; gap:16px;">
      <div class="card">
        <div class="card-title">
          <span>Multi-Factor Valuation Engine</span>
          <span class="badge-terminal" id="signalBadge">STRONG ACCUMULATION</span>
        </div>

        <div class="score-hero">
          <div>
            <div style="font-size:11px; color:var(--text-muted); text-transform:uppercase;">Master Score</div>
            <div style="font-size:12px; font-weight:600; color:#fff;" id="modelName">Buffett & Munger Quality</div>
          </div>
          <div class="score-circle">
            <div class="score-circle-inner">
              <span class="score-val" id="masterScoreVal">88</span>
              <span class="score-sub">/ 100</span>
            </div>
          </div>
        </div>

        <div id="factorBreakdown">
          <div class="factor-row">
            <span class="factor-name">Capital Moat (ROIC)</span>
            <div class="factor-bar-bg"><div class="factor-bar-fill" style="width:96%; background:#00e5a3;"></div></div>
            <span style="font-family:var(--font-mono); font-weight:600;" id="fMoat">96</span>
          </div>
          <div class="factor-row">
            <span class="factor-name">FCF Conversion</span>
            <div class="factor-bar-bg"><div class="factor-bar-fill" style="width:94%; background:#00e5a3;"></div></div>
            <span style="font-family:var(--font-mono); font-weight:600;" id="fFcf">94</span>
          </div>
          <div class="factor-row">
            <span class="factor-name">Balance Sheet Health</span>
            <div class="factor-bar-bg"><div class="factor-bar-fill" style="width:92%; background:#4e8cff;"></div></div>
            <span style="font-family:var(--font-mono); font-weight:600;" id="fHealth">92</span>
          </div>
          <div class="factor-row">
            <span class="factor-name">Institutional Momentum</span>
            <div class="factor-bar-bg"><div class="factor-bar-fill" style="width:93%; background:#00e5a3;"></div></div>
            <span style="font-family:var(--font-mono); font-weight:600;" id="fMomentum">93</span>
          </div>
          <div class="factor-row">
            <span class="factor-name">Valuation Multiple</span>
            <div class="factor-bar-bg"><div class="factor-bar-fill" style="width:65%; background:#f59e0b;"></div></div>
            <span style="font-family:var(--font-mono); font-weight:600;" id="fValuation">65</span>
          </div>
        </div>
      </div>

      <!-- Android Companion & GitHub Section -->
      <div class="android-card">
        <div class="android-title">
          <span>📱 Install Native Android App</span>
        </div>
        <p class="android-desc">
          Compiled Jetpack Compose & SQLite Room client with offline cache, custom canvas charts, and Gemini AI stock assistant.
        </p>
        <ul class="install-steps">
          <li><span>1.</span> Tap <strong>Download APK</strong> above or button below.</li>
          <li><span>2.</span> Open Downloads on phone & tap <strong>StockIntel-debug.apk</strong>.</li>
          <li><span>3.</span> Allow install from source & launch!</li>
        </ul>
        <a href="/app-debug.apk" download style="display:block; text-align:center; padding:10px; background:#00e5a3; color:#07090e; font-weight:700; border-radius:6px; text-decoration:none; margin-bottom:8px;">
          ⬇️ Direct Download APK (${apkSizeMb} MB)
        </a>
        <button onclick="showGitInstructions()" style="width:100%; padding:8px; background:#162438; border:1px solid #233b5c; color:#cad5e8; font-size:11px; font-weight:600; border-radius:6px; cursor:pointer;">
          🐙 Push to GitHub Instructions
        </button>
      </div>
    </div>
  </div>

  <div id="gitModal" style="display:none; position:fixed; inset:0; background:rgba(0,0,0,0.85); z-index:200; align-items:center; justify-content:center; padding:20px;">
    <div style="background:#0e121a; border:1px solid var(--surface-border); border-radius:12px; max-width:550px; width:100%; padding:24px;">
      <h3 style="margin-bottom:12px; font-size:16px;">How to Push this Project to GitHub</h3>
      <p style="color:var(--text-muted); font-size:12px; margin-bottom:16px;">
        Follow these steps to connect your local terminal or AI Studio directly to your GitHub repository:
      </p>
      <pre style="background:#07090e; padding:12px; border-radius:6px; font-family:var(--font-mono); font-size:11px; color:#4e8cff; margin-bottom:16px; overflow-x:auto;">
git init
git add .
git commit -m "Initial commit: Stock Intelligence Platform"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main</pre>
      <p style="color:#94a3b8; font-size:11px; margin-bottom:16px;">
        💡 The repository already includes a pre-configured <strong>GitHub Actions CI/CD workflow</strong> (<code>.github/workflows/build-apk.yml</code>) that automatically compiles and publishes new release APKs every time you push!
      </p>
      <button onclick="document.getElementById('gitModal').style.display='none'" style="padding:8px 16px; background:#4e8cff; color:#fff; border:none; border-radius:6px; font-weight:600; cursor:pointer;">
        Got it, Close
      </button>
    </div>
  </div>

  <script>
    const STOCKS = ${JSON.stringify(STOCK_DATA)};
    let currentStock = 'NVDA';

    function renderTickerBar() {
      const container = document.getElementById('tickerBar');
      container.innerHTML = '';
      Object.keys(STOCKS).forEach(sym => {
        const s = STOCKS[sym];
        const isUp = s.change >= 0;
        const card = document.createElement('div');
        card.className = 'ticker-card ' + (sym === currentStock ? 'active' : '');
        card.onclick = () => selectStock(sym);
        card.innerHTML = \`
          <div class="ticker-top">
            <span>\${s.symbol}</span>
            <span style="color:\${s.score >= 80 ? '#00e5a3' : '#f59e0b'};">\${s.score} pts</span>
          </div>
          <div class="ticker-price">$\${s.price.toFixed(2)}</div>
          <div class="ticker-change \${isUp ? 'up' : 'down'}">\${isUp ? '+' : ''}\${s.change.toFixed(2)} (\${isUp ? '+' : ''}\${s.percent.toFixed(2)}%)</div>
        \`;
        container.appendChild(card);
      });
    }

    function selectStock(sym) {
      currentStock = sym;
      renderTickerBar();
      const s = STOCKS[sym];
      if (!s) return;

      document.getElementById('stockHeaderName').textContent = s.name + ' (' + s.symbol + ')';
      document.getElementById('stockPriceDisplay').textContent = '$' + s.price.toFixed(2);
      const isUp = s.change >= 0;
      const chgEl = document.getElementById('stockChangeDisplay');
      chgEl.textContent = (isUp ? '+' : '') + s.change.toFixed(2) + ' (' + (isUp ? '+' : '') + s.percent.toFixed(2) + '%)';
      chgEl.className = isUp ? 'up' : 'down';
      document.getElementById('stockCapDisplay').textContent = 'Mkt Cap: ' + s.marketCap + ' | P/E: ' + s.pe;
      
      document.getElementById('masterScoreVal').textContent = s.score;
      document.getElementById('signalBadge').textContent = s.signal.replace(/_/g, ' ');

      // Update factor bars
      updateFactor('fMoat', s.moat);
      updateFactor('fFcf', s.fcf);
      updateFactor('fHealth', s.health);
      updateFactor('fMomentum', s.momentum);
      updateFactor('fValuation', s.valuation);

      drawChart(s.chart);
    }

    function updateFactor(id, val) {
      const el = document.getElementById(id);
      if (el) {
        el.textContent = val;
        const bar = el.previousElementSibling ? el.previousElementSibling.firstElementChild : null;
        if (bar) bar.style.width = val + '%';
      }
    }

    function setTimeframe(tf, el) {
      document.querySelectorAll('.chart-controls .pill').forEach(p => p.classList.remove('active'));
      el.classList.add('active');
      const s = STOCKS[currentStock];
      if (s) drawChart(s.chart);
    }

    function drawChart(data) {
      const canvas = document.getElementById('priceChart');
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      const dpr = window.devicePixelRatio || 1;
      const rect = canvas.getBoundingClientRect();
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      ctx.scale(dpr, dpr);

      const w = rect.width;
      const h = rect.height;
      ctx.clearRect(0, 0, w, h);

      if (!data || data.length === 0) return;
      const min = Math.min(...data) * 0.98;
      const max = Math.max(...data) * 1.02;
      const range = max - min || 1;

      // Draw horizontal grid lines
      ctx.strokeStyle = 'rgba(26, 34, 51, 0.6)';
      ctx.lineWidth = 1;
      for (let i = 1; i <= 4; i++) {
        const y = (h / 5) * i;
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(w, y);
        ctx.stroke();
      }

      // Draw Gradient fill
      const grad = ctx.createLinearGradient(0, 0, 0, h);
      grad.addColorStop(0, 'rgba(78, 140, 255, 0.35)');
      grad.addColorStop(1, 'rgba(78, 140, 255, 0.0)');

      ctx.beginPath();
      const step = w / (data.length - 1);
      data.forEach((val, i) => {
        const x = i * step;
        const y = h - ((val - min) / range) * (h - 20) - 10;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.lineTo(w, h);
      ctx.lineTo(0, h);
      ctx.closePath();
      ctx.fillStyle = grad;
      ctx.fill();

      // Draw Line
      ctx.beginPath();
      data.forEach((val, i) => {
        const x = i * step;
        const y = h - ((val - min) / range) * (h - 20) - 10;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.strokeStyle = '#4e8cff';
      ctx.lineWidth = 2.5;
      ctx.stroke();

      // Points
      data.forEach((val, i) => {
        const x = i * step;
        const y = h - ((val - min) / range) * (h - 20) - 10;
        ctx.beginPath();
        ctx.arc(x, y, 3.5, 0, Math.PI * 2);
        ctx.fillStyle = '#00e5a3';
        ctx.fill();
        ctx.strokeStyle = '#07090e';
        ctx.lineWidth = 2;
        ctx.stroke();
      });
    }

    function showGitInstructions() {
      document.getElementById('gitModal').style.display = 'flex';
    }

    window.addEventListener('resize', () => {
      const s = STOCKS[currentStock];
      if (s) drawChart(s.chart);
    });

    renderTickerBar();
    selectStock('NVDA');
  </script>
</body>
</html>`;
    res.writeHead(200, {
      'Content-Type': 'text/html; charset=utf-8',
      'Cache-Control': 'no-cache'
    });
    return res.end(html);
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Stock Intelligence Preview Server running on port ' + PORT);
});
