# Institutional Stock Intelligence Platform

An institutional-grade quantitative equity research, fundamental valuation, and technical analysis platform built with **Kotlin**, **Jetpack Compose (Material 3)**, **SQLite Room**, and powered by **Google Gemini 2.5**.

Designed for institutional investors, quantitative analysts, and serious compounders who demand sub-second financial statement audits, real-time market candlestick charting, SEC Form 13F / Form 4 flow tracking, and multi-factor valuation scoring.

---

## 📥 Direct One-Tap APK Download Links

### 🚀 [Download Latest APK (v1.0.0)](https://github.com/ALViNoMaRK/Ai-Quant-/releases/download/v1.0.0/stock-intel-latest.apk) (Instant Direct Download)

Tap the link above to immediately start the download of **`stock-intel-latest.apk`** (24.6 MB) onto your phone without loading GitHub's file previewer.

### Alternative Download Options:
- **[GitHub Releases Page](https://github.com/ALViNoMaRK/Ai-Quant-/releases/tag/v1.0.0)**: View Release v1.0.0 and tap `stock-intel-latest.apk` under **Assets**.
- **In File Tree**: Tap the blue text **"View raw"** on `stock-intel-latest.apk` to download.
- **AI Studio Code Explorer**: Right-click or tap the options menu (`⋮`) next to `stock-intel-latest.apk` in the root workspace and select **Download**.

---

## 📲 How to Install on Your Android Phone

1. **Download the APK** to your phone via your mobile browser or transfer it from your PC via USB / Google Drive.
2. Open your phone's **Files** or **Downloads** folder and tap on **`app-debug.apk`**.
3. If Android prompts *"For your security, your phone is not allowed to install unknown apps from this source"*:
   - Tap **Settings** in the popup.
   - Toggle **Allow from this source** to ON.
   - Tap the back button and tap **Install**.
4. Once installation completes, tap **Open** to launch the platform.

---

## ⚡ Key Capabilities & Features

### 1. Institutional Multi-Factor Scoring Engine (0–100)
- **Mathematical Factor Weighting**: Dynamically computes Master Scores based on:
  - **Financial Health & Balance Sheet** (Interest coverage, net debt-to-EBITDA, Altman Z-Score proxy)
  - **Free Cash Flow Generation** (FCF margin, FCF conversion rate, operating cash flow trajectory)
  - **Capital Efficiency & Moat** (ROIC, ROE, sustained gross margin superiority)
  - **Valuation Multiples** (Forward P/E, PEG ratio, EV/EBITDA, FCF yield)
  - **Institutional Momentum** (13F net accumulation, insider conviction trades)
- **Model Presets**: Switch instantly between **Balanced Master**, **Deep Value & Margin of Safety**, **High Growth & Rule of 40**, **Buffett/Munger Quality Compounder**, or create **Custom Factor Weights**.

### 2. High-Performance Candlestick Charting Terminal
- **Zero-Lag Custom Canvas**: Renders hundreds of OHLCV bars smoothly with 60 FPS gesture panning and pinch-to-zoom.
- **Multiple Timeframes & Ranges**: 1m, 5m, 15m, 1h, 1D, 1W with 1D, 5D, 1M, 3M, 6M, 1Y, 5Y ranges.
- **Chart Styles**: Candlestick, Line, Mountain Area, and Traditional Bar charts.
- **Built-in Technical Overlays**:
  - Triple EMA Ribbon (8, 50, 200 EMA)
  - Bollinger Bands with squeeze volatility detection
  - RSI Momentum Extremes (14-period with 70/30 institutional boundaries)
  - MACD Histogram and Signal Line Crossovers
  - Auto Swing High / Low Market Structure Detection
- **Pine Script v6 Runtime**: Custom lightweight Pine engine supporting user scripts, custom indicator scripts, and overlays.
- **Chart Drawings**: Interactive Horizontal Support / Resistance lines, Ray lines, and Trendlines.
- **Corporate Event Overlays**: On-chart markers for Earnings beats/misses, SEC 13F fund accumulation, and Form 4 insider buys.

### 3. Major Market Event Intelligence Engine
- **Verified Event Detection**: Ingests official government awards (CHIPS Act, DOE/DOD grants), presidential executive directives, export control revisions, antitrust rulings, corporate capital commitments, and M&A developments.
- **Strict Verification Hierarchy**:
  - **Level 1**: Official Government Register & SEC Filings (100% confidence weight).
  - **Level 2**: Corporate Disclosures & Earnings Calls (85% weight).
  - **Level 3**: Top-tier Wire Reporting (Reuters, Bloomberg, WSJ) (70% weight).
  - **Level 4**: Unverified Rumors / Social Media (0% weight - quarantined from scoring).
- **Mathematical Score Decomposition**: Preserves underlying fundamentals while cleanly displaying the calibrated event impact:
  $$\text{Current Intelligence Score} = \text{Base Fundamental Score} + \text{Event Impact Adjustment}$$
- **Time Decay Mechanics**: Exponential time decay with a 14-day half-life so event adjustments naturally expire as news is digested by the market.
- **Second-Order Security Propagation**: Automatically traces supply-chain beneficiaries, direct competitors, and enterprise customers across correlated equities (e.g. NVDA $\rightarrow$ TSM, ASML, AMD).
- **Dedicated Events Screen & Tab**: Browse detected market events with category chips, tap to view executive summaries, and inspect score impacts directly in the Stock Research overview.

### 4. Opportunity Scanner & Thesis Deterioration Detector
- **Automated Opportunity Scanner**: Identifies compounder dips, accelerating FCF inflection points, and low PEG mispricings.
- **Thesis Deterioration Early Warning**: Flags margin compression, unexpected FCF burn, rising leverage, or aggressive insider liquidation before traditional headlines report them.

### 4. Big Money Institutional Tracker (SEC 13F & Form 4)
- Tracks filings from tier-1 institutional funds (Berkshire Hathaway, Vanguard, BlackRock, Citadel, Renaissance Technologies).
- Reports net quarterly position changes, portfolio weighting, and Form 4 insider purchases vs. sales.

### 5. Gemini 2.5 Quantitative Reasoning Assistant
- Ask quantitative questions about balance sheets, capital allocation, moat sustainability, or intrinsic value ranges.
- Generate one-tap comprehensive **Institutional AI Investment Memorandums** with clear bull cases, bear risks, key metrics, and financial quality grades.

### 6. Offline-First & Sub-100ms Instant Startup
- Local SQLite Room persistence ensures the entire universe, scoring models, and baseline candlestick histories display instantaneously (<100ms) without waiting on remote server responses.
- Asynchronous concurrent provider health monitoring with fast failover.

---

## 🖥️ Screen-by-Screen Navigation

| Screen | Primary Function |
|---|---|
| **Dashboard** | Macro regime indicators (10Y Yield, Fed Funds, CPI, DXY), Top Ranked Compounders, and Active Deterioration warnings. |
| **Scanner** | Full universe filtering by score, sector, PEG ratio, FCF yield, and market cap. |
| **Research** | Interactive candlestick chart terminal, financial statement audit (Income Statement, Balance Sheet, Cash Flow), and AI Deep-Dive Memo generator. |
| **Portfolio** | Track owned positions, average buy prices, unrealized profit/loss, target prices, and watchlisted stocks. |
| **13F Flow** | Institutional ownership changes and SEC Form 4 insider trading logs. |
| **Health** | Real-time connection latency and status for all 7 market data and intelligence providers. |
| **AI Assist** | Interactive quantitative dialogue with Gemini 2.5 trained on balance sheets and corporate SEC filings. |

---

## 🔒 GitHub Push Troubleshooting: Fixing "Permission Denied" & Storing Your Project

If you see **"Failed to push commit to GitHub: permission denied"** in AI Studio, your code and all latest files are **100% safe in your cloud container**. This error is caused by GitHub access tokens or branch permissions.

Here are the 4 exact steps to fix it and store your work permanently:

### Step 1: Re-authenticate AI Studio with Full GitHub Permissions
1. In the AI Studio top bar or settings, click on your GitHub connection profile.
2. Select **Disconnect / Unlink GitHub**.
3. Re-link your GitHub account. When GitHub displays the OAuth authorization dialog, ensure the **"repo"** permission checkbox (Full control of private repositories) is enabled.
4. If your repository belongs to a **GitHub Organization**, click the **Grant** button next to your organization name in the authorization window.

### Step 2: Check Branch Protection Rules on GitHub
1. Open your repository on **[github.com](https://github.com)**.
2. Click **Settings** > **Branches** (in the left sidebar).
3. If there is a Branch Protection Rule on `main` or `master` (such as *"Require a pull request before merging"* or *"Restrict who can push to matching branches"*):
   - Click **Edit** on the rule.
   - Either add your user account to **"Allow specified actors to bypass required pull requests"**, or temporarily uncheck *"Do not allow bypassing the above settings"*.
   - Save changes and try pushing from AI Studio again.

### Step 3: Guaranteed Backup — Export as ZIP (Zero Risk of Data Loss)
Your project files are always safe and can be exported at any time:
1. In the AI Studio interface (top header or Project menu), click **Export** → **Export as ZIP** (or Download Project).
2. Save the ZIP file to your local drive or Google Drive.
3. Every single file, Kotlin class, image, and Gradle configuration is preserved.

### Step 4: Push Directly from Your Local Computer (Zero-Blocker Fallback)
If AI Studio's web OAuth continues to be blocked by GitHub branch rules, push directly via standard Git:
```bash
# 1. Unzip the downloaded project and open a terminal inside the directory:
cd <extracted-project-folder>

# 2. Initialize Git and stage all files:
git init
git add .
git commit -m "feat: complete institutional stock intelligence platform"

# 3. Rename branch to main and set your GitHub remote:
git branch -M main
git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPOSITORY_NAME>.git

# 4. Push your commits (use your Personal Access Token if prompted for password):
git push -u origin main --force
```

---

The application is pre-configured with active public market feeds (Yahoo Finance and FRED macro series) and verified offline models. To enable live AI Chat and secondary real-time feeds:

In **AI Studio Secrets** (or in a `.env` file at the root):

| Key | Description | Optional / Required |
|---|---|---|
| `GEMINI_API_KEY` | Google AI Studio key for Gemini 2.5 intelligence | Recommended for AI Assistant |
| `FINNHUB_API_KEY` | Secondary real-time stock quote & candle feed | Optional |
| `POLYGON_API_KEY` | Secondary market tick & previous close feed | Optional |
| `FMP_API_KEY` | Secondary historical financial statement feed | Optional |

*Note: If secondary keys are omitted, the app automatically routes all quotes and historical series through the built-in Yahoo Finance feed with zero interruption.*

---

## 🛠️ Building from Source

To compile and build the APK locally using the command line:

```bash
# Clone repository
git clone <your-github-repo-url>
cd <repo-folder>

# Grant execute permissions
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏛️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+ (100% Kotlin)
- **UI Toolkit**: Jetpack Compose with Material Design 3 (M3) Dark Theme
- **Architecture**: Clean Architecture / MVVM with Coroutines & StateFlow
- **Local Persistence**: Android Room Database (SQLite)
- **Networking**: Retrofit 2 + Moshi + OkHttp
- **AI Engine**: Google Generative AI (Gemini 2.5 Flash / Pro) via REST
- **CI/CD**: GitHub Actions Automated Build & Release Pipeline
