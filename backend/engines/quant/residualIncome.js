// Residual Income (Edwards-Bell-Ohlson) Valuation Mathematics & Applicability Engine

function calculateResidualIncome(stock) {
  const currentPrice = stock.price || 150.0;
  const shares = stock.sharesOutstanding || 2500000000;
  const sector = stock.sector || 'Technology';
  const roe = (stock.roe || 28.0) / 100;
  const costOfEquity = 0.095; // r = 9.5%
  const currentBookValuePerShare = stock.bookValuePerShare || (currentPrice / (stock.pb || 8.5));
  const currentTotalBV = currentBookValuePerShare * shares;

  // Applicability assessment:
  // Financial institutions (Banks, Insurance) where debt is raw material and FCFF is distorted,
  // or capital-intensive/negative FCF firms where book value & ROE are better anchored.
  const isFinancialSector = sector === 'Financial Services' || sector === 'Financials' || sector === 'Banking';
  const isNegativeFCF = (stock.fcf || 100) < 0;
  const isApplicable = isFinancialSector || isNegativeFCF;

  let applicabilityReason = '';
  if (isFinancialSector) {
    applicabilityReason = 'Highly Recommended: Target company is in Financial Services where debt functions as operating raw material and Free Cash Flow to Firm (FCFF) is economically distorted.';
  } else if (isNegativeFCF) {
    applicabilityReason = 'Recommended Alternative: Target company exhibits negative or volatile operating cash flow; Residual Income grounds valuation in balance sheet capital preservation.';
  } else {
    applicabilityReason = 'Secondary Reference Only: Target company generates robust positive Free Cash Flow to Firm. FCFF DCF remains the primary valuation standard.';
  }

  // 5-Year Residual Income Forecast
  let bv = currentTotalBV;
  let pvResidualSum = 0;
  const residualSchedule = [];

  const retentionRate = 0.70; // 30% dividend payout
  const growthInBV = roe * retentionRate;

  for (let t = 1; t <= 5; t++) {
    const priorBV = bv;
    const netIncome = priorBV * roe;
    const equityCharge = priorBV * costOfEquity;
    const residualIncome = netIncome - equityCharge;

    const discountFactor = Math.pow(1 + costOfEquity, t);
    const pvRI = residualIncome / discountFactor;
    pvResidualSum += pvRI;

    residualSchedule.push({
      year: t,
      bookValueBeginning: Math.round(priorBV),
      netIncome: Math.round(netIncome),
      equityCharge: Math.round(equityCharge),
      residualIncome: Math.round(residualIncome),
      pvResidualIncome: Math.round(pvRI)
    });

    bv += netIncome * retentionRate;
  }

  // Terminal Residual Value (persistence factor omega = 0.60)
  const lastRI = residualSchedule[residualSchedule.length - 1].residualIncome;
  const omega = 0.60;
  const terminalRI = (lastRI * omega) / (1 + costOfEquity - omega);
  const pvTerminalRI = terminalRI / Math.pow(1 + costOfEquity, 5);

  const intrinsicEquityValue = currentTotalBV + pvResidualSum + pvTerminalRI;
  const intrinsicPerShare = intrinsicEquityValue / shares;

  return {
    modelType: 'RESIDUAL_INCOME_EBO',
    isApplicable,
    applicabilityReason,
    currentPrice,
    intrinsicPerShare: Math.round(intrinsicPerShare * 100) / 100,
    bookValuePerShare: Math.round(currentBookValuePerShare * 100) / 100,
    costOfEquityPercent: Math.round(costOfEquity * 1000) / 10,
    roePercent: Math.round(roe * 1000) / 10,
    pvResidualIncome5Y: Math.round(pvResidualSum),
    pvTerminalResidualIncome: Math.round(pvTerminalRI),
    schedule: residualSchedule
  };
}

module.exports = { calculateResidualIncome };
