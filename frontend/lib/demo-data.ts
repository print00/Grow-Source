import type { DashboardData } from "./types";

export const demoDashboard: DashboardData = {
  restaurant: {
    id: "demo",
    name: "Harbor Spoon Cafe",
    type: "Casual dining",
    month: "September 2026"
  },
  metrics: {
    revenue: 82450,
    expenses: 68170,
    netProfit: 14280,
    profitMargin: 17.32,
    foodCost: 25190,
    foodCostPercent: 30.55,
    laborCost: 22840,
    laborCostPercent: 27.7,
    primeCost: 48030,
    primeCostPercent: 58.26,
    averageDailySales: 2748.33,
    revenueChangePercent: 8.2,
    profitChangePercent: -4.1
  },
  moneyFlow: [
    { label: "Profit", amount: 14280, percentOfRevenue: 17.32, tone: "profit" },
    { label: "Food & beverage", amount: 25190, percentOfRevenue: 30.55, tone: "food" },
    { label: "Labor", amount: 22840, percentOfRevenue: 27.7, tone: "labor" },
    { label: "Rent", amount: 7500, percentOfRevenue: 9.1, tone: "rent" },
    { label: "Delivery fees", amount: 4120, percentOfRevenue: 5.0, tone: "delivery" },
    { label: "Other", amount: 8510, percentOfRevenue: 10.33, tone: "other" }
  ],
  topVendors: [
    { name: "Sysco", category: "Food & beverage", amount: 9120, changePercent: 31.5 },
    { name: "Restaurant Depot", category: "Food & beverage", amount: 6280, changePercent: 12.1 },
    { name: "Toast Payroll", category: "Labor", amount: 22840, changePercent: 6.8 },
    { name: "DoorDash", category: "Delivery fees", amount: 2740, changePercent: 18.6 },
    { name: "Main Street Properties", category: "Rent", amount: 7500, changePercent: 0 }
  ],
  transactions: [
    { id: "t1", date: "2026-09-02", vendor: "Toast POS Deposit", description: "Card sales deposit", amount: 18420, category: "Revenue", confidence: "high", direction: "credit" },
    { id: "t2", date: "2026-09-03", vendor: "Sysco", description: "Weekly produce and proteins", amount: -5210, category: "Food & beverage", confidence: "high", direction: "debit" },
    { id: "t3", date: "2026-09-04", vendor: "Restaurant Depot", description: "Dry goods and supplies", amount: -3180, category: "Food & beverage", confidence: "high", direction: "debit" },
    { id: "t4", date: "2026-09-05", vendor: "Toast Payroll", description: "Biweekly payroll", amount: -11240, category: "Labor", confidence: "high", direction: "debit" },
    { id: "t5", date: "2026-09-09", vendor: "DoorDash", description: "Marketplace commission", amount: -1390, category: "Delivery fees", confidence: "medium", direction: "debit" },
    { id: "t6", date: "2026-09-12", vendor: "Beltway Utility", description: "Gas and electric", amount: -1180, category: "Utilities", confidence: "high", direction: "debit" },
    { id: "t7", date: "2026-09-16", vendor: "QuickSupply Co", description: "Unclear restaurant supplies", amount: -740, category: "Other operating", confidence: "low", direction: "debit" },
    { id: "t8", date: "2026-09-19", vendor: "Toast POS Deposit", description: "Card sales deposit", amount: 19870, category: "Revenue", confidence: "high", direction: "credit" }
  ],
  pnlLines: [
    { label: "Sales", amount: 82450, kind: "revenue", changePercent: 8.2 },
    { label: "Food & beverage", amount: -25190, kind: "cogs", changePercent: 18.3 },
    { label: "Labor", amount: -22840, kind: "expense", changePercent: 6.8 },
    { label: "Rent", amount: -7500, kind: "expense", changePercent: 0 },
    { label: "Delivery fees", amount: -4120, kind: "expense", changePercent: 18.6 },
    { label: "Utilities", amount: -2180, kind: "expense", changePercent: 9.4 },
    { label: "Other operating", amount: -6340, kind: "expense", changePercent: -3.1 }
  ],
  insights: [
    {
      title: "Sales rose, but profit slipped",
      detail: "Revenue increased 8.2% from August, while net profit fell 4.1% because food and delivery costs grew faster than sales.",
      severity: "warning"
    },
    {
      title: "Food cost is above target",
      detail: "Food cost is 30.6% of sales. Your target is 28%, so September ran about $2,100 above target.",
      severity: "alert"
    },
    {
      title: "Prime cost is still workable",
      detail: "Prime cost is 58.3% of sales. Many restaurants aim to keep food plus labor under 60%.",
      severity: "good"
    },
    {
      title: "Sysco spend changed unusually",
      detail: "Sysco spending increased 31.5% month over month, mostly from produce and protein purchases.",
      severity: "warning"
    }
  ],
  questions: [
    "Why did profit fall if sales were up?",
    "Which costs should I look at first?",
    "Is my labor cost healthy?",
    "What changed with food vendors?"
  ]
};
