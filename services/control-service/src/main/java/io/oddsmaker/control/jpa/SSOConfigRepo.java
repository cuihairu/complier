package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SSOConfigRepo extends JpaRepository<SSOConfigEntity, String> {

    /**
     * 查找活跃的SSO配置
     */
    @Query("SELECT s FROM SSOConfigEntity s WHERE s.ssoStatus = 'ACTIVE' AND s.deletedAt IS NULL ORDER BY s.isDefault DESC, s.createdAt DESC")
    List<SSOConfigEntity> findActive();

    /**
     * 查找默认SSO配置
     */
    @Query("SELECT s FROM SSOConfigEntity s WHERE s.isDefault = true AND s.deletedAt IS NULL")
    Optional<SSOConfigEntity> findDefault();

    /**
     * 根据协议查找
     */
    @Query("SELECT s FROM SSOConfigEntity s WHERE s.ssoProtocol = :protocol AND s.deletedAt IS NULL")
    List<SSOConfigEntity> findByProtocol(@Param("protocol") SSOConfigEntity.SSOProtocol protocol);

    /**
     * 根据提供商查找
     */
    @Query("SELECT s FROM SSOConfigEntity s WHERE s.oauthProvider = :provider AND s.deletedAt IS NULL")
    Optional<SSOConfigEntity> findByOAuthProvider(@Param("provider") String provider);

    /**
     * 统计活跃SSO配置数量
     */
    @Query("SELECT COUNT(s) FROM SSOConfigEntity s WHERE s.ssoStatus = 'ACTIVE' AND s.deletedAt IS NULL")
    long countActive();

    /**
     * 查找有错误的SSO配置
     */
    @Query("SELECT s FROM SSOConfigEntity s WHERE s.ssoStatus = 'ERROR' AND s.deletedAt IS NULL")
    List<SSOConfigEntity> findWithError();
}
