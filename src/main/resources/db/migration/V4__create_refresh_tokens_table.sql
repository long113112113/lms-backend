CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    token       VARCHAR(100) NOT NULL UNIQUE,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id   VARCHAR(100) NOT NULL,
    device_name VARCHAR(255),
    expiry_date TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_refresh_token_user_device UNIQUE (user_id, device_id)
);

-- Covering Index: tối ưu kiểm tra Token Expiry ngay trên Index
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id) INCLUDE (expiry_date);
