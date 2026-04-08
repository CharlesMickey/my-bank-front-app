CREATE SCHEMA IF NOT EXISTS accounts;

CREATE TABLE IF NOT EXISTS accounts.accounts (
    login VARCHAR(80) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    birth_date DATE NOT NULL,
    balance BIGINT NOT NULL CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0
);
