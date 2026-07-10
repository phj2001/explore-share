package com.smartcampus.util;

import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class ImageThumbnailUtils {

    private static final Set<String> THUMBNAIL_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final String THUMBNAIL_SUFFIX = "_thumb";

    private ImageThumbnailUtils() {
    }

    public static String resolveThumbnailUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return imageUrl;
        }

        int slashIndex = imageUrl.lastIndexOf('/');
        int dotIndex = imageUrl.lastIndexOf('.');
        if (dotIndex <= slashIndex) {
            return imageUrl;
        }

        String extension = imageUrl.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!THUMBNAIL_EXTENSIONS.contains(extension)) {
            return imageUrl;
        }

        return imageUrl.substring(0, dotIndex) + THUMBNAIL_SUFFIX + imageUrl.substring(dotIndex);
    }

    /**
     * 为支持的图片格式生成缩略图。
     *
     * @return true 表示真实写出了缩略图文件；false 表示无需生成（格式不支持 / 解码失败 / 原图已小于缩略图尺寸）
     */
    public static boolean createThumbnailIfSupported(byte[] bytes, String extension, Path originalPath, int maxWidth, int maxHeight) throws IOException {
        if (bytes == null || bytes.length == 0 || originalPath == null) {
            return false;
        }

        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!THUMBNAIL_EXTENSIONS.contains(normalizedExtension)) {
            return false;
        }

        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            return false;
        }

        Dimension dimension = calculateDimension(source.getWidth(), source.getHeight(), maxWidth, maxHeight);
        if (dimension.width >= source.getWidth() && dimension.height >= source.getHeight()) {
            Files.deleteIfExists(resolveThumbnailPath(originalPath));
            return false;
        }

        int imageType = "png".equals(normalizedExtension) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage thumbnail = new BufferedImage(dimension.width, dimension.height, imageType);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (imageType == BufferedImage.TYPE_INT_RGB) {
                graphics.setComposite(AlphaComposite.Src);
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, dimension.width, dimension.height);
            }

            Image scaled = source.getScaledInstance(dimension.width, dimension.height, Image.SCALE_SMOOTH);
            graphics.drawImage(scaled, 0, 0, dimension.width, dimension.height, null);
        } finally {
            graphics.dispose();
        }

        ImageIO.write(thumbnail, normalizedExtension.equals("jpeg") ? "jpg" : normalizedExtension, resolveThumbnailPath(originalPath).toFile());
        return true;
    }

    /**
     * 缩略图缺失时补生成（启动预热用）。
     *
     * @return true 表示本次真实生成了缩略图；false 表示已存在或无需生成
     */
    public static boolean createThumbnailIfMissing(Path originalPath, int maxWidth, int maxHeight) throws IOException {
        if (originalPath == null || !Files.isRegularFile(originalPath)) {
            return false;
        }

        Path thumbnailPath = resolveThumbnailPath(originalPath);
        if (Files.exists(thumbnailPath)) {
            return false;
        }

        String extension = getExtension(originalPath.getFileName().toString());
        if (!THUMBNAIL_EXTENSIONS.contains(extension)) {
            return false;
        }

        byte[] bytes = Files.readAllBytes(originalPath);
        return createThumbnailIfSupported(bytes, extension, originalPath, maxWidth, maxHeight);
    }

    public static void deleteImageAndThumbnailQuietly(Path originalPath) {
        if (originalPath == null) {
            return;
        }

        try {
            Files.deleteIfExists(originalPath);
        } catch (IOException ignored) {
        }

        try {
            Files.deleteIfExists(resolveThumbnailPath(originalPath));
        } catch (IOException ignored) {
        }
    }

    private static Path resolveThumbnailPath(Path originalPath) {
        String filename = originalPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return originalPath.resolveSibling(filename + THUMBNAIL_SUFFIX);
        }
        return originalPath.resolveSibling(filename.substring(0, dotIndex) + THUMBNAIL_SUFFIX + filename.substring(dotIndex));
    }

    private static Dimension calculateDimension(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return new Dimension(maxWidth, maxHeight);
        }

        double ratio = Math.min((double) maxWidth / sourceWidth, (double) maxHeight / sourceHeight);
        ratio = Math.min(ratio, 1D);

        int width = Math.max(1, (int) Math.round(sourceWidth * ratio));
        int height = Math.max(1, (int) Math.round(sourceHeight * ratio));
        return new Dimension(width, height);
    }

    public static boolean isThumbnailFile(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename.endsWith(THUMBNAIL_SUFFIX);
        }
        return filename.substring(0, dotIndex).endsWith(THUMBNAIL_SUFFIX);
    }

    private static String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private record Dimension(int width, int height) {
    }
}
