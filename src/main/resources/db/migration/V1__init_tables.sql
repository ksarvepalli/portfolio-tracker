-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Asset classes reference table
CREATE TABLE asset_classes (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

INSERT INTO asset_classes (code, name) VALUES
('STOCK', 'Stocks'),
('MUTUAL_FUND', 'Mutual Funds'),
('GOLD', 'Gold'),
('REAL_ESTATE', 'Real Estate'),
('FIXED_DEPOSIT', 'Fixed Deposits');

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Holdings table
CREATE TABLE holdings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_class VARCHAR(20) NOT NULL REFERENCES asset_classes(code),
    asset_symbol VARCHAR(50),
    asset_name VARCHAR(255) NOT NULL,
    quantity DECIMAL(15,6) NOT NULL DEFAULT 0,
    avg_cost_price DECIMAL(15,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'INR',
    properties JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    holding_id UUID NOT NULL REFERENCES holdings(id) ON DELETE CASCADE,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('BUY', 'SELL', 'DIVIDEND')),
    quantity DECIMAL(15,6) NOT NULL,
    price DECIMAL(15,4) NOT NULL,
    transaction_date DATE NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Market data tables
CREATE TABLE stock_prices (
    symbol VARCHAR(50) NOT NULL,
    trade_date DATE NOT NULL,
    open_price DECIMAL(15,4),
    high_price DECIMAL(15,4),
    low_price DECIMAL(15,4),
    close_price DECIMAL(15,4),
    volume BIGINT,
    PRIMARY KEY (symbol, trade_date)
);

CREATE TABLE mutual_fund_nav (
    scheme_code VARCHAR(20) NOT NULL,
    nav_date DATE NOT NULL,
    nav DECIMAL(15,4) NOT NULL,
    PRIMARY KEY (scheme_code, nav_date)
);

-- Daily net worth snapshots
CREATE TABLE net_worth_snapshots (
    user_id UUID NOT NULL REFERENCES users(id),
    snapshot_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_value DECIMAL(15,2) NOT NULL,
    breakdown JSONB,
    PRIMARY KEY (user_id, snapshot_date)
);