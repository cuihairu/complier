package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SDK版本仓库接口
 */
@Repository
public interface SDKVersionRepo extends JpaRepository<SDKVersionEntity, String> {

    /**
     * 根据平台查找版本
     */
    List<SDKVersionEntity> findByPlatformOrderByCreatedAtDesc(SDKVersionEntity.SDKPlatform platform);

    /**
     * 根据平台和状态查找版本
     */
    List<SDKVersionEntity> findByPlatformAndVersionStatusOrderByCreatedAtDesc(
            SDKVersionEntity.SDKPlatform platform, SDKVersionEntity.VersionStatus status);

    /**
     * 根据平台和版本号查找
     */
    Optional<SDKVersionEntity> findByPlatformAndVersion(SDKVersionEntity.SDKPlatform platform, String version);

    /**
     * 查找已发布的版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.versionStatus = 'RELEASED' ORDER BY v.platform, v.releasedAt DESC")
    List<SDKVersionEntity> findAllReleased();

    /**
     * 根据平台查找已发布的版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.platform = :platform AND v.versionStatus = 'RELEASED' ORDER BY v.releasedAt DESC")
    List<SDKVersionEntity> findReleasedByPlatform(@Param("platform") SDKVersionEntity.SDKPlatform platform);

    /**
     * 查找最新版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.platform = :platform AND v.versionStatus = 'RELEASED' ORDER BY v.releasedAt DESC")
    Optional<SDKVersionEntity> findLatestByPlatform(@Param("platform") SDKVersionEntity.SDKPlatform platform);

    /**
     * 查找Beta版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.platform = :platform AND v.versionStatus = 'BETA' ORDER BY v.createdAt DESC")
    List<SDKVersionEntity> findBetaByPlatform(@Param("platform") SDKVersionEntity.SDKPlatform platform);

    /**
     * 查找已弃用的版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.versionStatus = 'DEPRECATED' ORDER BY v.platform, v.releasedAt DESC")
    List<SDKVersionEntity> findAllDeprecated();

    /**
     * 查找即将退役的版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.versionStatus = 'DEPRECATED' AND v.retirementDate IS NOT NULL AND v.retirementDate < :threshold ORDER BY v.retirementDate ASC")
    List<SDKVersionEntity> findRetiringSoon(@Param("threshold") java.time.LocalDateTime threshold);

    /**
     * 统计各平台版本数量
     */
    @Query("SELECT v.platform, COUNT(v) FROM SDKVersionEntity v GROUP BY v.platform")
    List<Object[]> countByPlatform();

    /**
     * 统计各状态版本数量
     */
    @Query("SELECT v.versionStatus, COUNT(v) FROM SDKVersionEntity v GROUP BY v.versionStatus")
    List<Object[]> countByStatus();

    /**
     * 查找下载量最高的版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.totalDownloads > 0 ORDER BY v.totalDownloads DESC")
    List<SDKVersionEntity> findMostDownloaded();

    /**
     * 查找活跃安装量最高的版本
     */
    @Query("SELECT v FROM SDKVersionEntity v WHERE v.activeInstallations > 0 ORDER BY v.activeInstallations DESC")
    List<SDKVersionEntity> findMostInstalled();

    /**
     * 检查版本是否存在
     */
    boolean existsByPlatformAndVersion(SDKVersionEntity.SDKPlatform platform, String version);

    /**
     * 根据变更类型查找
     */
    List<SDKVersionEntity> findByChangeTypeOrderByCreatedAtDesc(SDKVersionEntity.ChangeType changeType);
}
