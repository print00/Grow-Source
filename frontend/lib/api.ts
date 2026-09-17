import type { AuthSession, DashboardData } from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function login(email: string, password: string): Promise<AuthSession> {
  const response = await fetch(`${API_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) {
    throw new Error("Invalid email or password");
  }

  return response.json();
}

export async function register(input: {
  email: string;
  password: string;
  ownerName: string;
  restaurantName: string;
  restaurantType: string;
}): Promise<AuthSession> {
  const response = await fetch(`${API_URL}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input)
  });

  if (!response.ok) {
    throw new Error("Could not create account");
  }

  return response.json();
}

export async function getMe(token: string): Promise<AuthSession> {
  const response = await fetch(`${API_URL}/api/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store"
  });

  if (!response.ok) {
    throw new Error("Session expired");
  }

  return response.json();
}

export async function getDashboard(token: string, restaurantId: string): Promise<DashboardData> {
  try {
    const response = await fetch(`${API_URL}/api/restaurants/${restaurantId}/dashboard`, {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store"
    });

    if (!response.ok) {
      throw new Error("Dashboard request failed");
    }

    return response.json();
  } catch {
    throw new Error("Dashboard request failed");
  }
}

export async function importCsv(input: {
  token: string;
  restaurantId: string;
  accountType: "BANK" | "CREDIT_CARD";
  signMode: "AUTO" | "MONEY_IN_POSITIVE" | "MONEY_OUT_POSITIVE";
  csv: string;
}) {
  const params = new URLSearchParams({
    accountType: input.accountType,
    signMode: input.signMode
  });
  const response = await fetch(`${API_URL}/api/restaurants/${input.restaurantId}/import-csv?${params}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${input.token}`,
      "Content-Type": "text/csv"
    },
    body: input.csv
  });

  if (!response.ok) {
    throw new Error("CSV import failed");
  }

  return response.json();
}
