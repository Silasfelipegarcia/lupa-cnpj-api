package br.com.lupainsights.repository;

import br.com.lupainsights.entity.UserDailyUsageEntity;
import br.com.lupainsights.entity.UserDailyUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyUsageRepository extends JpaRepository<UserDailyUsageEntity, UserDailyUsageId> {
}
