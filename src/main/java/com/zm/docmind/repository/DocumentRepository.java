package com.zm.docmind.repository;

import com.zm.docmind.entity.Document;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends CrudRepository<Document, String> {

    List<Document> findByUserId(String userId);

    List<Document> findByIsPublicTrue();
}
