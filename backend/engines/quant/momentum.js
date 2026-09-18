// Momentum Mathematics: Rate of Change (ROC), Volatility-Adjusted Momentum, Consistency

function calculateMomentum(prices, realizedVolatility, benchmarkPrices = null) {
  if (!prices || prices.length === 0) {
    return {
      roc5D: 0,
      roc20D: 0,
      roc63D: 0,
      roc126D: 0,
      roc252D: 0,
      volAdjustedMomentum: 0,
      momentumConsistency: 50,
      relativeMomentumBenchmark: 0,
      momentumZScore: 0
    };
  }

  const n = prices.length;
  const currentPrice = prices[n - 1];

  function calcRoc(period) {
    if (n <= period) {
      const oldest = prices[0];
      return oldest > 0 ? ((currentPrice - oldest) / oldest) * 100 : 0;
    }
    const pastPrice = prices[n - 1 - period];
    return pastPrice > 0 ? ((currentPrice - pastPrice) / pastPrice) * 100 : 0;
  }

  const roc5D = calcRoc(5);
  const roc20D = calcRoc(20);
  const roc63D = calcRoc(63);
  const roc126D = calcRoc(126);
  const roc252D = calcRoc(252);

  // Volatility-Adjusted Momentum (1Y or available period)
  // Stock producing 20% return with 10% vol is vastly superior to 20% with 60% vol
  const volForAdj = Math.max(0.05, realizedVolatility || 0.20);
  // Annualized return equivalent for vol adjustment
  const annualRet = roc252D / 100;
  const volAdjustedMomentum = annualRet / volForAdj;

  // Momentum Consistency: percentage of positive 5-day rolling segments
  let positiveSegments = 0;
  let totalSegments = 0;
  for (let i = 5; i < n; i += 5) {
    totalSegments++;
    if (prices[i] >= prices[i - 5]) {
      positiveSegments++;
    }
  }
  const momentumConsistency = totalSegments > 0 ? Math.round((positiveSegments / totalSegments) * 100) : 50;

  // Relative Momentum vs Benchmark (e.g. SPY)
  let relativeMomentumBenchmark = 0;
  if (benchmarkPrices && benchmarkPrices.length > 0) {
    const bmLen = benchmarkPrices.length;
    const bmPast = benchmarkPrices[0];
    const bmCurr = benchmarkPrices[bmLen - 1];
    const bmRoc = bmPast > 0 ? ((bmCurr - bmPast) / bmPast) * 100 : 0;
    relativeMomentumBenchmark = roc252D - bmRoc;
  } else {
    // Default benchmark baseline (approx 12% SPY 1Y)
    relativeMomentumBenchmark = roc252D - 12.5;
  }

  // Momentum Z-Score (vs theoretical market distribution: mean ~8%, std ~22%)
  const momentumZScore = (roc252D - 8.0) / 22.0;

  return {
    roc5D: Math.round(roc5D * 100) / 100,
    roc20D: Math.round(roc20D * 100) / 100,
    roc63D: Math.round(roc63D * 100) / 100,
    roc126D: Math.round(roc126D * 100) / 100,
    roc252D: Math.round(roc252D * 100) / 100,
    volAdjustedMomentum: Math.round(volAdjustedMomentum * 100) / 100,
    momentumConsistency,
    relativeMomentumBenchmark: Math.round(relativeMomentumBenchmark * 100) / 100,
    momentumZScore: Math.round(momentumZScore * 100) / 100
  };
}

module.exports = { calculateMomentum };
