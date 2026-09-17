export type DashboardData = {
  restaurant: {
    id: string;
    name: string;
    type: string;
    month: string;
  };
  metrics: {
    revenue: number;
    expenses: number;
    netProfit: number;
    profitMargin: number;
    foodCost: number;
    foodCostPercent: number;
    laborCost: number;
    laborCostPercent: number;
    primeCost: number;
    primeCostPercent: number;
    averageDailySales: number;
    revenueChangePercent: number;
    profitChangePercent: number;
  };
  moneyFlow: Array<{
    label: string;
    amount: number;
    percentOfRevenue: number;
    tone: string;
  }>;
  topVendors: Array<{
    name: string;
    category: string;
    amount: number;
    changePercent: number;
  }>;
  transactions: Array<{
    id: string;
    date: string;
    vendor: string;
    description: string;
    amount: number;
    category: string;
    confidence: "high" | "medium" | "low";
    direction: "credit" | "debit";
  }>;
  pnlLines: Array<{
    label: string;
    amount: number;
    kind: "revenue" | "cogs" | "expense";
    changePercent: number;
  }>;
  insights: Array<{
    title: string;
    detail: string;
    severity: "good" | "warning" | "alert";
  }>;
  questions: string[];
};

export type AuthSession = {
  token: string;
  user: {
    id: string;
    email: string;
    name: string;
  };
  restaurants: Array<{
    id: string;
    name: string;
    role: string;
  }>;
};
