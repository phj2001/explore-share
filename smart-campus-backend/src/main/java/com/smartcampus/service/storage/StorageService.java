package com.smartcampus.service.storage;

/**
 * 文件存储抽象：把原本散落在各 Service 的"写文件 + 生成缩略图 + 删除"逻辑收口到一处。
 *
 * <p>当前唯一实现 {@link LocalFileStorage}（本地磁盘）。未来扩展到对象存储（OSS/MinIO/S3）时，
 * 只需新增一个实现类（如 OssFileStorage）并切换 Bean，业务 Service 无需任何改动——面向接口编程。
 *
 * <p>面试考点：接口抽象与策略模式、依赖倒置（业务 Service 依赖抽象而非本地磁盘实现）、
 * 存储关注点收口（路径校验/缩略图/清理统一，避免散落重复代码）。
 */
public interface StorageService {

    /**
     * 存储文件到指定分类目录，按分类配置生成缩略图，返回可访问的 URL（如 /uploads/avatars/xxx.jpg）。
     *
     * @param category 存储分类（决定目录、URL 前缀、缩略图尺寸）
     * @param filename 文件名（含扩展名，由调用方生成，如 uuid + ".jpg"）
     * @param content  文件字节内容
     * @return 可访问的 URL
     */
    String store(StorageCategory category, String filename, byte[] content);

    /**
     * 删除指定分类目录下的文件及其缩略图。幂等——文件不存在静默返回。
     *
     * @param category 存储分类
     * @param filename 文件名
     */
    void delete(StorageCategory category, String filename);
}
