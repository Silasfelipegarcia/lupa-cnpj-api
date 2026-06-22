package br.com.dadoscnpj.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UserDailyUsageId implements Serializable {

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    public UserDailyUsageId() {
    }

    public UserDailyUsageId(UUID userId, LocalDate usageDate) {
        this.userId = userId;
        this.usageDate = usageDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDailyUsageId that = (UserDailyUsageId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(usageDate, that.usageDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, usageDate);
    }
}
