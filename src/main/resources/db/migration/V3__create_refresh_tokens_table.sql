CREATE TABLE refresh_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(255),
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_refresh_token_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
