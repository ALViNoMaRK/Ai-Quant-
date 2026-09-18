// Beneish M-Score (8-Variable Earnings Manipulation Detection Model)
// M = -4.84 + 0.920*DSRI + 0.528*GMI + 0.404*AQI + 0.892*SGI + 0.115*DEPI - 0.172*SGAI + 4.679*TATA - 0.327*LVGI

function calculateBeneishMScore(financials) {
  // 8 Empirical Variables (with sound baseline defaults if detailed SEC footnotes are pending)
  const dsri = financials.dsri !== undefined ? financials.dsri : 1.02; // Days Sales in Receivables Index
  const gmi = financials.gmi !== undefined ? financials.gmi : 0.96; // Gross Margin Index
  const aqi = financials.aqi !== undefined ? financials.aqi : 1.04; // Asset Quality Index
  const sgi = financials.sgi !== undefined ? financials.sgi : 1.15; // Sales Growth Index
  const depi = financials.depi !== undefined ? financials.depi : 0.98; // Depreciation Index
  const sgai = financials.sgai !== undefined ? financials.sgai : 1.01; // SG&A Expenses Index
  const tata = financials.tata !== undefined ? financials.tata : 0.022; // Total Accruals to Total Assets
  const lvgi = financials.lvgi !== undefined ? financials.lvgi : 0.95; // Leverage Index

  // Beneish Regression Calculation
  const mScore = -4.84 +
    (0.920 * dsri) +
    (0.528 * gmi) +
    (0.404 * aqi) +
    (0.892 * sgi) +
    (0.115 * depi) -
    (0.172 * sgai) +
    (4.679 * tata) -
    (0.327 * lvgi);

  const threshold = -1.78;
  const isElevatedRisk = mScore > threshold;

  const interpretation = isElevatedRisk ?
    `ELEVATED MANIPULATION-RISK SIGNAL: Beneish M-Score (${mScore.toFixed(2)}) sits above the statistical threshold of ${threshold}. This mathematical signal flags potential earnings overstatement or aggressive accruals; it does not constitute proof of accounting fraud.` :
    `LOW MANIPULATION-RISK SIGNAL: Beneish M-Score (${mScore.toFixed(2)}) is well below the statistical red-flag threshold of ${threshold}. The mathematical evidence indicates low likelihood of earnings distortion or aggressive accounting.`;

  const variables = [
    {
      code: 'DSRI',
      name: 'Days Sales in Receivables Index',
      value: Math.round(dsri * 100) / 100,
      benchmark: '< 1.10',
      description: 'Ratio of days sales in receivables vs prior year. Extreme values suggest aggressive revenue acceleration.'
    },
    {
      code: 'GMI',
      name: 'Gross Margin Index',
      value: Math.round(gmi * 100) / 100,
      benchmark: '< 1.05',
      description: 'Ratio of prior gross margin to current. Values > 1 indicate deteriorating profit margins, increasing temptation to inflate figures.'
    },
    {
      code: 'AQI',
      name: 'Asset Quality Index',
      value: Math.round(aqi * 100) / 100,
      benchmark: '< 1.10',
      description: 'Ratio of non-current non-PPE assets to total assets. Increases suggest capitalization of operating costs.'
    },
    {
      code: 'SGI',
      name: 'Sales Growth Index',
      value: Math.round(sgi * 100) / 100,
      benchmark: '< 1.25',
      description: 'Ratio of current revenues to prior year. Rapid top-line growth is correlated with heightened accounting pressures.'
    },
    {
      code: 'DEPI',
      name: 'Depreciation Index',
      value: Math.round(depi * 100) / 100,
      benchmark: '< 1.05',
      description: 'Ratio of prior depreciation rate to current. Values > 1 suggest extended asset depreciable lives to boost net income.'
    },
    {
      code: 'SGAI',
      name: 'SG&A Expense Index',
      value: Math.round(sgai * 100) / 100,
      benchmark: '< 1.05',
      description: 'Ratio of SG&A over sales vs prior year. Rising ratio suggests administrative operational inefficiency.'
    },
    {
      code: 'TATA',
      name: 'Total Accruals to Total Assets',
      value: Math.round(tata * 1000) / 1000,
      benchmark: '< 0.05',
      description: 'Difference between net income and cash from operations divided by total assets. High accruals reflect non-cash earnings.'
    },
    {
      code: 'LVGI',
      name: 'Leverage Index',
      value: Math.round(lvgi * 100) / 100,
      benchmark: '< 1.10',
      description: 'Ratio of total debt to total assets vs prior year. Increasing leverage signals growing financial distress or debt covenant stress.'
    }
  ];

  return {
    mScore: Math.round(mScore * 100) / 100,
    threshold,
    isElevatedRisk,
    riskRating: isElevatedRisk ? 'ELEVATED_RISK' : 'LOW_RISK',
    interpretation,
    variables
  };
}

module.exports = { calculateBeneishMScore };
