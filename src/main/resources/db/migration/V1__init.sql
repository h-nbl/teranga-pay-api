-- V1 : schéma initial. Une migration déjà appliquée ne se modifie JAMAIS :
-- pour changer quelque chose, on ajoute V3, V4, etc.

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    -- Numéro au format international (E.164), ex. +221771234567.
    -- La normalisation est faite par l'application ; la base refuse tout le reste.
    phone         VARCHAR(20)  NOT NULL UNIQUE
                  CHECK (phone ~ '^\+[1-9][0-9]{7,14}$'),
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL UNIQUE REFERENCES users (id),
    -- Montants en FCFA, en entiers (pas de float/double pour l'argent).
    -- Dernier garde-fou : le solde ne peut jamais être négatif.
    balance    BIGINT      NOT NULL DEFAULT 0 CHECK (balance >= 0),
    -- Verrouillage optimiste (@Version côté JPA).
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id              UUID PRIMARY KEY,
    type            VARCHAR(20) NOT NULL
                    CHECK (type IN ('DEPOSIT', 'TRANSFER')),
    -- NULL pour un dépôt (l'argent vient de l'extérieur).
    from_account_id UUID REFERENCES accounts (id),
    to_account_id   UUID        NOT NULL REFERENCES accounts (id),
    amount          BIGINT      NOT NULL CHECK (amount > 0),
    status          VARCHAR(20) NOT NULL
                    CHECK (status IN ('COMPLETED', 'BLOCKED', 'PENDING_REVIEW')),
    -- Idempotence : même clé + même émetteur = même opération.
    idempotency_key VARCHAR(80),
    -- Empreinte (SHA-256 en hexadécimal = 64 caractères) du corps de la requête :
    -- permet de détecter une même clé réutilisée avec un contenu différent.
    request_hash    VARCHAR(64),
    risk_score      INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (from_account_id, idempotency_key),
    -- Cohérence du type : un dépôt n'a pas d'émetteur ;
    -- un transfert en a un, différent du destinataire.
    CHECK (
        (type = 'DEPOSIT'  AND from_account_id IS NULL)
        OR
        (type = 'TRANSFER' AND from_account_id IS NOT NULL
                           AND from_account_id <> to_account_id)
    )
);

CREATE INDEX idx_tx_from_created ON transactions (from_account_id, created_at DESC);
CREATE INDEX idx_tx_to_created   ON transactions (to_account_id, created_at DESC);
