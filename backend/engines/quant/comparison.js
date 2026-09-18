// Cross-Stock Quantitative Comparison Engine

function buildQuantComparison(stockQuantResults = []) {
  return stockQuantResults.map(item => ({
    symbol: item.symbol,
    name: item.name || item.symbol,
    sector: item.sector || 'Technology',
    price: item.price,
    quantScore: item.quantScore,
    investmentScore: item.investmentScore,
    scoreSpread: item.investmentScore - item.quantScore,
    relationship: item.relationship.classification,
    horizon: item.horizon,
    regimeSummary: item.regime.summary,
    hurstExponent: item.regime.hurst,
    momentumZ: item.zScores.compositeZ.momentumZ,
    realizedVol: item.volatility.realizedVolatility,
    sharpeRatio: item.riskMetrics.sharpeRatio,
    maxDrawdown: item.riskMetrics.maxDrawdown,
    piotroski: item.piotroski.score,
    beneishRisk: item.beneish.isElevatedRisk ? 'Elevated' : 'Low',
    valuationZ: item.zScores.compositeZ.valuationZ,
    dcfFairValue: item.valuation.dcf.baseFairValue,
    dcfUpside: item.valuation.dcf.upsideDownsidePercent,
    institutionalFlowPercent: item.institutionalFlow.netFlowPercentOfShares
  }));
}

module.exports = { buildQuantComparison };
