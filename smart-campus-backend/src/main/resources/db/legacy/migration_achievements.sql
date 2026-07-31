-- 成就定义表
CREATE TABLE achievement_definitions (
    id          VARCHAR(50) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(200) NOT NULL,
    icon_url    VARCHAR(255),
    category    VARCHAR(50) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 用户成就表
CREATE TABLE user_achievements (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  VARCHAR(50) NOT NULL REFERENCES achievement_definitions(id),
    unlocked_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_achievements UNIQUE (user_id, achievement_id)
);

CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);

-- 初始化成就定义数据
INSERT INTO achievement_definitions (id, name, description, category, sort_order) VALUES
('check_in_1',   '初次打卡',   '完成第一次地点打卡',     '探索', 1),
('check_in_10',  '探索者',     '打卡 10 个不同地点',     '探索', 2),
('check_in_50',  '资深探索者', '打卡 50 个不同地点',     '探索', 3),
('check_in_100', '百地达人',   '打卡 100 个不同地点',    '探索', 4),
('share_1',      '第一次分享', '发布第一条打卡内容',     '社交', 5),
('share_10',     '内容创作者', '发布 10 条打卡内容',     '社交', 6),
('likes_10',     '受欢迎',     '获得累计 10 个赞',       '社交', 7),
('likes_100',    '人气之星',   '获得累计 100 个赞',      '社交', 8),
('category_5',   '多元探索',   '打卡涵盖 5 个不同分类',  '探索', 9),
('route_1',      '路线规划师', '完成第一次路线规划',     '路线', 10);
