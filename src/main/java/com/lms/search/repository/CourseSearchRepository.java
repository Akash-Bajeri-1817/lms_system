package com.lms.search.repository;

import com.lms.search.document.CourseDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// works exactly like JpaRepository but for Elasticsearch
@Repository
public interface CourseSearchRepository
        extends ElasticsearchRepository<CourseDocument, String> {

    Optional<CourseDocument> findByCourseId(Long courseId);

    void deleteByCourseId(Long courseId);
}