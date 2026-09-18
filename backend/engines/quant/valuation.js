// Valuation Mathematics Synthesis: DCF, Residual Income, and Multiples

const { calculateDCF } = require('./dcf');
const { calculateResidualIncome } = require('./residualIncome');

function calculateValuationSuite(stock) {
  const dcf = calculateDCF(stock);
  const residualIncome = calculateResidualIncome(stock);

  // Primary model recommendation
  const primaryModel = residualIncome.isApplicable ? 'RESIDUAL_INCOME_EBO' : 'FCFF_DCF';
  const primaryFairValue = primaryModel === 'RESIDUAL_INCOME_EBO' ? residualIncome.intrinsicPerShare : dcf.baseFairValue;
  const currentPrice = stock.price || 150.0;
  const intrinsicDiscountPercent = Math.round(((primaryFairValue - currentPrice) / currentPrice) * 1000) / 10;

  return {
    currentPrice,
    primaryModel,
    primaryFairValue,
    intrinsicDiscountPercent,
    dcf,
    residualIncome
  };
}

module.exports = { calculateValuationSuite };
