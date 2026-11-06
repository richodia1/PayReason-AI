ALTER TABLE payment_drafts ADD COLUMN paypal_order_id VARCHAR(64) UNIQUE;
ALTER TABLE payment_drafts ADD COLUMN authorization_id VARCHAR(64);
ALTER TABLE payment_drafts ADD COLUMN capture_id VARCHAR(64);
ALTER TABLE payment_drafts ADD COLUMN approval_url VARCHAR(1000);
ALTER TABLE payment_activity ADD COLUMN notes VARCHAR(2000);
CREATE TABLE risk_assessments (
    payment_id UUID PRIMARY KEY REFERENCES payment_drafts(id), score DOUBLE PRECISION NOT NULL,
    band VARCHAR(12) NOT NULL, model_version VARCHAR(100) NOT NULL, source VARCHAR(32) NOT NULL,
    evidence TEXT NOT NULL, input_features TEXT NOT NULL, assessed_by VARCHAR(100) NOT NULL,
    assessed_at TIMESTAMP WITH TIME ZONE NOT NULL, decision VARCHAR(12) NOT NULL,
    review_notes VARCHAR(2000), reviewed_by VARCHAR(100), reviewed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE paypal_operations (
    id UUID PRIMARY KEY, payment_id UUID NOT NULL REFERENCES payment_drafts(id),
    kind VARCHAR(16) NOT NULL, state VARCHAR(16) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL, leased_until TIMESTAMP WITH TIME ZONE,
    UNIQUE(payment_id, kind)
);
CREATE TABLE paypal_webhooks (
    id VARCHAR(100) PRIMARY KEY, event_type VARCHAR(100) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);
