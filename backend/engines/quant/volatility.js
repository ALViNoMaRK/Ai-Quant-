// Realized volatility, rolling volatility, and historical percentile ranking

function calculateVolatility(prices, highPrices = null, lowPrices = null) {
  if (!prices || prices.length < 5) {
    return {
      realizedVolatility: 0.20,
      rollingVolatility20D: 0.20,
      volatilityPercentile: 50.0,
      volatilityRegime: 'NORMAL',
      volatilityZScore: 0.0
    };
  }

  // Calculate daily log returns
  const logReturns = [];
  for (let i = 1; i < prices.length; i++) {
    if (prices[i - 1] > 0 && prices[i] > 0) {
      logReturns.push(Math.log(prices[i] / prices[i - 1]));
    }
  }

  if (logReturns.length === 0) {
    return {
      realizedVolatility: 0.20,
      rollingVolatility20D: 0.20,
      volatilityPercentile: 50.0,
      volatilityRegime: 'NORMAL',
      volatilityZScore: 0.0
    };
  }

  // Realized volatility over the full sample (annualized)
  const meanRet = logReturns.reduce((a, b) => a + b, 0) / logReturns.length;
  let variance = 0;
  for (let i = 0; i < logReturns.length; i++) {
    variance += Math.pow(logReturns[i] - meanRet, 2);
  }
  const sampleStd = Math.sqrt(variance / Math.max(1, logReturns.length - 1));
  const realizedVolatility = sampleStd * Math.sqrt(252);

  // 20-day rolling volatilities across history to determine percentile
  const window = 20;
  const rollingVols = [];
  for (let i = window; i <= logReturns.length; i++) {
    const slice = logReturns.slice(i - window, i);
    const m = slice.reduce((a, b) => a + b, 0) / window;
    let v = 0;
    for (let j = 0; j < window; j++) {
      v += Math.pow(slice[j] - m, 2);
    }
    const s = Math.sqrt(v / (window - 1)) * Math.sqrt(252);
    rollingVols.push(s);
  }

  const currentRollingVol = rollingVols.length > 0 ? rollingVols[rollingVols.length - 1] : realizedVolatility;

  // Percentile ranking of current volatility relative to history
  let rank = 0;
  if (rollingVols.length > 0) {
    const belowCount = rollingVols.filter(v => v < currentRollingVol).length;
    rank = (belowCount / rollingVols.length) * 100;
  } else {
    rank = 50.0;
  }

  // Rolling Volatility Mean & Std for Z-Score
  let volMean = realizedVolatility;
  let volStd = 0.05;
  if (rollingVols.length > 1) {
    volMean = rollingVols.reduce((a, b) => a + b, 0) / rollingVols.length;
    let vVar = 0;
    for (let i = 0; i < rollingVols.length; i++) {
      vVar += Math.pow(rollingVols[i] - volMean, 2);
    }
    volStd = Math.max(0.01, Math.sqrt(vVar / (rollingVols.length - 1)));
  }
  const volatilityZScore = (currentRollingVol - volMean) / volStd;

  let volatilityRegime = 'NORMAL';
  if (currentRollingVol > 0.45 || rank > 85) volatilityRegime = 'EXTREME_ELEVATED';
  else if (currentRollingVol > 0.28 || rank > 65) volatilityRegime = 'EXPANDING';
  else if (currentRollingVol < 0.15 || rank < 25) volatilityRegime = 'COMPRESSING';

  return {
    realizedVolatility: Math.round(realizedVolatility * 10000) / 10000,
    rollingVolatility20D: Math.round(currentRollingVol * 10000) / 10000,
    volatilityPercentile: Math.round(rank * 10) / 10,
    volatilityRegime,
    volatilityZScore: Math.round(volatilityZScore * 100) / 100
  };
}

module.exports = { calculateVolatility };
