-- 个人主页可见性：0 公开（默认）/ 1 仅关注者 / 2 仅自己
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_visibility SMALLINT NOT NULL DEFAULT 0;
