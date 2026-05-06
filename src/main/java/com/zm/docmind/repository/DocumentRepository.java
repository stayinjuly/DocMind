package com.zm.docmind.repository;

import com.zm.docmind.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends CrudRepository<Document, String> {

    Page<Document> findByUserId(String userId, Pageable pageable);

    Page<Document> findByIsPublicTrue(Pageable pageable);
}
