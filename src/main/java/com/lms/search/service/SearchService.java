package com.lms.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.lms.common.PageResponse;
import com.lms.course.entity.Course;
import com.lms.search.document.CourseDocument;
import com.lms.search.dto.SearchRequest;
import com.lms.search.repository.CourseSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final CourseSearchRepository searchRepository;
    private final ElasticsearchClient elasticsearchClient;

    // index a course into Elasticsearch when published
    public void indexCourse(Course course) {
        CourseDocument document = CourseDocument.builder()
                .id(String.valueOf(course.getId()))
                .courseId(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .status(course.getStatus().name())
                .instructorName(
                        course.getInstructor().getFirstName() + " " +
                                course.getInstructor().getLastName()
                )
                .price(course.getPrice())
                .averageRating(0.0)
                .totalStudents(0)
                .publishedAt(course.getUpdatedAt())
                .build();

        searchRepository.save(document);
        log.info("Course indexed in Elasticsearch: {}", course.getTitle());
    }

    // remove a course from Elasticsearch when deleted
    public void removeCourse(Long courseId) {
        searchRepository.deleteByCourseId(courseId);
        log.info("Course removed from Elasticsearch: {}", courseId);
    }

    // update student count when someone enrolls
    public void updateStudentCount(Long courseId, int delta) {
        searchRepository.findByCourseId(courseId).ifPresent(doc -> {
            int current = doc.getTotalStudents() != null
                    ? doc.getTotalStudents() : 0;
            doc.setTotalStudents(current + delta);
            searchRepository.save(doc);
        });
    }

    // main search method
    public PageResponse<com.lms.search.dto.SearchResponse> search(
            SearchRequest request) {
        try {
            // build the search query
            SearchResponse<CourseDocument> response =
                    elasticsearchClient.search(s -> s
                                    .index("courses")
                                    .from(request.getPage() * request.getSize())
                                    .size(request.getSize())
                                    .query(buildQuery(request))
                                    .sort(buildSort(request)),
                            CourseDocument.class
                    );

            // map hits to response DTOs
            List<com.lms.search.dto.SearchResponse> results =
                    new ArrayList<>();

            for (Hit<CourseDocument> hit : response.hits().hits()) {
                CourseDocument doc = hit.source();
                if (doc != null) {
                    results.add(com.lms.search.dto.SearchResponse.builder()
                            .courseId(hit.id())
                            .id(doc.getCourseId())
                            .title(doc.getTitle())
                            .description(doc.getDescription())
                            .instructorName(doc.getInstructorName())
                            .price(doc.getPrice())
                            .averageRating(doc.getAverageRating())
                            .totalStudents(doc.getTotalStudents())
                            .status(doc.getStatus())
                            .score(hit.score() != null
                                    ? hit.score().floatValue() : 0f)
                            .build());
                }
            }

            // build page response
            long totalHits = response.hits().total() != null
                    ? response.hits().total().value() : 0;

            return PageResponse.<com.lms.search.dto.SearchResponse>builder()
                    .content(results)
                    .currentPage(request.getPage())
                    .pageSize(request.getSize())
                    .totalElements(totalHits)
                    .totalPages((int) Math.ceil(
                            (double) totalHits / request.getSize()))
                    .hasNext(
                            (request.getPage() + 1) * request.getSize()
                                    < totalHits)
                    .hasPrevious(request.getPage() > 0)
                    .build();

        } catch (IOException e) {
            log.error("Elasticsearch search failed: {}", e.getMessage());
            throw new RuntimeException("Search failed: " + e.getMessage());
        }
    }

    // ── QUERY BUILDER ────────────────────────────────────────────────

    private Query buildQuery(SearchRequest request) {
        return Query.of(q -> q
                .bool(b -> {
                    // full text search across title and description
                    if (request.getQuery() != null
                            && !request.getQuery().isBlank()) {
                        b.must(m -> m
                                .multiMatch(mm -> mm
                                        .query(request.getQuery())
                                        .fields(
                                                "title^3",        // title is 3x more important
                                                "description^1",
                                                "instructorName^2"
                                        )
                                )
                        );
                    } else {
                        // no search term — match all published courses
                        b.must(m -> m
                                .term(t -> t
                                        .field("status")
                                        .value("PUBLISHED")
                                )
                        );
                    }

                    // filter: free courses only
                    if (Boolean.TRUE.equals(request.getFreeOnly())) {
                        b.filter(f -> f
                                .term(t -> t
                                        .field("price")
                                        .value(0)
                                )
                        );
                    }

                    // filter: price range
                    if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                        b.filter(f -> f
                                .range(r -> r
                                        .number(n -> {
                                            n.field("price");
                                            if (request.getMinPrice() != null) {
                                                n.gte(request.getMinPrice().doubleValue());
                                            }
                                            if (request.getMaxPrice() != null) {
                                                n.lte(request.getMaxPrice().doubleValue());
                                            }
                                            return n;
                                        })
                                )
                        );
                    }

                    // filter: minimum rating
                    if (request.getMinRating() != null) {
                        b.filter(f -> f
                                .range(r -> r
                                        .number(n -> n
                                                .field("averageRating")
                                                .gte(request.getMinRating())
                                        )
                                )
                        );
                    }

                    return b;
                })
        );
    }

    private co.elastic.clients.elasticsearch._types.SortOptions
    buildSort(SearchRequest request) {
        return switch (request.getSortBy()) {
            case "price_asc" ->
                    co.elastic.clients.elasticsearch._types.SortOptions.of(
                            s -> s.field(f -> f
                                    .field("price")
                                    .order(SortOrder.Asc)
                            )
                    );
            case "price_desc" ->
                    co.elastic.clients.elasticsearch._types.SortOptions.of(
                            s -> s.field(f -> f
                                    .field("price")
                                    .order(SortOrder.Desc)
                            )
                    );
            case "rating" ->
                    co.elastic.clients.elasticsearch._types.SortOptions.of(
                            s -> s.field(f -> f
                                    .field("averageRating")
                                    .order(SortOrder.Desc)
                            )
                    );
            case "newest" ->
                    co.elastic.clients.elasticsearch._types.SortOptions.of(
                            s -> s.field(f -> f
                                    .field("publishedAt")
                                    .order(SortOrder.Desc)
                            )
                    );
            default ->  // relevance — Elasticsearch handles this by default
                    co.elastic.clients.elasticsearch._types.SortOptions.of(
                            s -> s.score(sc -> sc.order(SortOrder.Desc))
                    );
        };
    }

    // update average rating when a review is added/updated/deleted
    public void updateCourseRating(Long courseId, Double averageRating) {
        searchRepository.findByCourseId(courseId).ifPresent(doc -> {
            doc.setAverageRating(averageRating);
            searchRepository.save(doc);
            log.info("Updated rating for course {}: {}", courseId, averageRating);
        });
    }
}