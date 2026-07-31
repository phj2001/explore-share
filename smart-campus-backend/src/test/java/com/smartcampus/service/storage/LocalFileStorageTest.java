package com.smartcampus.service.storage;

import com.smartcampus.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LocalFileStorage} 单元测试：store/delete、缩略图生成、路径穿越防护。
 * 用 {@link TempDir} 隔离存储目录，反射注入 {@code @Value} 字段（单元测试不启动 Spring）。
 */
class LocalFileStorageTest {

    private LocalFileStorage storage;
    private Path root;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        storage = new LocalFileStorage();
        root = tempDir.resolve("storage");
        setField("avatarDir", root.resolve("avatars").toString());
        setField("poiShareDir", root.resolve("poi-shares").toString());
        setField("announcementDir", root.resolve("announcements").toString());
        setField("activityDir", root.resolve("activities").toString());
        setField("routeDir", root.resolve("routes").toString());
        storage.init();
    }

    @Test
    void store写入文件并返回带前缀的URL() throws IOException {
        byte[] content = {1, 2, 3, 4};
        String url = storage.store(StorageCategory.AVATAR, "abc.jpg", content);

        assertThat(url).isEqualTo("/uploads/avatars/abc.jpg");
        assertThat(Files.exists(root.resolve("avatars/abc.jpg"))).isTrue();
        assertThat(Files.readAllBytes(root.resolve("avatars/abc.jpg"))).containsExactly(content);
    }

    @Test
    void delete删除文件且幂等() {
        storage.store(StorageCategory.ANNOUNCEMENT, "del.jpg", new byte[]{1});
        storage.delete(StorageCategory.ANNOUNCEMENT, "del.jpg");
        assertThat(Files.exists(root.resolve("announcements/del.jpg"))).isFalse();
        // 幂等：再次删除不存在的文件不抛异常
        storage.delete(StorageCategory.ANNOUNCEMENT, "del.jpg");
    }

    @Test
    void 无缩略图分类只存原图() {
        // AVATAR 缩略图尺寸为 null，store 后只应有原图
        storage.store(StorageCategory.AVATAR, "no-thumb.jpg", new byte[]{1});
        assertThat(Files.exists(root.resolve("avatars/no-thumb.jpg"))).isTrue();
        assertThat(Files.exists(root.resolve("avatars/no-thumb_thumb.jpg"))).isFalse();
    }

    @Test
    void 大图自动生成缩略图() throws IOException {
        // 800x800 大于 POI_SHARE 的 640x640，应生成缩略图
        byte[] bigJpg = makeJpeg(800, 800);
        storage.store(StorageCategory.POI_SHARE, "big.jpg", bigJpg);

        assertThat(Files.exists(root.resolve("poi-shares/big.jpg"))).isTrue();
        assertThat(Files.exists(root.resolve("poi-shares/big_thumb.jpg"))).isTrue();
    }

    @Test
    void 路径穿越防护拒绝逃逸路径() {
        assertThatThrownBy(() -> storage.store(StorageCategory.AVATAR, "../escape.jpg", new byte[]{1}))
                .isInstanceOf(BusinessException.class);
    }

    private void setField(String name, String value) throws Exception {
        Field f = LocalFileStorage.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(storage, value);
    }

    private byte[] makeJpeg(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}
