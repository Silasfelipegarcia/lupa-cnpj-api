package br.com.dadoscnpj.repository;

import br.com.dadoscnpj.entity.ImportResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ImportResultRepository extends JpaRepository<ImportResultEntity, Long> {

    List<ImportResultEntity> findByJobIdOrderByLinhaNumeroAsc(UUID jobId);

    void deleteByJobId(UUID jobId);
}
