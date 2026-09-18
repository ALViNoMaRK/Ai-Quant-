// Quantitative Intelligence Score Composite Calibration & Contribution Engine (0 - 100)

function calculateQuantScore({
  returnsData,
  volatilityData,
  momentumData,
  regimeData,
  zScoreData,
  valuationData,
  riskData,
  factorData,
  piotroskiData,
  beneishData,
  institutionalData,
  horizon = '1Y'
}) {
  // Base weights dynamically adapted to selected horizon
  let w = {
    momentum: 0.18,
    riskAdjusted: 0.18,
    volatility: 0.12,
    relativeStrength: 0.12,
    regime: 0.10,
    valuation: 0.10,
    earningsQuality: 0.12,
    institutionalFlow: 0.08
  };

  if (horizon === '1D' || horizon === '1W') {
    w = {
      momentum: 0.28,
      riskAdjusted: 0.12,
      volatility: 0.22,
      relativeStrength: 0.18,
      regime: 0.12,
      valuation: 0.02,
      earningsQuality: 0.02,
      institutionalFlow: 0.04
    };
  } else if (horizon === '1M' || horizon === '3M') {
    w = {
      momentum: 0.22,
      riskAdjusted: 0.18,
      volatility: 0.15,
      relativeStrength: 0.15,
      regime: 0.12,
      valuation: 0.06,
      earningsQuality: 0.06,
      institutionalFlow: 0.06
    };
  } else if (horizon === '3Y' || horizon === '5Y') {
    w = {
      momentum: 0.10,
      riskAdjusted: 0.22,
      volatility: 0.10,
      relativeStrength: 0.10,
      regime: 0.10,
      valuation: 0.18,
      earningsQuality: 0.14,
      institutionalFlow: 0.06
    };
  }

  // 1. Momentum Score (0-100)
  // Evaluates ROC, Volatility-Adjusted Momentum, and Consistency
  let rawMom = 50;
  rawMom += Math.min(25, Math.max(-25, momentumData.roc252D * 0.5));
  rawMom += Math.min(15, Math.max(-15, momentumData.volAdjustedMomentum * 10));
  rawMom += (momentumData.momentumConsistency - 50) * 0.4;
  const momSubscore = Math.max(0, Math.min(100, rawMom));

  // 2. Risk-Adjusted Score (0-100)
  // Sharpe (benchmark ~1.0), Sortino, Calmar, Max Drawdown
  let rawRisk = 50;
  rawRisk += (riskData.sharpeRatio - 1.0) * 20;
  rawRisk += (riskData.sortinoRatio - 1.2) * 15;
  rawRisk += Math.min(15, Math.max(-25, (riskData.maxDrawdown + 15) * 1.5));
  const riskSubscore = Math.max(0, Math.min(100, rawRisk));

  // 3. Volatility Profile Score (0-100)
  // Lower / stable volatility rewarded unless extreme compression
  let rawVol = 50;
  if (volatilityData.volatilityRegime === 'COMPRESSING') rawVol += 25;
  else if (volatilityData.volatilityRegime === 'NORMAL') rawVol += 15;
  else if (volatilityData.volatilityRegime === 'EXPANDING') rawVol -= 15;
  else if (volatilityData.volatilityRegime === 'EXTREME_ELEVATED') rawVol -= 30;
  rawVol -= volatilityData.volatilityZScore * 10;
  const volSubscore = Math.max(0, Math.min(100, rawVol));

  // 4. Relative Strength / Cross-Sectional Z-Score (0-100)
  let rawRel = 50;
  rawRel += zScoreData.compositeZ.qualityZ * 12;
  rawRel += zScoreData.compositeZ.growthZ * 10;
  rawRel += momentumData.relativeMomentumBenchmark * 0.8;
  const relSubscore = Math.max(0, Math.min(100, rawRel));

  // 5. Statistical Regime & Trend Persistence (0-100)
  // Hurst Exponent: H > 0.55 trending, H < 0.45 mean reverting
  let rawRegime = 50;
  if (regimeData.regime === 'PERSISTENT_TRENDING') {
    rawRegime = 50 + (regimeData.hurst - 0.50) * 80;
  } else if (regimeData.regime === 'MEAN_REVERTING') {
    rawRegime = 50 - (0.50 - regimeData.hurst) * 60;
  }
  const regimeSubscore = Math.max(0, Math.min(100, rawRegime));

  // 6. Valuation Mathematics Score (0-100)
  // Intrinsic discount/premium vs DCF / Residual Income
  let rawVal = 50;
  rawVal += Math.min(30, Math.max(-30, valuationData.intrinsicDiscountPercent * 1.2));
  rawVal -= zScoreData.compositeZ.valuationZ * 8; // Higher valuation multiple reduces quant valuation score
  const valSubscore = Math.max(0, Math.min(100, rawVal));

  // 7. Earnings Quality & Manipulation Signals (0-100)
  // Piotroski (0-9) + Beneish M-Score (-1.78 threshold)
  let rawQuality = (piotroskiData.score / 9) * 80;
  if (!beneishData.isElevatedRisk) rawQuality += 20;
  else rawQuality -= 25;
  const qualitySubscore = Math.max(0, Math.min(100, rawQuality));

  // 8. Institutional Quantitative Flow (0-100)
  let rawFlow = 50;
  rawFlow += Math.min(25, Math.max(-25, institutionalData.netFlowPercentOfShares * 30));
  if (institutionalData.buyerToSellerRatio > 1.2) rawFlow += 15;
  else if (institutionalData.buyerToSellerRatio < 0.8) rawFlow -= 15;
  if (institutionalData.insiderFlow.sentiment === 'NET_ACCUMULATION') rawFlow += 10;
  const flowSubscore = Math.max(0, Math.min(100, rawFlow));

  // Component Contributions to the final score (each subscore * weight)
  const contributions = [
    { name: 'Momentum Mathematics', subscore: Math.round(momSubscore), weight: w.momentum, points: Math.round(momSubscore * w.momentum * 10) / 10 },
    { name: 'Risk-Adjusted Performance', subscore: Math.round(riskSubscore), weight: w.riskAdjusted, points: Math.round(riskSubscore * w.riskAdjusted * 10) / 10 },
    { name: 'Volatility Profile', subscore: Math.round(volSubscore), weight: w.volatility, points: Math.round(volSubscore * w.volatility * 10) / 10 },
    { name: 'Relative Strength & Z-Scores', subscore: Math.round(relSubscore), weight: w.relativeStrength, points: Math.round(relSubscore * w.relativeStrength * 10) / 10 },
    { name: 'Statistical Regime & Hurst', subscore: Math.round(regimeSubscore), weight: w.regime, points: Math.round(regimeSubscore * w.regime * 10) / 10 },
    { name: 'Valuation Discount (DCF/RI)', subscore: Math.round(valSubscore), weight: w.valuation, points: Math.round(valSubscore * w.valuation * 10) / 10 },
    { name: 'Earnings Quality (Piotroski/Beneish)', subscore: Math.round(qualitySubscore), weight: w.earningsQuality, points: Math.round(qualitySubscore * w.earningsQuality * 10) / 10 },
    { name: 'Institutional 13F & Insider Flow', subscore: Math.round(flowSubscore), weight: w.institutionalFlow, points: Math.round(flowSubscore * w.institutionalFlow * 10) / 10 }
  ];

  const compositeScore = contributions.reduce((sum, c) => sum + c.points, 0);
  const finalQuantScore = Math.max(1, Math.min(99, Math.round(compositeScore)));

  return {
    finalScore: finalQuantScore,
    horizon,
    weights: w,
    contributions,
    subscores: {
      momentum: Math.round(momSubscore),
      riskAdjusted: Math.round(riskSubscore),
      volatility: Math.round(volSubscore),
      relativeStrength: Math.round(relSubscore),
      regime: Math.round(regimeSubscore),
      valuation: Math.round(valSubscore),
      earningsQuality: Math.round(qualitySubscore),
      institutionalFlow: Math.round(flowSubscore)
    }
  };
}

module.exports = { calculateQuantScore };
