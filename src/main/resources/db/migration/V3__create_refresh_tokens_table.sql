CREATE TABLE refresh_tokens (
    token VARCHAR(100) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
