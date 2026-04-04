package com.smartcampus.config;

import com.smartcampus.util.ImageThumbnailUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class ImageThumbnailWarmupInitializer implements ApplicationRunner {

    private static final int COVER_THUMB_MAX_WIDTH = 480;
    private static final int COVER_THUMB_MAX_HEIGHT = 320;
    private static final int AVATAR_THUMB_MAX_WIDTH = 256;
    private static final int AVATAR_THUMB_MAX_HEIGHT = 256;
    private static final int SHARE_THUMB_MAX_WIDTH = 640;
    private static final int SHARE_THUMB_MAX_HEIGHT = 640;

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

    @Override
    public void run(ApplicationArguments args) {
        warmUpDirectory(avatarDir, AVATAR_THUMB_MAX_WIDTH, AVATAR_THUMB_MAX_HEIGHT);
        warmUpDirectory(poiShareDir, SHARE_THUMB_MAX_WIDTH, SHARE_THUMB_MAX_HEIGHT);
        warmUpDirectory(announcementDir, COVER_THUMB_MAX_WIDTH, COVER_THUMB_MAX_HEIGHT);
        warmUpDirectory(activityDir, COVER_THUMB_MAX_WIDTH, COVER_THUMB_MAX_HEIGHT);
        warmUpDirectory(routeDir, COVER_THUMB_MAX_WIDTH, COVER_THUMB_MAX_HEIGHT);
    }

    private void warmUpDirectory(String directory, int maxWidth, int maxHeight) {
        Path root = Paths.get(directory).toAbsolutePath().normalize();
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return;
        }

        int createdCount = 0;
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> originals = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !ImageThumbnailUtils.isThumbnailFile(path))
                    .toList();

            for (Path original : originals) {
                try {
                    ImageThumbnailUtils.createThumbnailIfMissing(original, maxWidth, maxHeight);
                    createdCount++;
                } catch (IOException ex) {
                    log.warn("生成缩略图失败: {}", original, ex);
                }
            }
        } catch (IOException ex) {
            log.warn("扫描图片目录失败: {}", root, ex);
            return;
        }

        if (createdCount > 0) {
            log.info("图片缩略图预热完成: dir={}, processed={}", root, createdCount);
        }
    }
}
