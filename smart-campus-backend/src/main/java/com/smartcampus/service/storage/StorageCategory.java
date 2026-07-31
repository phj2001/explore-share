package com.smartcampus.service.storage;

/**
 * 文件存储分类：每类对应独立的存储目录、访问 URL 前缀、可选的缩略图尺寸。
 *
 * <p>缩略图尺寸为 null 表示该类文件上传时不自动生成缩略图（如头像由启动预热负责补生成）。
 * 目录路径由 {@code app.upload.*-dir} 配置项决定（见 LocalFileStorage），URL 前缀与 WebConfig 静态资源映射对应。
 */
public enum StorageCategory {

    AVATAR("/uploads/avatars/", null, null),
    POI_SHARE("/uploads/poi-shares/", 640, 640),
    ANNOUNCEMENT("/uploads/announcements/", 480, 320),
    ACTIVITY("/uploads/activities/", 480, 320),
    ROUTE("/uploads/routes/", 480, 320);

    private final String urlPrefix;
    private final Integer thumbWidth;
    private final Integer thumbHeight;

    StorageCategory(String urlPrefix, Integer thumbWidth, Integer thumbHeight) {
        this.urlPrefix = urlPrefix;
        this.thumbWidth = thumbWidth;
        this.thumbHeight = thumbHeight;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    /** 缩略图最大宽度；null 表示不生成缩略图。 */
    public Integer getThumbWidth() {
        return thumbWidth;
    }

    public Integer getThumbHeight() {
        return thumbHeight;
    }
}
