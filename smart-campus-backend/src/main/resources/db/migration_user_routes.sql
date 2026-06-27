-- 用户自创路线表
CREATE TABLE user_routes (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(100) NOT NULL,
    summary         VARCHAR(200),
    description     TEXT,
    default_mode    VARCHAR(20) NOT NULL DEFAULT 'walking',
    cover_image_url VARCHAR(255),
    status          SMALLINT NOT NULL DEFAULT 1,
    like_count      INT NOT NULL DEFAULT 0,
    favorite_count  INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_route_waypoints (
    id              BIGSERIAL PRIMARY KEY,
    route_id        BIGINT NOT NULL REFERENCES user_routes(id) ON DELETE CASCADE,
    poi_id          BIGINT REFERENCES pois(id) ON DELETE SET NULL,
    latitude        DECIMAL(10,7) NOT NULL,
    longitude       DECIMAL(10,7) NOT NULL,
    waypoint_name   VARCHAR(100),
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_routes_user_id ON user_routes(user_id);
CREATE INDEX idx_user_routes_status ON user_routes(status);

CREATE TABLE user_route_likes (
    id          BIGSERIAL PRIMARY KEY,
    route_id    BIGINT NOT NULL REFERENCES user_routes(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_route_likes UNIQUE (route_id, user_id)
);

CREATE TABLE user_route_favorites (
    id          BIGSERIAL PRIMARY KEY,
    route_id    BIGINT NOT NULL REFERENCES user_routes(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_route_favorites UNIQUE (route_id, user_id)
);
