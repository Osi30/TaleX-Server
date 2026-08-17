package com.talex.server.repositories.media;

import com.talex.server.entities.media.ViolationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ViolationDetailRepository extends JpaRepository<ViolationDetail, String> {

    // Admin hard-purge: delete every violation_detail belonging to any censorship of the media.
    // Must run BEFORE deleting the parent content_censorship rows (bulk delete bypasses the
    // orphanRemoval cascade that would otherwise remove these).
    @Modifying
    @Query("DELETE FROM ViolationDetail v WHERE v.censorship.media.mediaId = :mediaId")
    void deleteAllByCensorshipMediaId(@Param("mediaId") String mediaId);
}
