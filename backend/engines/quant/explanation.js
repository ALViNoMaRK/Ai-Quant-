// Transparent Mathematical Explanation & Score Relationship Engine

function generateQuantExplanation({
  quantResult,
  investmentScore = 85,
  momentumData,
  riskData,
  volatilityData,
  zScoreData,
  regimeData,
  valuationData,
  piotroskiData,
  beneishData,
  institutionalData
}) {
  const score = quantResult.finalScore;
  const positiveDrivers = [];
  const negativeDrivers = [];

  // 1. Momentum & Risk-Adjusted Evidence
  if (momentumData.volAdjustedMomentum > 0.8) {
    positiveDrivers.push({
      metric: 'Volatility-Adjusted Momentum',
      detail: `Strong risk-adjusted price momentum (${momentumData.volAdjustedMomentum.toFixed(2)}), with ${momentumData.roc252D >= 0 ? '+' : ''}${momentumData.roc252D.toFixed(1)}% 1-year rate of change and ${momentumData.momentumConsistency}% positive interval consistency.`
    });
  } else if (momentumData.volAdjustedMomentum < 0) {
    negativeDrivers.push({
      metric: 'Volatility-Adjusted Momentum',
      detail: `Negative risk-adjusted momentum (${momentumData.volAdjustedMomentum.toFixed(2)}) reflecting trailing price drawdown.`
    });
  }

  // 2. Risk Metrics: Sharpe & Max Drawdown
  if (riskData.sharpeRatio >= 1.2) {
    positiveDrivers.push({
      metric: 'Sharpe Ratio',
      detail: `Superior risk-adjusted return profile with annualized Sharpe of ${riskData.sharpeRatio.toFixed(2)} and Sortino of ${riskData.sortinoRatio.toFixed(2)} vs S&P 500 benchmark.`
    });
  } else if (riskData.sharpeRatio < 0.8) {
    negativeDrivers.push({
      metric: 'Sharpe Ratio',
      detail: `Subdued Sharpe ratio (${riskData.sharpeRatio.toFixed(2)}) relative to 4.28% risk-free rate hurdle.`
    });
  }

  if (Math.abs(riskData.maxDrawdown) > 20) {
    negativeDrivers.push({
      metric: 'Maximum Drawdown',
      detail: `Historical peak-to-trough drawdown reached ${riskData.maxDrawdown.toFixed(1)}%, requiring ${riskData.recoveryTimeDays} trading days of recovery.`
    });
  } else {
    positiveDrivers.push({
      metric: 'Downside Resilience',
      detail: `Controlled maximum drawdown of ${riskData.maxDrawdown.toFixed(1)}% preserves capital efficiency.`
    });
  }

  // 3. Hurst & Persistence
  if (regimeData.hurst > 0.55) {
    positiveDrivers.push({
      metric: 'Trend Persistence (Hurst Exponent)',
      detail: `Rescaled Range R/S analysis yields Hurst exponent of ${regimeData.hurst.toFixed(2)}, mathematically indicating persistent autocorrelation rather than mean-reverting decay.`
    });
  } else if (regimeData.hurst < 0.45) {
    negativeDrivers.push({
      metric: 'Mean-Reverting Regime (Hurst Exponent)',
      detail: `Hurst exponent of ${regimeData.hurst.toFixed(2)} indicates mean-reverting price dynamics, increasing whipsaw risk for trend systems.`
    });
  }

  // 4. Cross-Sectional Z-Scores
  if (zScoreData.compositeZ.qualityZ > 0.5) {
    positiveDrivers.push({
      metric: 'Cross-Sectional Quality Z-Score',
      detail: `Composite quality Z-score of +${zScoreData.compositeZ.qualityZ.toFixed(2)} vs sector peers, driven by ROE (${zScoreData.items.roe.stockValue}%) and ROIC (${zScoreData.items.roic.stockValue}%).`
    });
  }

  if (zScoreData.compositeZ.valuationZ > 0.8) {
    negativeDrivers.push({
      metric: 'Valuation Multiple Expansion Z-Score',
      detail: `P/E multiple sits +${zScoreData.items.pe.zScore.toFixed(2)} standard deviations above sector mean (${zScoreData.items.pe.stockValue}x vs peer mean ${zScoreData.items.pe.peerMean}x).`
    });
  } else if (zScoreData.compositeZ.valuationZ < -0.5) {
    positiveDrivers.push({
      metric: 'Valuation Multiple Discount Z-Score',
      detail: `Trading at a statistically significant valuation discount (${zScoreData.compositeZ.valuationZ.toFixed(2)}σ) relative to peer universe.`
    });
  }

  // 5. Piotroski & Beneish
  if (piotroskiData.score >= 7) {
    positiveDrivers.push({
      metric: 'Piotroski F-Score',
      detail: `Exceptional financial health score of ${piotroskiData.score}/9 with positive cash-to-income accrual test and healthy liquidity.`
    });
  } else if (piotroskiData.score <= 4) {
    negativeDrivers.push({
      metric: 'Piotroski F-Score',
      detail: `Sub-par Piotroski score of ${piotroskiData.score}/9 flags operational margin and balance sheet compression.`
    });
  }

  if (!beneishData.isElevatedRisk) {
    positiveDrivers.push({
      metric: 'Beneish M-Score',
      detail: `Low manipulation risk signal (${beneishData.mScore.toFixed(2)} vs -1.78 threshold) demonstrates clean accruals.`
    });
  } else {
    negativeDrivers.push({
      metric: 'Beneish M-Score',
      detail: `Elevated manipulation-risk signal (${beneishData.mScore.toFixed(2)} > -1.78 threshold) warrants cautious accounting audit.`
    });
  }

  // 6. Institutional Flow
  if (institutionalData.netFlowPercentOfShares > 0.2) {
    positiveDrivers.push({
      metric: 'Decayed 13F Institutional Net Flow',
      detail: `Institutional accumulation of +${institutionalData.netFlowPercentOfShares.toFixed(2)}% of outstanding float with exponential recency decay weighting.`
    });
  }

  // Concise Executive Mathematical Summary
  const topPos = positiveDrivers.slice(0, 2).map(d => d.detail).join(' ');
  const topNeg = negativeDrivers.slice(0, 1).map(d => d.detail).join(' ');
  const summaryExplanation = `The stock achieves a Quantitative Intelligence Score of ${score}/100 based on rigorous statistical evaluation. ${topPos} Conversely, ${topNeg || 'valuation multiples require sustained compounding to justify.'}`;

  // Evaluate Relationship between Investment Score and Quant Score
  const scoreDiff = investmentScore - score;
  let relationshipClassification = 'ALIGNED';
  let relationshipNarrative = '';

  if (Math.abs(scoreDiff) <= 10) {
    relationshipClassification = 'STRONG_CONVERGENCE';
    relationshipNarrative = `Both Fundamental Investment Intelligence (${investmentScore}) and Quantitative Intelligence (${score}) strongly converge, confirming robust business fundamentals underpinned by favorable market microstructure and statistical support.`;
  } else if (scoreDiff > 10) {
    relationshipClassification = 'FUNDAMENTAL_AHEAD_OF_QUANT';
    relationshipNarrative = `Divergence observed: Fundamental business quality remains exceptional (${investmentScore}), but quantitative market behavior (${score}) reflects short-term consolidation, elevated multiple dispersion, or recent volatility expansion. Ideal for patient accumulation.`;
  } else {
    relationshipClassification = 'QUANT_AHEAD_OF_FUNDAMENTALS';
    relationshipNarrative = `Divergence observed: Quantitative statistical momentum (${score}) is currently outperforming underlying accounting valuation metrics (${investmentScore}), indicating rapid technical price appreciation ahead of reported balance sheet fundamentals.`;
  }

  return {
    summaryExplanation,
    positiveDrivers,
    negativeDrivers,
    relationship: {
      investmentScore,
      quantScore: score,
      differential: scoreDiff,
      classification: relationshipClassification,
      narrative: relationshipNarrative
    }
  };
}

module.exports = { generateQuantExplanation };
