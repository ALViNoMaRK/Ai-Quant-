# Institutional Stock Intelligence Platform

An institutional-grade quantitative equity research, fundamental valuation, and technical analysis platform built with **Kotlin**, **Jetpack Compose (Material 3)**, **SQLite Room**, and powered by **Google Gemini 2.5**.

Designed for institutional investors, quantitative analysts, and serious compounders who demand sub-second financial statement audits, real-time market candlestick charting, SEC Form 13F / Form 4 flow tracking, and multi-factor valuation scoring.

---

## 📥 How to Download & Install the Android APK

### Option 1: Direct Download from GitHub Actions (Recommended)

Whenever code is pushed to this repository, a GitHub Action automatically builds the latest APK:

1. Go to the **[Actions](../../actions)** tab in this GitHub repository.
2. Click on the latest workflow run (e.g., **"Build & Release Android APK"**).
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Click on **`stock-intel-debug-apk`** to download the ZIP package containing the APK.
5. Extract the ZIP to obtain `app-debug.apk`.

### Option 2: Download from GitHub Releases

1. Go to the **[Releases](../../releases)** page of this GitHub repository.
2. Under **Assets**, click on **`app-debug.apk`** to download it directly onto your Android device.

### Option 3: Download from AI Studio

1. In the **Files / Code** explorer tab, locate `app-debug.apk` in the root workspace.
2. Tap the file or the options menu (`⋮`) and select **Download**.
3. Or use the top bar menu: **Export > Generate / Download APK**.

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

### 3. Opportunity Scanner & Thesis Deterioration Detector
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

## 🔑 API Keys & Environment Configuration

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
