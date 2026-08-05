CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_embeddings (
    id BIGSERIAL PRIMARY KEY,
    knowledge_file_id BIGINT,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(384),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_embeddings_vector
    ON knowledge_embeddings USING ivfflat (embedding vector_cosine_ops);
