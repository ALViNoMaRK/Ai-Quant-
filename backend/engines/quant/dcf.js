// Discounted Cash Flow (DCF / FCFF) Valuation Mathematics with Scenario & Sensitivity Analysis

function calculateDCF(stock) {
  const currentPrice = stock.price || 150.0;
  const shares = stock.sharesOutstanding || 2500000000;
  const netDebt = stock.netDebt || 5000000000; // in USD
  const baseRevenue = stock.revenue || 35000000000;
  const taxRate = 0.21;
  const rf = 0.0428; // 10-Year Treasury Yield
  const equityRiskPremium = 0.055; // Rm - Rf
  const beta = stock.beta || 1.15;
  const costOfEquity = rf + beta * equityRiskPremium;
  const costOfDebt = 0.052;
  const debtWeight = 0.12;
  const equityWeight = 0.88;
  const wacc = equityWeight * costOfEquity + debtWeight * costOfDebt * (1 - taxRate);

  // 3 Scenarios: Bear, Base, Bull
  const scenarios = {
    bear: {
      name: 'Bear Scenario',
      revGrowth: 0.08,
      operatingMargin: 0.24,
      taxRate: 0.21,
      wacc: wacc + 0.015,
      terminalGrowth: 0.02,
      capexRatio: 0.07,
      nwcRatio: 0.02
    },
    base: {
      name: 'Base Scenario',
      revGrowth: 0.14,
      operatingMargin: 0.32,
      taxRate: 0.21,
      wacc: wacc,
      terminalGrowth: 0.03,
      capexRatio: 0.06,
      nwcRatio: 0.015
    },
    bull: {
      name: 'Bull Scenario',
      revGrowth: 0.22,
      operatingMargin: 0.38,
      taxRate: 0.21,
      wacc: Math.max(0.065, wacc - 0.01),
      terminalGrowth: 0.035,
      capexRatio: 0.055,
      nwcRatio: 0.01
    }
  };

  function runScenario(sc) {
    let rev = baseRevenue;
    const cashFlows = [];
    let pvSum = 0;

    for (let t = 1; t <= 5; t++) {
      rev *= (1 + sc.revGrowth);
      const ebit = rev * sc.operatingMargin;
      const nopat = ebit * (1 - sc.taxRate);
      const da = rev * 0.05; // D&A estimate
      const capex = rev * sc.capexRatio;
      const deltaNwc = rev * sc.nwcRatio;
      const fcff = nopat + da - capex - deltaNwc;

      const df = Math.pow(1 + sc.wacc, t);
      const pv = fcff / df;
      pvSum += pv;
      cashFlows.push({ year: t, revenue: rev, fcff, pv });
    }

    const lastFcff = cashFlows[cashFlows.length - 1].fcff;
    const tv = (lastFcff * (1 + sc.terminalGrowth)) / (sc.wacc - sc.terminalGrowth);
    const pvTv = tv / Math.pow(1 + sc.wacc, 5);
    const enterpriseValue = pvSum + pvTv;
    const equityValue = enterpriseValue - netDebt;
    const fairValuePerShare = equityValue / shares;

    return {
      name: sc.name,
      fairValue: Math.round(fairValuePerShare * 100) / 100,
      enterpriseValue: Math.round(enterpriseValue),
      equityValue: Math.round(equityValue),
      wacc: Math.round(sc.wacc * 1000) / 10,
      terminalGrowth: Math.round(sc.terminalGrowth * 1000) / 10,
      revGrowth: Math.round(sc.revGrowth * 1000) / 10,
      operatingMargin: Math.round(sc.operatingMargin * 1000) / 10,
      cashFlows
    };
  }

  const bearResult = runScenario(scenarios.bear);
  const baseResult = runScenario(scenarios.base);
  const bullResult = runScenario(scenarios.bull);

  // WACC vs Terminal Growth Sensitivity Table for Base Scenario
  const waccList = [wacc - 0.01, wacc, wacc + 0.01];
  const growthList = [0.02, 0.03, 0.04];
  const sensitivityMatrix = [];

  for (const w of waccList) {
    const row = [];
    for (const g of growthList) {
      if (w <= g) {
        row.push(null);
        continue;
      }
      let rev = baseRevenue;
      let pvSum = 0;
      let lastFcff = 0;
      for (let t = 1; t <= 5; t++) {
        rev *= (1 + scenarios.base.revGrowth);
        const ebit = rev * scenarios.base.operatingMargin;
        const nopat = ebit * (1 - scenarios.base.taxRate);
        const da = rev * 0.05;
        const capex = rev * scenarios.base.capexRatio;
        const deltaNwc = rev * scenarios.base.nwcRatio;
        const fcff = nopat + da - capex - deltaNwc;
        lastFcff = fcff;
        pvSum += fcff / Math.pow(1 + w, t);
      }
      const tv = (lastFcff * (1 + g)) / (w - g);
      const pvTv = tv / Math.pow(1 + w, 5);
      const eqVal = (pvSum + pvTv) - netDebt;
      row.push(Math.round((eqVal / shares) * 100) / 100);
    }
    sensitivityMatrix.push({
      waccPercent: Math.round(w * 1000) / 10,
      values: row
    });
  }

  const upsideDownsidePercent = Math.round(((baseResult.fairValue - currentPrice) / currentPrice) * 1000) / 10;

  return {
    modelType: 'FCFF_DCF',
    currentPrice,
    baseFairValue: baseResult.fairValue,
    upsideDownsidePercent,
    wacc: Math.round(wacc * 1000) / 10,
    costOfEquity: Math.round(costOfEquity * 1000) / 10,
    costOfDebt: Math.round(costOfDebt * 1000) / 10,
    scenarios: {
      bear: bearResult,
      base: baseResult,
      bull: bullResult
    },
    sensitivityTable: {
      terminalGrowths: growthList.map(g => Math.round(g * 1000) / 10),
      matrix: sensitivityMatrix
    },
    assumptions: {
      forecastYears: 5,
      taxRate: Math.round(taxRate * 100),
      riskFreeRate: Math.round(rf * 1000) / 10,
      equityRiskPremium: Math.round(equityRiskPremium * 1000) / 10,
      beta: Math.round(beta * 100) / 100
    }
  };
}

module.exports = { calculateDCF };
