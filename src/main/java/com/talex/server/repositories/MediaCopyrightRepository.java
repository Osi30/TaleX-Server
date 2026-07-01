package com.talex.server.repositories;

import com.talex.server.entities.media.MediaCopyright;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaCopyrightRepository extends JpaRepository<MediaCopyright, String> {

    List<MediaCopyright> findAllByMedia_MediaId(String mediaId);
}
