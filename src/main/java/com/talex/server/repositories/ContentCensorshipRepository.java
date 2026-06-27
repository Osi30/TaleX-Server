package com.talex.server.repositories;

import com.talex.server.entities.ContentCensorship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentCensorshipRepository extends JpaRepository<ContentCensorship, String> {

    List<ContentCensorship> findAllByMedia_MediaId(String mediaId);

    Optional<ContentCensorship> findFirstByMedia_MediaIdOrderByCheckedAtDesc(String mediaId);
}
