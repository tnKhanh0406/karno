CREATE TABLE checkout_sagas (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    context_payload JSONB NOT NULL,
    result_payload JSONB,
    failure_code VARCHAR(100),
    failure_message TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_checkout_saga_customer_idempotency
        UNIQUE (customer_id, idempotency_key)
);