// Logarithmic, simple, and rolling returns, distributions, skewness, kurtosis, and downside deviation

function calculateReturns(prices) {
  if (!prices || prices.length < 2) {
    return {
      simpleReturns: [],
      logReturns: [],
      meanLogReturn: 0,
      meanSimpleReturn: 0,
      variance: 0,
      stdDev: 0,
      skewness: 0,
      kurtosis: 0,
      downsideDeviation: 0,
      priceZScore: 0
    };
  }

  const simpleReturns = [];
  const logReturns = [];
  for (let i = 1; i < prices.length; i++) {
    const prev = prices[i - 1];
    const curr = prices[i];
    if (prev > 0 && curr > 0) {
      simpleReturns.push((curr - prev) / prev);
      logReturns.push(Math.log(curr / prev));
    }
  }

  const n = logReturns.length;
  if (n === 0) {
    return {
      simpleReturns: [],
      logReturns: [],
      meanLogReturn: 0,
      meanSimpleReturn: 0,
      variance: 0,
      stdDev: 0,
      skewness: 0,
      kurtosis: 0,
      downsideDeviation: 0,
      priceZScore: 0
    };
  }

  const sumLog = logReturns.reduce((acc, r) => acc + r, 0);
  const meanLogReturn = sumLog / n;

  const sumSimple = simpleReturns.reduce((acc, r) => acc + r, 0);
  const meanSimpleReturn = sumSimple / n;

  // Sample variance and standard deviation
  let variance = 0;
  for (let i = 0; i < n; i++) {
    variance += Math.pow(logReturns[i] - meanLogReturn, 2);
  }
  variance = n > 1 ? variance / (n - 1) : 0;
  const stdDev = Math.sqrt(variance);

  // Skewness: Fisher-Pearson standardized moment coefficient
  let skewness = 0;
  if (n > 2 && stdDev > 0) {
    let m3 = 0;
    for (let i = 0; i < n; i++) {
      m3 += Math.pow((logReturns[i] - meanLogReturn) / stdDev, 3);
    }
    skewness = (n / ((n - 1) * (n - 2))) * m3;
  }

  // Excess Kurtosis
  let kurtosis = 0;
  if (n > 3 && stdDev > 0) {
    let m4 = 0;
    for (let i = 0; i < n; i++) {
      m4 += Math.pow((logReturns[i] - meanLogReturn) / stdDev, 4);
    }
    const c1 = (n * (n + 1)) / ((n - 1) * (n - 2) * (n - 3));
    const c2 = (3 * Math.pow(n - 1, 2)) / ((n - 2) * (n - 3));
    kurtosis = c1 * m4 - c2;
  }

  // Downside Deviation (relative to 0 or risk-free rate threshold)
  let sumDownsideSq = 0;
  for (let i = 0; i < n; i++) {
    const diff = logReturns[i] - 0;
    if (diff < 0) {
      sumDownsideSq += Math.pow(diff, 2);
    }
  }
  const downsideDeviation = Math.sqrt(sumDownsideSq / n) * Math.sqrt(252);

  // Price Z-Score relative to its recent window mean
  const priceSum = prices.reduce((a, b) => a + b, 0);
  const priceMean = priceSum / prices.length;
  let priceVar = 0;
  for (let i = 0; i < prices.length; i++) {
    priceVar += Math.pow(prices[i] - priceMean, 2);
  }
  const priceStd = prices.length > 1 ? Math.sqrt(priceVar / (prices.length - 1)) : 1;
  const currentPrice = prices[prices.length - 1];
  const priceZScore = priceStd > 0 ? (currentPrice - priceMean) / priceStd : 0;

  return {
    simpleReturns,
    logReturns,
    meanLogReturn,
    meanSimpleReturn,
    variance,
    stdDev,
    skewness,
    kurtosis,
    downsideDeviation,
    priceZScore
  };
}

module.exports = { calculateReturns };
