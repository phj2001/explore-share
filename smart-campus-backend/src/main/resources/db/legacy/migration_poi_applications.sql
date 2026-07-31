-- POI 申请表
CREATE TABLE IF NOT EXISTS poi_applications (
    id              BIGSERIAL PRIMARY KEY,
    applicant_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    description     TEXT,
    latitude        DECIMAL(10,7) NOT NULL,
    longitude       DECIMAL(10,7) NOT NULL,
    address         VARCHAR(255),
    photo_urls      TEXT,
    status          SMALLINT NOT NULL DEFAULT 1,
    reviewed_by     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMP,
    review_note     VARCHAR(500),
    created_poi_id  BIGINT REFERENCES pois(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_poi_applications_applicant ON poi_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_poi_applications_status ON poi_applications(status);
