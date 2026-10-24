CREATE TABLE tbl_audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID,
    description VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tbl_users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(255),
    avatar_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE tbl_roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tbl_permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tbl_user_roles (
    user_id UUID NOT NULL REFERENCES tbl_users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES tbl_roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE tbl_role_permissions (
    role_id UUID NOT NULL REFERENCES tbl_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES tbl_permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE tbl_auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tbl_users(id) ON DELETE CASCADE,
    device_id VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tbl_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tbl_users(id) ON DELETE CASCADE,
    session_id UUID REFERENCES tbl_auth_sessions(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(255),
    ip_address VARCHAR(50),
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refresh_live_session CHECK (session_id IS NOT NULL OR revoked_at IS NOT NULL)
);

CREATE TABLE tbl_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES tbl_users(id) ON DELETE CASCADE
);

CREATE TABLE tbl_password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tbl_users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_refresh_token_hash ON tbl_refresh_tokens(token_hash);
CREATE INDEX idx_refresh_user_device ON tbl_refresh_tokens(user_id, device_id)
    WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_expiration ON tbl_refresh_tokens(expires_at);
CREATE INDEX idx_refresh_session ON tbl_refresh_tokens(session_id);
CREATE UNIQUE INDEX idx_auth_session_user_device ON tbl_auth_sessions(user_id, device_id)
    WHERE revoked_at IS NULL;
CREATE INDEX idx_auth_session_expiration ON tbl_auth_sessions(expires_at);
CREATE INDEX idx_verification_user ON tbl_verification_tokens(user_id);
CREATE INDEX idx_audit_user_created ON tbl_audit_logs(user_id, created_at DESC);
CREATE INDEX idx_password_reset_user_created ON tbl_password_reset_tokens(user_id, created_at DESC);
CREATE INDEX idx_password_reset_expiration ON tbl_password_reset_tokens(expires_at);
