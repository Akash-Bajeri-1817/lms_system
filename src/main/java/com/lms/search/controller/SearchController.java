package com.lms.search.controller;

import com.lms.common.ApiResponse;
import com.lms.common.PageResponse;
import com.lms.search.dto.SearchRequest;
import com.lms.search.dto.SearchResponse;
import com.lms.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Elasticsearch powered course search")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "Search courses with filters and sorting")
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<PageResponse<SearchResponse>>>
    searchCourses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Boolean freeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "relevance") String sortBy
    ) {

        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setMinPrice(minPrice != null
                ? java.math.BigDecimal.valueOf(minPrice) : null);
        request.setMaxPrice(maxPrice != null
                ? java.math.BigDecimal.valueOf(maxPrice) : null);
        request.setMinRating(minRating);
        request.setFreeOnly(freeOnly);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);

        return ResponseEntity.ok(
                ApiResponse.success(
                        searchService.search(request),
                        "Search completed"
                )
        );
    }
}