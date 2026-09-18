// Trend Persistence & Statistical Regime Engine (Hurst Exponent via Rescaled Range R/S analysis)

function calculateHurstExponent(prices) {
  if (!prices || prices.length < 20) {
    return {
      hurst: 0.52,
      regime: 'RANDOM_WALK',
      interpretation: 'Insufficient price history for robust R/S regression; default neutral random walk baseline used.',
      methodology: 'Rescaled Range (R/S) Analysis'
    };
  }

  // Calculate log returns
  const returns = [];
  for (let i = 1; i < prices.length; i++) {
    if (prices[i - 1] > 0 && prices[i] > 0) {
      returns.push(Math.log(prices[i] / prices[i - 1]));
    }
  }

  const n = returns.length;
  // Sub-period windows
  const lags = [8, 16, 32, 64, 128].filter(l => l <= n);
  if (lags.length < 2) {
    return {
      hurst: 0.51,
      regime: 'RANDOM_WALK',
      interpretation: 'Sample size too small to confirm fractal persistence.',
      methodology: 'Rescaled Range (R/S) Analysis'
    };
  }

  const logLags = [];
  const logRS = [];

  for (const lag of lags) {
    const numChunks = Math.floor(n / lag);
    let totalRS = 0;

    for (let c = 0; c < numChunks; c++) {
      const chunk = returns.slice(c * lag, (c + 1) * lag);
      const mean = chunk.reduce((a, b) => a + b, 0) / lag;

      // Cumulative deviations
      const y = [];
      let cum = 0;
      for (let i = 0; i < lag; i++) {
        cum += chunk[i] - mean;
        y.push(cum);
      }

      const r = Math.max(...y) - Math.min(...y);

      // Sample std
      let variance = 0;
      for (let i = 0; i < lag; i++) {
        variance += Math.pow(chunk[i] - mean, 2);
      }
      const s = Math.sqrt(variance / Math.max(1, lag - 1));

      if (s > 0) {
        totalRS += r / s;
      }
    }

    const avgRS = totalRS / numChunks;
    if (avgRS > 0) {
      logLags.push(Math.log(lag));
      logRS.push(Math.log(avgRS));
    }
  }

  // Linear regression of log(RS) on log(lags) to get Hurst exponent slope
  let hurst = 0.50;
  if (logLags.length >= 2) {
    const meanX = logLags.reduce((a, b) => a + b, 0) / logLags.length;
    const meanY = logRS.reduce((a, b) => a + b, 0) / logRS.length;

    let num = 0;
    let den = 0;
    for (let i = 0; i < logLags.length; i++) {
      num += (logLags[i] - meanX) * (logRS[i] - meanY);
      den += Math.pow(logLags[i] - meanX, 2);
    }
    if (den > 0) {
      hurst = Math.max(0.1, Math.min(0.95, num / den));
    }
  }

  let regime = 'RANDOM_WALK';
  let interpretation = '';

  if (hurst > 0.55) {
    regime = 'PERSISTENT_TRENDING';
    interpretation = `Price behavior has exhibited statistical persistence (Hurst = ${hurst.toFixed(2)}) over the selected lookback. Long memory processes indicate autocorrelated trend characteristics.`;
  } else if (hurst < 0.45) {
    regime = 'MEAN_REVERTING';
    interpretation = `Price behavior has exhibited statistical mean-reversion (Hurst = ${hurst.toFixed(2)}) over the selected lookback. Deviations from the rolling mean tend to decay.`;
  } else {
    regime = 'RANDOM_WALK';
    interpretation = `Price behavior exhibits approximate random walk dynamics (Hurst = ${hurst.toFixed(2)}). Historical increments show near-zero autocorrelation.`;
  }

  return {
    hurst: Math.round(hurst * 100) / 100,
    regime,
    interpretation,
    lookbackDays: prices.length,
    methodology: 'Rescaled Range (R/S) Fractal Analysis'
  };
}

function evaluateQuantRegime(hurstResult, volatilityResult, momentumResult, maxDrawdown) {
  const trend = hurstResult.regime === 'PERSISTENT_TRENDING' ? 'Persistent' :
    hurstResult.regime === 'MEAN_REVERTING' ? 'Mean-Reverting' : 'Random Walk';

  const momentum = momentumResult.roc252D > 15 ? 'Strong Positive' :
    momentumResult.roc252D > 0 ? 'Positive' :
    momentumResult.roc252D > -15 ? 'Consolidating' : 'Negative';

  const volatility = volatilityResult.volatilityRegime === 'COMPRESSING' ? 'Compressing' :
    volatilityResult.volatilityRegime === 'EXPANDING' ? 'Expanding' :
    volatilityResult.volatilityRegime === 'EXTREME_ELEVATED' ? 'Extreme' : 'Normal';

  const relativeStrength = momentumResult.relativeMomentumBenchmark > 10 ? 'Strong Outperformer' :
    momentumResult.relativeMomentumBenchmark > 0 ? 'Moderate Outperformer' :
    momentumResult.relativeMomentumBenchmark > -10 ? 'Market Neutral' : 'Underperformer';

  const drawdown = Math.abs(maxDrawdown) < 0.10 ? 'Minimal' :
    Math.abs(maxDrawdown) < 0.25 ? 'Moderate' : 'Elevated';

  const statisticalPersistence = hurstResult.hurst > 0.60 ? 'High' :
    hurstResult.hurst > 0.50 ? 'Moderate' : 'Low';

  const summary = `${trend} Trend / ${volatility} Volatility`;

  return {
    trend,
    momentum,
    volatility,
    relativeStrength,
    correlation: 'Moderate',
    drawdown,
    statisticalPersistence,
    liquidity: 'Ample Tier-1',
    summary
  };
}

module.exports = { calculateHurstExponent, evaluateQuantRegime };
