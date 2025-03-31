package com.greenpastures.tagq.repository;

import com.greenpastures.tagq.repository.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, Integer> {

    Optional<TagEntity> findByTag(String tagName);

    boolean existsByTag(String tag);

    Set<TagEntity> findByTagIn(Collection<String> tags);
}
