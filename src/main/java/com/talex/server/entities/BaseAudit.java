package com.talex.server.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseAudit {
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public void markCreatedBy(String actorId) {
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void markUpdatedBy(String actorId) {
        this.updatedBy = actorId;
    }

    public void softDelete(String actorId) {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = actorId;
        this.updatedBy = actorId;
    }

    public void restore(String actorId) {
        this.isDeleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
        this.updatedBy = actorId;
    }
}
