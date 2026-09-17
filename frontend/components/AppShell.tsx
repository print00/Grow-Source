"use client";

import { Building2, Loader2, LockKeyhole, Store, UserPlus } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { getDashboard, getMe, importCsv, login, register } from "../lib/api";
import type { AuthSession, DashboardData } from "../lib/types";
import { RestaurantDashboard } from "./RestaurantDashboard";

const SESSION_KEY = "tableproof_session";

export function AppShell() {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [restaurantId, setRestaurantId] = useState<string>("");
  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refreshIndex, setRefreshIndex] = useState(0);

  useEffect(() => {
    const saved = window.localStorage.getItem(SESSION_KEY);
    if (!saved) {
      setLoading(false);
      return;
    }

    const parsed = JSON.parse(saved) as AuthSession;
    getMe(parsed.token)
      .then((fresh) => {
        saveSession(fresh);
      })
      .catch(() => {
        window.localStorage.removeItem(SESSION_KEY);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!session || !restaurantId) {
      return;
    }

    setDashboard(null);
    getDashboard(session.token, restaurantId)
      .then(setDashboard)
      .catch(() => setError("Could not load this restaurant dashboard."));
  }, [session, restaurantId, refreshIndex]);

  function saveSession(nextSession: AuthSession) {
    setSession(nextSession);
    setRestaurantId(nextSession.restaurants[0]?.id ?? "");
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
  }

  function logout() {
    window.localStorage.removeItem(SESSION_KEY);
    setSession(null);
    setDashboard(null);
    setRestaurantId("");
  }

  async function handleImportCsv(input: {
    accountType: "BANK" | "CREDIT_CARD";
    signMode: "AUTO" | "MONEY_IN_POSITIVE" | "MONEY_OUT_POSITIVE";
    csv: string;
  }) {
    if (!session || !restaurantId) {
      return;
    }

    await importCsv({
      token: session.token,
      restaurantId,
      accountType: input.accountType,
      signMode: input.signMode,
      csv: input.csv
    });
    setRefreshIndex((current) => current + 1);
  }

  if (loading) {
    return (
      <main className="authLoading">
        <Loader2 className="spin" size={28} />
      </main>
    );
  }

  if (!session) {
    return <AuthScreen onAuthenticated={saveSession} />;
  }

  if (!dashboard) {
    return (
      <main className="authLoading">
        <Loader2 className="spin" size={28} />
        <p>{error || "Loading restaurant dashboard..."}</p>
      </main>
    );
  }

  return (
    <RestaurantDashboard
      data={dashboard}
      restaurants={session.restaurants}
      selectedRestaurantId={restaurantId}
      userName={session.user.name}
      onRestaurantChange={setRestaurantId}
      onLogout={logout}
      onImportCsv={handleImportCsv}
    />
  );
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (session: AuthSession) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("owner@harborspoon.test");
  const [password, setPassword] = useState("TableProof123!");
  const [ownerName, setOwnerName] = useState("Sam Rivera");
  const [restaurantName, setRestaurantName] = useState("Harbor Spoon Cafe");
  const [restaurantType, setRestaurantType] = useState("Casual dining");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const title = useMemo(
    () => (mode === "login" ? "Sign in to your restaurant P&L" : "Create your restaurant workspace"),
    [mode]
  );

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const session =
        mode === "login"
          ? await login(email, password)
          : await register({ email, password, ownerName, restaurantName, restaurantType });
      onAuthenticated(session);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Something went wrong");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="authPage">
      <section className="authHero">
        <div className="brand authBrand">
          <span>TP</span>
          <div>
            <strong>TableProof</strong>
            <small>P&L for restaurants</small>
          </div>
        </div>
        <h1>Know what your restaurant actually kept.</h1>
        <p>
          Secure owner workspaces, restaurant-level access, deterministic P&L math, and plain-English
          insights for every location.
        </p>
      </section>

      <section className="authCard">
        <div className="authSwitch">
          <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")} type="button">
            <LockKeyhole size={17} />
            Sign in
          </button>
          <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")} type="button">
            <UserPlus size={17} />
            New restaurant
          </button>
        </div>

        <form onSubmit={submit}>
          <div className="formTitle">
            <Building2 size={24} />
            <h2>{title}</h2>
          </div>

          {mode === "register" ? (
            <>
              <label>
                Owner name
                <input value={ownerName} onChange={(event) => setOwnerName(event.target.value)} required />
              </label>
              <label>
                Restaurant name
                <input value={restaurantName} onChange={(event) => setRestaurantName(event.target.value)} required />
              </label>
              <label>
                Restaurant type
                <input value={restaurantType} onChange={(event) => setRestaurantType(event.target.value)} required />
              </label>
            </>
          ) : (
            <div className="demoHint">
              <Store size={18} />
              <span>Demo login: owner@harborspoon.test / TableProof123!</span>
            </div>
          )}

          <label>
            Email
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              minLength={8}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>

          {error ? <p className="formError">{error}</p> : null}

          <button className="primaryButton authSubmit" type="submit" disabled={busy}>
            {busy ? <Loader2 className="spin" size={18} /> : null}
            {mode === "login" ? "Sign in" : "Create workspace"}
          </button>
        </form>
      </section>
    </main>
  );
}
