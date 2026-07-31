package com.smartcampus.service.storage;

import com.smartcampus.exception.BusinessException;
import com.smartcampus.util.ImageThumbnailUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 本地磁盘存储实现。
 *
 * <p>每个 {@link StorageCategory} 对应一个目录（由 {@code app.upload.*-dir} 配置），{@link PostConstruct} 时建目录。
 * <ul>
 *   <li>{@code store}：路径穿越校验 → 写原图 → 按分类配置生成缩略图
 *       （下沉了原本散落在 POIShare/AdminAnnouncement/AdminActivity/AdminRecommendedRoute 的 ImageThumbnailUtils 调用）；</li>
 *   <li>{@code delete}：删除原图 + 缩略图（{@link ImageThumbnailUtils#deleteImageAndThumbnailQuietly}，幂等）。</li>
 * </ul>
 *
 * <p>改造前各 Service 各自维护 storagePath / saveXxx / deleteXxx / 缩略图调用，逻辑重复且易漂移；
 * 收口后存储关注点集中在此，业务 Service 只调 {@code store/delete}。
 *
 * <p>面试考点：策略模式的可切换实现（本地→对象存储无感切换）、路径穿越防护、缩略图作为存储实现的内部细节。
 */
@Slf4j
@Component
public class LocalFileStorage implements StorageService {

    private final Map<StorageCategory, Path> basePaths = new EnumMap<>(StorageCategory.class);

    @Value("${app.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;
    @Value("${app.upload.poi-share-dir:uploads/poi-shares}")
    private String poiShareDir;
    @Value("${app.upload.announcement-dir:uploads/announcements}")
    private String announcementDir;
    @Value("${app.upload.activity-dir:uploads/activities}")
    private String activityDir;
    @Value("${app.upload.route-dir:uploads/routes}")
    private String routeDir;

    @PostConstruct
    public void init() {
        register(StorageCategory.AVATAR, avatarDir);
        register(StorageCategory.POI_SHARE, poiShareDir);
        register(StorageCategory.ANNOUNCEMENT, announcementDir);
        register(StorageCategory.ACTIVITY, activityDir);
        register(StorageCategory.ROUTE, routeDir);
        log.info("本地文件存储初始化完成，分类数：{}", basePaths.size());
    }

    private void register(StorageCategory category, String dir) {
        Path path = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建存储目录: " + dir, e);
        }
        basePaths.put(category, path);
    }

    @Override
    public String store(StorageCategory category, String filename, byte[] content) {
        Path base = basePaths.get(category);
        Path target = base.resolve(filename).normalize();
        // 路径穿越防护：解析后必须仍在 base 目录内
        if (!target.startsWith(base)) {
            throw new BusinessException(400, "非法的存储路径");
        }
        try {
            Files.write(target, content);
            Integer width = category.getThumbWidth();
            if (width != null) {
                String extension = extractExtension(filename);
                ImageThumbnailUtils.createThumbnailIfSupported(content, extension, target,
                        width, category.getThumbHeight());
            }
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }
        return category.getUrlPrefix() + filename;
    }

    @Override
    public void delete(StorageCategory category, String filename) {
        Path base = basePaths.get(category);
        Path target = base.resolve(filename).normalize();
        if (!target.startsWith(base)) {
            return;
        }
        ImageThumbnailUtils.deleteImageAndThumbnailQuietly(target);
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
