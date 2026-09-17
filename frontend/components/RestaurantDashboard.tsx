"use client";

import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Banknote,
  BarChart3,
  Bot,
  CheckCircle2,
  ClipboardList,
  FileSpreadsheet,
  Gauge,
  HelpCircle,
  Home,
  LogOut,
  Search,
  Upload
} from "lucide-react";
import { useMemo, useState } from "react";
import type { DashboardData } from "../lib/types";

type TabId = "dashboard" | "transactions" | "pnl" | "insights" | "ask";

const tabs: Array<{ id: TabId; label: string; icon: React.ComponentType<{ size?: number }> }> = [
  { id: "dashboard", label: "Dashboard", icon: Home },
  { id: "transactions", label: "Transactions", icon: ClipboardList },
  { id: "pnl", label: "P&L", icon: FileSpreadsheet },
  { id: "insights", label: "Insights", icon: Gauge },
  { id: "ask", label: "Ask", icon: Bot }
];

const money = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0
});

const compactMoney = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  notation: "compact",
  maximumFractionDigits: 1
});

function formatChange(value: number) {
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(1)}%`;
}

function MetricCard({
  label,
  value,
  detail,
  trend,
  strong
}: {
  label: string;
  value: string;
  detail: string;
  trend?: number;
  strong?: boolean;
}) {
  return (
    <section className={strong ? "metric metricStrong" : "metric"}>
      <div className="metricLabel">{label}</div>
      <div className="metricValue">{value}</div>
      <div className="metricFoot">
        <span>{detail}</span>
        {trend !== undefined ? (
          <span className={trend >= 0 ? "trendUp" : "trendDown"}>
            {trend >= 0 ? <ArrowUpRight size={15} /> : <ArrowDownRight size={15} />}
            {formatChange(trend)}
          </span>
        ) : null}
      </div>
    </section>
  );
}

function OwnerFlow() {
  const steps = ["Restaurant", "CSV upload", "Categorize", "Owner review", "P&L ready"];

  return (
    <div className="ownerFlow" aria-label="MVP progress">
      {steps.map((step, index) => (
        <div className="flowStep" key={step}>
          <span>{index + 1}</span>
          <p>{step}</p>
        </div>
      ))}
    </div>
  );
}

function Dashboard({ data }: { data: DashboardData }) {
  const maxFlow = Math.max(...data.moneyFlow.map((item) => item.amount));

  return (
    <div className="screen">
      <div className="heroBand">
        <div>
          <p className="eyebrow">{data.restaurant.name} · {data.restaurant.month}</p>
          <h1>Money kept, costs watched.</h1>
          <p className="heroCopy">
            After food, labor, rent, delivery fees, utilities, and operating costs, the restaurant kept{" "}
            {money.format(data.metrics.netProfit)}.
          </p>
        </div>
        <div className="profitBadge">
          <span>Net profit</span>
          <strong>{money.format(data.metrics.netProfit)}</strong>
          <small>{data.metrics.profitMargin.toFixed(1)}% margin</small>
        </div>
      </div>

      <OwnerFlow />

      <div className="metricGrid">
        <MetricCard
          label="Sales"
          value={money.format(data.metrics.revenue)}
          detail="Total restaurant revenue"
          trend={data.metrics.revenueChangePercent}
          strong
        />
        <MetricCard
          label="Food cost"
          value={`${data.metrics.foodCostPercent.toFixed(1)}%`}
          detail={money.format(data.metrics.foodCost)}
        />
        <MetricCard
          label="Labor cost"
          value={`${data.metrics.laborCostPercent.toFixed(1)}%`}
          detail={money.format(data.metrics.laborCost)}
        />
        <MetricCard
          label="Prime cost"
          value={`${data.metrics.primeCostPercent.toFixed(1)}%`}
          detail={money.format(data.metrics.primeCost)}
        />
      </div>

      <div className="twoColumn">
        <section className="panel">
          <div className="panelHeader">
            <div>
              <p className="eyebrow">Every $1 of sales</p>
              <h2>Money flow</h2>
            </div>
            <Banknote size={22} />
          </div>
          <div className="flowBars">
            {data.moneyFlow.map((item) => (
              <div className="barRow" key={item.label}>
                <div className="barLabel">
                  <span>{item.label}</span>
                  <strong>{item.percentOfRevenue.toFixed(1)}%</strong>
                </div>
                <div className="barTrack">
                  <span
                    className={`barFill ${item.tone}`}
                    style={{ width: `${Math.max(7, (item.amount / maxFlow) * 100)}%` }}
                  />
                </div>
                <small>{compactMoney.format(item.amount)}</small>
              </div>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panelHeader">
            <div>
              <p className="eyebrow">Watch first</p>
              <h2>Top vendors</h2>
            </div>
            <BarChart3 size={22} />
          </div>
          <div className="vendorList">
            {data.topVendors.map((vendor) => (
              <div className="vendorRow" key={vendor.name}>
                <div>
                  <strong>{vendor.name}</strong>
                  <span>{vendor.category}</span>
                </div>
                <div>
                  <strong>{money.format(vendor.amount)}</strong>
                  <span className={vendor.changePercent > 15 ? "watch" : ""}>
                    {formatChange(vendor.changePercent)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function Transactions({
  data,
  onImportCsv
}: {
  data: DashboardData;
  onImportCsv: (input: {
    accountType: "BANK" | "CREDIT_CARD";
    signMode: "AUTO" | "MONEY_IN_POSITIVE" | "MONEY_OUT_POSITIVE";
    csv: string;
  }) => Promise<void>;
}) {
  const [selected, setSelected] = useState("all");
  const [accountType, setAccountType] = useState<"BANK" | "CREDIT_CARD">("BANK");
  const [signMode, setSignMode] = useState<"AUTO" | "MONEY_IN_POSITIVE" | "MONEY_OUT_POSITIVE">("AUTO");
  const [importStatus, setImportStatus] = useState("");
  const [importing, setImporting] = useState(false);
  const questionable = data.transactions.filter((transaction) => transaction.confidence !== "high");
  const filtered = selected === "review" ? questionable : data.transactions;

  async function handleFileSelected(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    setImporting(true);
    setImportStatus("");
    try {
      await onImportCsv({
        accountType,
        signMode,
        csv: await file.text()
      });
      setImportStatus("CSV imported and normalized. Review any low-confidence categories below.");
    } catch {
      setImportStatus("Could not import that CSV. Check the columns and try again.");
    } finally {
      setImporting(false);
      event.target.value = "";
    }
  }

  return (
    <div className="screen">
      <div className="sectionTitle">
        <div>
          <p className="eyebrow">CSV to clean books</p>
          <h1>Transactions</h1>
        </div>
        <label className="primaryButton uploadButton">
          <Upload size={18} />
          {importing ? "Importing..." : "Upload CSV"}
          <input accept=".csv,text/csv" type="file" onChange={handleFileSelected} disabled={importing} />
        </label>
      </div>

      <section className="importPanel">
        <div>
          <span>Card or account type</span>
          <select
            value={accountType}
            onChange={(event) => setAccountType(event.target.value as "BANK" | "CREDIT_CARD")}
            aria-label="CSV account type"
          >
            <option value="BANK">Bank / debit card</option>
            <option value="CREDIT_CARD">Credit card</option>
          </select>
        </div>
        <div>
          <span>Amount signs in this CSV</span>
          <select
            value={signMode}
            onChange={(event) =>
              setSignMode(event.target.value as "AUTO" | "MONEY_IN_POSITIVE" | "MONEY_OUT_POSITIVE")
            }
            aria-label="CSV sign format"
          >
            <option value="AUTO">Auto: bank + is in, credit card + is out</option>
            <option value="MONEY_IN_POSITIVE">Positive means money in</option>
            <option value="MONEY_OUT_POSITIVE">Positive means money out</option>
          </select>
        </div>
        <p>
          Signed Amount and Debit/Credit columns are both supported. Amounts are normalized before P&L.
        </p>
      </section>

      {importStatus ? <div className="importStatus">{importStatus}</div> : null}

      <div className="reviewStrip">
        <AlertTriangle size={22} />
        <div>
          <strong>{questionable.length} transactions need a quick check</strong>
          <span>Confirm these so the P&L stays accurate before the month is closed.</span>
        </div>
      </div>

      <div className="segmented">
        <button className={selected === "all" ? "active" : ""} onClick={() => setSelected("all")} type="button">
          All transactions
        </button>
        <button className={selected === "review" ? "active" : ""} onClick={() => setSelected("review")} type="button">
          Needs review
        </button>
      </div>

      <section className="tablePanel">
        <div className="transactionHeader">
          <span>Date</span>
          <span>Vendor</span>
          <span>Category</span>
          <span>Amount</span>
          <span>Status</span>
        </div>
        {filtered.map((transaction) => (
          <div className="transactionRow" key={transaction.id}>
            <span>{transaction.date.slice(5)}</span>
            <div>
              <strong>{transaction.vendor}</strong>
              <small>{transaction.description}</small>
            </div>
            <select defaultValue={transaction.category} aria-label={`Category for ${transaction.vendor}`}>
              <option>Revenue</option>
              <option>Food & beverage</option>
              <option>Labor</option>
              <option>Rent</option>
              <option>Delivery fees</option>
              <option>Utilities</option>
              <option>Other operating</option>
            </select>
            <strong className={transaction.direction === "credit" ? "credit" : "debit"}>
              {money.format(transaction.amount)}
            </strong>
            <span className={`confidence ${transaction.confidence}`}>
              {transaction.confidence === "high" ? <CheckCircle2 size={16} /> : <HelpCircle size={16} />}
              {transaction.confidence}
            </span>
          </div>
        ))}
      </section>
    </div>
  );
}

function ProfitAndLoss({ data }: { data: DashboardData }) {
  const totalExpenses = data.pnlLines
    .filter((line) => line.kind !== "revenue")
    .reduce((sum, line) => sum + Math.abs(line.amount), 0);

  return (
    <div className="screen">
      <div className="sectionTitle">
        <div>
          <p className="eyebrow">Deterministic calculation</p>
          <h1>Profit & loss</h1>
        </div>
        <div className="monthPill">{data.restaurant.month}</div>
      </div>

      <section className="pnlSheet">
        {data.pnlLines.map((line) => (
          <div className="pnlRow" key={line.label}>
            <div>
              <strong>{line.label}</strong>
              <span>{line.kind === "revenue" ? "Money in" : "Money out"}</span>
            </div>
            <span className={line.amount >= 0 ? "credit" : "debit"}>{money.format(line.amount)}</span>
            <small>{formatChange(line.changePercent)} vs last month</small>
          </div>
        ))}
        <div className="pnlTotal">
          <div>
            <span>Total expenses</span>
            <strong>{money.format(totalExpenses)}</strong>
          </div>
          <div>
            <span>Net profit</span>
            <strong>{money.format(data.metrics.netProfit)}</strong>
          </div>
          <div>
            <span>Profit margin</span>
            <strong>{data.metrics.profitMargin.toFixed(1)}%</strong>
          </div>
        </div>
      </section>
    </div>
  );
}

function Insights({ data }: { data: DashboardData }) {
  return (
    <div className="screen">
      <div className="sectionTitle">
        <div>
          <p className="eyebrow">Plain English facts</p>
          <h1>Insights</h1>
        </div>
      </div>
      <div className="insightGrid">
        {data.insights.map((insight) => (
          <section className={`insightCard ${insight.severity}`} key={insight.title}>
            <div className="insightIcon">
              {insight.severity === "good" ? <CheckCircle2 size={22} /> : <AlertTriangle size={22} />}
            </div>
            <div>
              <h2>{insight.title}</h2>
              <p>{insight.detail}</p>
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}

function Ask({ data }: { data: DashboardData }) {
  const [question, setQuestion] = useState(data.questions[0]);
  const answer = useMemo(() => {
    if (question.includes("profit")) {
      return `Sales were up ${formatChange(data.metrics.revenueChangePercent)}, but profit was down ${formatChange(
        data.metrics.profitChangePercent
      )}. The main reason is that food costs and delivery fees grew faster than sales.`;
    }

    if (question.includes("labor")) {
      return `Labor cost was ${data.metrics.laborCostPercent.toFixed(1)}% of sales. That is workable for many restaurants, but it should be watched with prime cost.`;
    }

    if (question.includes("food")) {
      return `Food cost was ${data.metrics.foodCostPercent.toFixed(1)}% of sales, and Sysco spending increased ${formatChange(
        data.topVendors[0].changePercent
      )}. That is the clearest place to review invoices first.`;
    }

    return `Start with food and delivery fees. Together they explain most of the month-over-month pressure on profit.`;
  }, [data, question]);

  return (
    <div className="screen">
      <div className="sectionTitle">
        <div>
          <p className="eyebrow">Explains calculated facts only</p>
          <h1>Ask</h1>
        </div>
      </div>

      <section className="askPanel">
        <div className="questionBox">
          <Search size={20} />
          <select value={question} onChange={(event) => setQuestion(event.target.value)} aria-label="Ask a question">
            {data.questions.map((item) => (
              <option key={item}>{item}</option>
            ))}
          </select>
        </div>
        <div className="answerBox">
          <Bot size={26} />
          <p>{answer}</p>
        </div>
      </section>
    </div>
  );
}

export function RestaurantDashboard({
  data,
  restaurants,
  selectedRestaurantId,
  userName,
  onRestaurantChange,
  onLogout,
  onImportCsv
}: {
  data: DashboardData;
  restaurants: Array<{ id: string; name: string; role: string }>;
  selectedRestaurantId: string;
  userName: string;
  onRestaurantChange: (restaurantId: string) => void;
  onLogout: () => void;
  onImportCsv: (input: {
    accountType: "BANK" | "CREDIT_CARD";
    signMode: "AUTO" | "MONEY_IN_POSITIVE" | "MONEY_OUT_POSITIVE";
    csv: string;
  }) => Promise<void>;
}) {
  const [activeTab, setActiveTab] = useState<TabId>("dashboard");
  const ActiveIcon = tabs.find((tab) => tab.id === activeTab)?.icon ?? Home;

  return (
    <main>
      <aside className="sidebar">
        <div className="brand">
          <span>TP</span>
          <div>
            <strong>TableProof</strong>
            <small>P&L for restaurants</small>
          </div>
        </div>

        <nav>
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                className={activeTab === tab.id ? "navButton active" : "navButton"}
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                type="button"
                title={tab.label}
              >
                <Icon size={19} />
                {tab.label}
              </button>
            );
          })}
        </nav>

        <div className="sidebarFooter">
          <div className="workspacePicker">
            <ActiveIcon size={18} />
            <select
              aria-label="Restaurant workspace"
              value={selectedRestaurantId}
              onChange={(event) => onRestaurantChange(event.target.value)}
            >
              {restaurants.map((restaurant) => (
                <option key={restaurant.id} value={restaurant.id}>
                  {restaurant.name}
                </option>
              ))}
            </select>
          </div>
          <div className="signedIn">
            <span>{userName}</span>
            <button type="button" onClick={onLogout} title="Sign out">
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>

      <div className="content">
        {activeTab === "dashboard" ? <Dashboard data={data} /> : null}
        {activeTab === "transactions" ? <Transactions data={data} onImportCsv={onImportCsv} /> : null}
        {activeTab === "pnl" ? <ProfitAndLoss data={data} /> : null}
        {activeTab === "insights" ? <Insights data={data} /> : null}
        {activeTab === "ask" ? <Ask data={data} /> : null}
      </div>
    </main>
  );
}
