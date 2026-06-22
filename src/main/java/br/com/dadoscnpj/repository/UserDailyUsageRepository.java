package br.com.dadoscnpj.repository;

import br.com.dadoscnpj.entity.UserDailyUsageEntity;
import br.com.dadoscnpj.entity.UserDailyUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyUsageRepository extends JpaRepository<UserDailyUsageEntity, UserDailyUsageId> {
}
