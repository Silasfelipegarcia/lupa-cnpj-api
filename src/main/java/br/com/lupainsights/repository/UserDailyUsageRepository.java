package br.com.lupainsights.repository;

import br.com.lupainsights.entity.UserDailyUsageEntity;
import br.com.lupainsights.entity.UserDailyUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface UserDailyUsageRepository extends JpaRepository<UserDailyUsageEntity, UserDailyUsageId> {

    @Query("""
            SELECT COALESCE(SUM(u.batchSearches), 0), COALESCE(SUM(u.directCnpjLookups), 0)
            FROM UserDailyUsageEntity u WHERE u.id.usageDate >= :since
            """)
    Object[] sumUsageSince(@Param("since") LocalDate since);

    @Query("""
            SELECT COALESCE(SUM(u.batchSearches), 0), COALESCE(SUM(u.directCnpjLookups), 0)
            FROM UserDailyUsageEntity u WHERE u.id.usageDate = :date
            """)
    Object[] sumUsageOnDate(@Param("date") LocalDate date);

    @Query("""
            SELECT u FROM UserDailyUsageEntity u
            WHERE u.id.userId IN :userIds AND u.id.usageDate = :date
            """)
    List<UserDailyUsageEntity> findByUserIdsAndDate(@Param("userIds") List<UUID> userIds,
                                                    @Param("date") LocalDate date);

    @Query("""
            SELECT u FROM UserDailyUsageEntity u
            WHERE u.id.userId = :userId AND u.id.usageDate >= :since
            ORDER BY u.id.usageDate DESC
            """)
    List<UserDailyUsageEntity> findByUserIdSince(@Param("userId") UUID userId,
                                                 @Param("since") LocalDate since);

    @Query("""
            SELECT COALESCE(SUM(u.batchSearches), 0), COALESCE(SUM(u.directCnpjLookups), 0)
            FROM UserDailyUsageEntity u WHERE u.id.userId = :userId
            """)
    Object[] sumUsageByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT COALESCE(SUM(u.batchSearches), 0), COALESCE(SUM(u.directCnpjLookups), 0)
            FROM UserDailyUsageEntity u
            WHERE u.id.userId = :userId AND u.id.usageDate >= :since
            """)
    Object[] sumUsageByUserIdSince(@Param("userId") UUID userId, @Param("since") LocalDate since);
}
