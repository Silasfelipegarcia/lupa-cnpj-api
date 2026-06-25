package br.com.lupainsights.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_daily_usage")
public class UserDailyUsageEntity {

    @EmbeddedId
    private UserDailyUsageId id;

    @Column(name = "batch_searches", nullable = false)
    private int batchSearches;

    @Column(name = "direct_cnpj_lookups", nullable = false)
    private int directCnpjLookups;

    public static UserDailyUsageEntity zerado(UUID userId, LocalDate date) {
        UserDailyUsageEntity entity = new UserDailyUsageEntity();
        entity.id = new UserDailyUsageId(userId, date);
        entity.batchSearches = 0;
        entity.directCnpjLookups = 0;
        return entity;
    }

    public UserDailyUsageId getId() {
        return id;
    }

    public void setId(UserDailyUsageId id) {
        this.id = id;
    }

    public int getBatchSearches() {
        return batchSearches;
    }

    public void setBatchSearches(int batchSearches) {
        this.batchSearches = batchSearches;
    }

    public int getDirectCnpjLookups() {
        return directCnpjLookups;
    }

    public void setDirectCnpjLookups(int directCnpjLookups) {
        this.directCnpjLookups = directCnpjLookups;
    }

    public UUID getUserId() {
        return id.getUserId();
    }

    public LocalDate getUsageDate() {
        return id.getUsageDate();
    }
}
