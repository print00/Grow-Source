create table restaurants (
    id uuid primary key,
    name text not null,
    restaurant_type text not null,
    created_at timestamptz not null default now()
);

create table users (
    id uuid primary key,
    email text not null unique,
    owner_name text not null,
    password_hash text not null,
    created_at timestamptz not null default now()
);

create table restaurant_memberships (
    id uuid primary key,
    restaurant_id uuid not null references restaurants(id),
    user_id uuid not null references users(id),
    role text not null check (role in ('owner', 'manager', 'bookkeeper')),
    created_at timestamptz not null default now(),
    unique (restaurant_id, user_id)
);

create table bank_imports (
    id uuid primary key,
    restaurant_id uuid not null references restaurants(id),
    source_filename text not null,
    status text not null check (status in ('uploaded', 'normalized', 'needs_review', 'closed')),
    imported_at timestamptz not null default now()
);

create table vendors (
    id uuid primary key,
    restaurant_id uuid not null references restaurants(id),
    display_name text not null,
    normalized_name text not null,
    default_category text,
    created_at timestamptz not null default now(),
    unique (restaurant_id, normalized_name)
);

create table transactions (
    id uuid primary key,
    restaurant_id uuid not null references restaurants(id),
    bank_import_id uuid references bank_imports(id),
    vendor_id uuid references vendors(id),
    posted_date date not null,
    raw_description text not null,
    normalized_description text not null,
    amount numeric(12, 2) not null,
    direction text not null check (direction in ('credit', 'debit')),
    category text not null,
    category_group text not null check (category_group in ('revenue', 'cogs', 'expense')),
    confidence text not null check (confidence in ('high', 'medium', 'low')),
    owner_confirmed boolean not null default false,
    created_at timestamptz not null default now()
);

create table monthly_pnl (
    id uuid primary key,
    restaurant_id uuid not null references restaurants(id),
    month date not null,
    revenue numeric(12, 2) not null,
    food_cost numeric(12, 2) not null,
    labor_cost numeric(12, 2) not null,
    prime_cost numeric(12, 2) not null,
    expenses numeric(12, 2) not null,
    net_profit numeric(12, 2) not null,
    profit_margin numeric(6, 2) not null,
    average_daily_sales numeric(12, 2) not null,
    calculated_at timestamptz not null default now(),
    unique (restaurant_id, month)
);

create table insights (
    id uuid primary key,
    restaurant_id uuid not null references restaurants(id),
    monthly_pnl_id uuid not null references monthly_pnl(id),
    severity text not null check (severity in ('good', 'warning', 'alert')),
    title text not null,
    deterministic_fact text not null,
    explanation text not null,
    created_at timestamptz not null default now()
);

create index transactions_restaurant_month_idx on transactions (restaurant_id, posted_date);
create index transactions_review_idx on transactions (restaurant_id, confidence, owner_confirmed);
create index vendors_restaurant_name_idx on vendors (restaurant_id, normalized_name);
create index restaurant_memberships_user_idx on restaurant_memberships (user_id);
create index restaurant_memberships_restaurant_idx on restaurant_memberships (restaurant_id);
