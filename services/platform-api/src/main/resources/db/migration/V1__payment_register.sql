CREATE TABLE payment_drafts (
    id UUID PRIMARY KEY,
    reference VARCHAR(80) NOT NULL,
    customer_reference VARCHAR(80),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    channel VARCHAR(12) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_payment_drafts_created ON payment_drafts(created_at DESC);
CREATE TABLE payment_activity (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payment_drafts(id),
    action VARCHAR(32) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_payment_activity_payment ON payment_activity(payment_id, occurred_at);
