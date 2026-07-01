package com.talex.server.repositories.interaction;

import com.talex.server.entities.interaction.AccountComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountCommentRepository extends JpaRepository<AccountComment, String> {
    Optional<AccountComment> findByIdAndIsDeletedFalse(String id);
}